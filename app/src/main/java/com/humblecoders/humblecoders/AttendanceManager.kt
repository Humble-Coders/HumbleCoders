package com.humblecoders.humblecoders

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class AttendanceRecord(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val courseTitle: String,
    val videoTitle: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVerified: Boolean = false,
    val verificationConfidence: Double? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "courseTitle" to courseTitle,
            "videoTitle" to videoTitle,
            "timestamp" to timestamp,
            "isVerified" to isVerified,
            "verificationConfidence" to verificationConfidence
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): AttendanceRecord {
            return AttendanceRecord(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                userId = map["userId"] as? String ?: "",
                courseTitle = map["courseTitle"] as? String ?: "",
                videoTitle = map["videoTitle"] as? String ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isVerified = map["isVerified"] as? Boolean ?: false,
                verificationConfidence = (map["verificationConfidence"] as? Number)?.toDouble()
            )
        }
    }
}

data class AttendanceUiState(
    val isVerifying: Boolean = false,
    val attendanceMarked: Boolean? = null,
    val verificationMessage: String? = null,
    val error: String? = null
)

class AttendanceManager(
    private val faceRepository: FaceRegistrationRepository,
    private val userRepository: UserRepository,
    private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()
    
    private fun getStoredPersonUuid(): String? {
        val prefs = context.getSharedPreferences("face_registration", Context.MODE_PRIVATE)
        return prefs.getString("person_uuid", null)
    }
    
    fun markAttendance(
        userId: String,
        courseTitle: String,
        videoTitle: String,
        faceImage: Bitmap
    ) {
        android.util.Log.d("AttendanceManager", "Starting attendance verification")
        android.util.Log.d("AttendanceManager", "User ID: $userId, Course: $courseTitle, Video: $videoTitle")
        
        _uiState.value = _uiState.value.copy(
            isVerifying = true,
            error = null,
            verificationMessage = "Verifying attendance..."
        )
        
        viewModelScope.launch {
            try {
                // Get user's face UUID - try SharedPreferences first (like TestVerificationScreen)
                android.util.Log.d("AttendanceManager", "Getting face UUID from SharedPreferences")
                var faceUuid = getStoredPersonUuid()
                
                if (faceUuid.isNullOrEmpty()) {
                    // Fallback to user profile
                    android.util.Log.d("AttendanceManager", "Face UUID not found in SharedPreferences, checking user profile")
                    val userProfile = userRepository.getUserProfile(userId)
                    faceUuid = userProfile?.faceUuid ?: userProfile?.luxandPersonId
                    android.util.Log.d("AttendanceManager", "User profile: $userProfile")
                } else {
                    // Sync face UUID to user profile if it's in SharedPreferences but not in profile
                    android.util.Log.d("AttendanceManager", "Face UUID found in SharedPreferences, syncing to user profile")
                    val userProfile = userRepository.getUserProfile(userId)
                    if (userProfile != null && (userProfile.faceUuid.isEmpty() && userProfile.luxandPersonId.isEmpty())) {
                        android.util.Log.d("AttendanceManager", "Updating user profile with face UUID from SharedPreferences")
                        val updatedProfile = userProfile.copy(faceUuid = faceUuid)
                        userRepository.updateUserProfile(updatedProfile)
                    }
                }
                
                android.util.Log.d("AttendanceManager", "Face UUID: $faceUuid")
                
                if (faceUuid == null || faceUuid.isEmpty()) {
                    android.util.Log.e("AttendanceManager", "Face UUID is null or empty")
                    _uiState.value = _uiState.value.copy(
                        isVerifying = false,
                        attendanceMarked = false,
                        verificationMessage = "Face not registered. Please complete face registration first.",
                        error = "Face not registered"
                    )
                    return@launch
                }
                
                // Verify face using Luxand API
                android.util.Log.d("AttendanceManager", "Calling face verification with UUID: $faceUuid")
                val verificationResult = faceRepository.verifyFace(faceUuid, faceImage)
                
                if (verificationResult.isSuccess) {
                    val response = verificationResult.getOrThrow()
                    android.util.Log.d("AttendanceManager", "Face verification response: $response")
                    
                    // Check if verification was successful
                    val isVerified = response.status == "success" && 
                                   (response.probability ?: 0.0) > 0.5
                    
                    android.util.Log.d("AttendanceManager", "Verification result: isVerified=$isVerified, probability=${response.probability}")
                    
                    // Create attendance record
                    val attendanceRecord = AttendanceRecord(
                        userId = userId,
                        courseTitle = courseTitle,
                        videoTitle = videoTitle,
                        isVerified = isVerified,
                        verificationConfidence = response.probability
                    )
                    
                    // Save attendance record
                    android.util.Log.d("AttendanceManager", "Saving attendance record: $attendanceRecord")
                    val saveResult = userRepository.saveAttendanceRecord(attendanceRecord)
                    
                    if (saveResult.isSuccess) {
                        val newState = _uiState.value.copy(
                            isVerifying = false,
                            attendanceMarked = isVerified,
                            verificationMessage = if (isVerified) {
                                "Attendance marked successfully! (Confidence: ${String.format("%.1f", response.probability ?: 0.0)})"
                            } else {
                                "Attendance not marked. Face verification failed. (Confidence: ${String.format("%.1f", response.probability ?: 0.0)})"
                            }
                        )
                        android.util.Log.d("AttendanceManager", "Updating UI state: $newState")
                        _uiState.value = newState
                    } else {
                        android.util.Log.e("AttendanceManager", "Failed to save attendance record: ${saveResult.exceptionOrNull()?.message}")
                        _uiState.value = _uiState.value.copy(
                            isVerifying = false,
                            attendanceMarked = false,
                            verificationMessage = "Attendance verification successful but failed to save record.",
                            error = saveResult.exceptionOrNull()?.message ?: "Save failed"
                        )
                    }
                } else {
                    android.util.Log.e("AttendanceManager", "Face verification failed: ${verificationResult.exceptionOrNull()?.message}")
                    val errorMessage = verificationResult.exceptionOrNull()?.message ?: "Unknown error"
                    _uiState.value = _uiState.value.copy(
                        isVerifying = false,
                        attendanceMarked = false,
                        verificationMessage = "Attendance not marked. Verification failed: $errorMessage",
                        error = errorMessage
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isVerifying = false,
                    attendanceMarked = false,
                    verificationMessage = "Attendance not marked. Error occurred.",
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
    
    fun clearAttendanceState() {
        _uiState.value = AttendanceUiState()
    }
}
