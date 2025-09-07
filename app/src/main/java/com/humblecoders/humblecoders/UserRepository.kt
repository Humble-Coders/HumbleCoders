package com.humblecoders.humblecoders

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun createUserProfile(userProfile: UserProfile): Result<UserProfile> {
        return try {
            val user = auth.currentUser
            if (user == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            val profileWithUid = userProfile.copy(uid = user.uid)
            firestore.collection("users")
                .document(user.uid)
                .set(profileWithUid.toMap())
                .await()

            Result.success(profileWithUid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(userProfile: UserProfile): Result<UserProfile> {
        return try {
            val user = auth.currentUser
            if (user == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            val updatedProfile = userProfile.copy(
                uid = user.uid,
                updatedAt = com.google.firebase.Timestamp.now()
            )
            
            firestore.collection("users")
                .document(user.uid)
                .update(updatedProfile.toMap())
                .await()

            Result.success(updatedProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(): Result<UserProfile?> {
        return try {
            val user = auth.currentUser
            if (user == null) {
                android.util.Log.e("UserRepository", "User not authenticated")
                return Result.failure(Exception("User not authenticated"))
            }

            android.util.Log.d("UserRepository", "Loading profile for user: ${user.uid}")
            val document = firestore.collection("users")
                .document(user.uid)
                .get()
                .await()

            if (document.exists()) {
                val data = document.data ?: emptyMap()
                android.util.Log.d("UserRepository", "Raw Firestore data: $data")
                val userProfile = UserProfile.fromMap(data)
                android.util.Log.d("UserRepository", "Profile loaded successfully. Email: '${userProfile.email}', DisplayName: '${userProfile.displayName}'")
                Result.success(userProfile)
            } else {
                android.util.Log.w("UserRepository", "Profile document does not exist for user: ${user.uid}")
                Result.success(null)
            }
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error loading profile", e)
            Result.failure(e)
        }
    }

    suspend fun updateLuxandPersonId(personId: String): Result<Boolean> {
        return try {
            val user = auth.currentUser
            if (user == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            firestore.collection("users")
                .document(user.uid)
                .update("luxandPersonId", personId, "updatedAt", com.google.firebase.Timestamp.now())
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markOnboardingComplete(): Result<Boolean> {
        return try {
            val user = auth.currentUser
            if (user == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            firestore.collection("users")
                .document(user.uid)
                .update("isOnboardingComplete", true, "updatedAt", com.google.firebase.Timestamp.now())
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrUpdateUserProfile(
        email: String,
        displayName: String,
        photoUrl: String? = null,
        luxandPersonId: String? = null
    ): Result<UserProfile> {
        return try {
            val user = auth.currentUser
            if (user == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            android.util.Log.d("UserRepository", "Creating/updating profile for user: ${user.uid}")
            android.util.Log.d("UserRepository", "Email: $email, DisplayName: $displayName")

            // Check if user profile already exists
            val existingProfile = getUserProfile().getOrNull()
            
            val userProfile = if (existingProfile != null) {
                android.util.Log.d("UserRepository", "Updating existing profile")
                existingProfile.copy(
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl ?: existingProfile.photoUrl,
                    luxandPersonId = luxandPersonId ?: existingProfile.luxandPersonId,
                    updatedAt = com.google.firebase.Timestamp.now()
                )
            } else {
                android.util.Log.d("UserRepository", "Creating new profile")
                UserProfile(
                    uid = user.uid,
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl ?: "",
                    luxandPersonId = luxandPersonId ?: ""
                )
            }

            android.util.Log.d("UserRepository", "Saving profile with email: ${userProfile.email}")
            firestore.collection("users")
                .document(user.uid)
                .set(userProfile.toMap())
                .await()

            android.util.Log.d("UserRepository", "Profile saved successfully")
            Result.success(userProfile)
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error creating/updating profile", e)
            Result.failure(e)
        }
    }

    suspend fun enrollInCourse(courseTitle: String): Result<Boolean> {
        return try {
            val user = auth.currentUser
            if (user == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            // Get current user profile
            val currentProfile = getUserProfile().getOrNull()
            if (currentProfile == null) {
                return Result.failure(Exception("User profile not found"))
            }

            // Add course to enrolled courses if not already enrolled
            val updatedEnrolledCourses = if (currentProfile.enrolledCourses.contains(courseTitle)) {
                currentProfile.enrolledCourses
            } else {
                currentProfile.enrolledCourses + courseTitle
            }

            val updatedProfile = currentProfile.copy(
                enrolledCourses = updatedEnrolledCourses,
                updatedAt = com.google.firebase.Timestamp.now()
            )

            firestore.collection("users")
                .document(user.uid)
                .update(updatedProfile.toMap())
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveAttendanceRecord(attendanceRecord: AttendanceRecord): Result<Boolean> {
        return try {
            val user = auth.currentUser
            if (user == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            firestore.collection("attendance")
                .document(attendanceRecord.id)
                .set(attendanceRecord.toMap())
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAttendanceRecords(userId: String, courseTitle: String? = null): Result<List<AttendanceRecord>> {
        return try {
            val user = auth.currentUser
            if (user == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            var query = firestore.collection("attendance")
                .whereEqualTo("userId", userId)

            if (courseTitle != null) {
                query = query.whereEqualTo("courseTitle", courseTitle)
            }

            val documents = query.get().await()
            val attendanceRecords = documents.mapNotNull { doc ->
                AttendanceRecord.fromMap(doc.data)
            }

            Result.success(attendanceRecords)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                UserProfile.fromMap(document.data ?: emptyMap())
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
