package com.humblecoders.humblecoders

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FaceRegistrationViewModel(
    private val userViewModel: UserViewModel? = null
) : ViewModel() {
    
    private val repository = FaceRegistrationRepository()
    private val userRepository = UserRepository()
    
    private val _uiState = MutableStateFlow(FaceRegistrationUiState())
    val uiState: StateFlow<FaceRegistrationUiState> = _uiState.asStateFlow()
    
    fun registerFace(firebaseUid: String, faceImages: List<Bitmap>, context: Context) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null,
            showRetry = false,
            progress = 0f,
            statusMessage = "Creating person profile...",
            capturedImages = faceImages
        )
        
        viewModelScope.launch {
            try {
                // Step 1: Create person with face images using Firebase UID as identifier
                val createPersonResult = repository.createPerson(firebaseUid, faceImages)
                if (createPersonResult.isFailure) {
                    val errorMessage = createPersonResult.exceptionOrNull()?.message ?: "Failed to create person"
                    val isFaceDetectionError = errorMessage.contains("Can't find faces", ignoreCase = true) ||
                                            errorMessage.contains("no faces detected", ignoreCase = true) ||
                                            errorMessage.contains("face not found", ignoreCase = true)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMessage,
                        showRetry = isFaceDetectionError
                    )
                    return@launch
                }
                
                val personId = createPersonResult.getOrThrow()
                
                // Store the person UUID in SharedPreferences
                val prefs = context.getSharedPreferences("face_registration", Context.MODE_PRIVATE)
                prefs.edit().putString("person_uuid", personId).apply()

                // Save faceUuid to user profile
                val currentProfile = userRepository.getUserProfile().getOrNull()
                if (currentProfile != null) {
                    val updatedProfile = currentProfile.copy(faceUuid = personId)
                    userRepository.updateUserProfile(updatedProfile)
                }

                // Note: Don't mark onboarding complete here - only after skills are completed

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRegistrationComplete = true,
                    progress = 1.0f,
                    statusMessage = "Face registration completed successfully! Person UUID: $personId",
                    personId = personId,
                    showRetry = false
                )
                
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error occurred"
                val isFaceDetectionError = errorMessage.contains("Can't find faces", ignoreCase = true) ||
                                        errorMessage.contains("no faces detected", ignoreCase = true) ||
                                        errorMessage.contains("face not found", ignoreCase = true)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage,
                    showRetry = isFaceDetectionError
                )
            }
        }
    }
    
    fun verifyFace(personUuid: String, bitmap: Bitmap) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null,
            statusMessage = "Verifying face..."
        )
        
        viewModelScope.launch {
            try {
                val verifyResult = repository.verifyFace(personUuid, bitmap)
                if (verifyResult.isSuccess) {
                    val response = verifyResult.getOrThrow()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = "Verification complete!",
                        verificationResponse = response
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = verifyResult.exceptionOrNull()?.message ?: "Verification failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, showRetry = false)
    }
    
    fun retryRegistration(firebaseUid: String, context: Context) {
        val capturedImages = _uiState.value.capturedImages
        if (capturedImages.isNotEmpty()) {
            registerFace(firebaseUid, capturedImages, context)
        }
    }
    
    fun resetState() {
        _uiState.value = FaceRegistrationUiState()
    }
    
    fun getStoredPersonUuid(context: Context): String? {
        val prefs = context.getSharedPreferences("face_registration", Context.MODE_PRIVATE)
        return prefs.getString("person_uuid", null)
    }
}

data class FaceRegistrationUiState(
    val isLoading: Boolean = false,
    val isRegistrationComplete: Boolean = false,
    val progress: Float = 0f,
    val statusMessage: String = "",
    val error: String? = null,
    val personId: String? = null,
    val verificationResponse: VerifyFaceResponse? = null,
    val showRetry: Boolean = false,
    val capturedImages: List<Bitmap> = emptyList()
)
