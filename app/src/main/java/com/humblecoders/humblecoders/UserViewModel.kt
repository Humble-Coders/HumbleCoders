package com.humblecoders.humblecoders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {
    private val repository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadUserProfile() {
        viewModelScope.launch {
            android.util.Log.d("UserViewModel", "Loading user profile")
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = repository.getUserProfile()
                if (result.isSuccess) {
                    val profile = result.getOrNull()
                    if (profile != null) {
                        android.util.Log.d("UserViewModel", "Profile loaded successfully: ${profile.email}")
                        
                        // Check if email is empty and update it with Firebase Auth email
                        if (profile.email.isEmpty()) {
                            android.util.Log.w("UserViewModel", "Profile email is empty, updating with Firebase Auth email")
                            val currentUser = auth.currentUser
                            if (currentUser != null && currentUser.email != null) {
                                val updatedProfile = profile.copy(
                                    email = currentUser.email!!,
                                    displayName = if (profile.displayName.isEmpty()) {
                                        currentUser.displayName ?: currentUser.email!!.split("@").firstOrNull() ?: "User"
                                    } else profile.displayName
                                )
                                android.util.Log.d("UserViewModel", "Updating profile with email: ${updatedProfile.email}")
                                updateUserProfile(updatedProfile)
                            } else {
                                android.util.Log.e("UserViewModel", "No Firebase Auth user or email found")
                                _userProfile.value = profile
                            }
                        } else {
                            _userProfile.value = profile
                        }
                    } else {
                        // Profile doesn't exist, create one with Firebase Auth data
                        android.util.Log.w("UserViewModel", "Profile not found, creating new profile")
                        val currentUser = auth.currentUser
                        if (currentUser != null && currentUser.email != null) {
                            createUserProfile(
                                email = currentUser.email!!,
                                displayName = currentUser.displayName ?: currentUser.email!!.split("@").firstOrNull() ?: "User",
                                photoUrl = currentUser.photoUrl?.toString()
                            )
                        } else {
                            android.util.Log.e("UserViewModel", "No current user or email found")
                            _errorMessage.value = "No user data available"
                        }
                    }
                } else {
                    android.util.Log.e("UserViewModel", "Failed to load profile: ${result.exceptionOrNull()?.message}")
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to load profile"
                }
            } catch (e: Exception) {
                android.util.Log.e("UserViewModel", "Exception loading profile", e)
                _errorMessage.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createUserProfile(
        email: String,
        displayName: String,
        photoUrl: String? = null,
        luxandPersonId: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = repository.createOrUpdateUserProfile(
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl,
                    luxandPersonId = luxandPersonId
                )
                if (result.isSuccess) {
                    _userProfile.value = result.getOrNull()
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to create profile"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUserProfile(userProfile: UserProfile) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = repository.updateUserProfile(userProfile)
                if (result.isSuccess) {
                    _userProfile.value = result.getOrNull()
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to update profile"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateLuxandPersonId(personId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = repository.updateLuxandPersonId(personId)
                if (result.isSuccess) {
                    // Reload user profile to get updated data
                    loadUserProfile()
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to update Luxand ID"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun markOnboardingComplete() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = repository.markOnboardingComplete()
                if (result.isSuccess) {
                    // Reload user profile to get updated data
                    loadUserProfile()
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to mark onboarding complete"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun enrollInCourse(courseTitle: String) {
        viewModelScope.launch {
            android.util.Log.d("UserViewModel", "Starting enrollment for course: $courseTitle")
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = repository.enrollInCourse(courseTitle)
                if (result.isSuccess) {
                    android.util.Log.d("UserViewModel", "Enrollment successful, reloading profile")
                    // Reload user profile to get updated data
                    loadUserProfile()
                } else {
                    android.util.Log.e("UserViewModel", "Enrollment failed: ${result.exceptionOrNull()?.message}")
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to enroll in course"
                }
            } catch (e: Exception) {
                android.util.Log.e("UserViewModel", "Exception during enrollment", e)
                _errorMessage.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun isEnrolledInCourse(courseTitle: String): Boolean {
        return _userProfile.value?.enrolledCourses?.contains(courseTitle) ?: false
    }

    fun getCurrentUser() = auth.currentUser
    
    fun refreshProfileWithAuthData() {
        viewModelScope.launch {
            android.util.Log.d("UserViewModel", "Refreshing profile with Firebase Auth data")
            val currentUser = auth.currentUser
            if (currentUser != null && currentUser.email != null) {
                createUserProfile(
                    email = currentUser.email!!,
                    displayName = currentUser.displayName ?: currentUser.email!!.split("@").firstOrNull() ?: "User",
                    photoUrl = currentUser.photoUrl?.toString()
                )
            } else {
                android.util.Log.e("UserViewModel", "No Firebase Auth user or email found for refresh")
            }
        }
    }
}
