package com.humblecoders.humblecoders


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblecoders.humblecoders.AuthRepository
import com.humblecoders.humblecoders.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository,
    private val userViewModel: UserViewModel? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signUp(email: String, password: String, confirmPassword: String) {
        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(
                error = "Passwords don't match",
                isLoading = false
            )
            return
        }

        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(
                error = "Password should be at least 6 characters",
                isLoading = false
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            when (val result = repository.signUpWithEmailPassword(email, password)) {
                is AuthResult.Success -> {
                    // Create user profile in Firestore
                    userViewModel?.createUserProfile(
                        email = email,
                        displayName = repository.currentUser?.displayName ?: email.split("@").firstOrNull() ?: "User",
                        photoUrl = repository.currentUser?.photoUrl?.toString()
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        error = null
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            when (val result = repository.signInWithEmailPassword(email, password)) {
                is AuthResult.Success -> {
                    // Load user profile after successful sign in
                    userViewModel?.loadUserProfile()
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        error = null
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            when (val result = repository.signInWithGoogle(idToken)) {
                is AuthResult.Success -> {
                    // Create or update user profile in Firestore
                    userViewModel?.createUserProfile(
                        email = repository.currentUser?.email ?: "",
                        displayName = repository.currentUser?.displayName ?: repository.currentUser?.email?.split("@")?.firstOrNull() ?: "User",
                        photoUrl = repository.currentUser?.photoUrl?.toString()
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        error = null
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _uiState.value = AuthUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun checkAuthStatus() {
        val isLoggedIn = repository.isUserLoggedIn()
        _uiState.value = _uiState.value.copy(
            isAuthenticated = isLoggedIn
        )
        
        // If user is logged in, load their profile
        if (isLoggedIn) {
            userViewModel?.loadUserProfile()
        }
    }
    
    fun getCurrentUser() = repository.currentUser
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null
)