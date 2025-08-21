package com.example.eventmanagement2.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventmanagement2.data.model.AuthState
import com.example.eventmanagement2.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import androidx.annotation.StringRes
import com.example.eventmanagement2.R

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val context: Context
) : ViewModel() {

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authState
                .catch { e ->
                    _errorMessage.value = e.message
                    _isLoading.value = false
                }
                .collectLatest { state ->
                    _authState.value = state
                    _isLoading.value = state is AuthState.Loading
                    
                    // Clear error when we get a new state that's not an error
                    if (state !is AuthState.Error) {
                        _errorMessage.value = null
                    }
                }
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = context.getString(R.string.error_required_fields)
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.signInWithEmailAndPassword(email, password)
            handleAuthResult(result)
        }
    }

    fun signUp(name: String, email: String, password: String, confirmPassword: String) {
        when {
            name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() -> {
                _errorMessage.value = context.getString(R.string.error_required_fields)
                return
            }
            password != confirmPassword -> {
                _errorMessage.value = context.getString(R.string.error_passwords_not_match)
                return
            }
            password.length < 6 -> {
                _errorMessage.value = context.getString(
                    R.string.error_password_too_short, 
                    MIN_PASSWORD_LENGTH
                )
                return
            }
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.signUpWithEmailAndPassword(name, email, password)
            handleAuthResult(result)
        }
    }

    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            _errorMessage.value = context.getString(R.string.error_required_fields)
            return
        }

       /* viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.sendPasswordResetEmail(email)
            _authState.value = result
            _isLoading.value = false
        }*/
    }

    suspend fun signOut() {
        authRepository.signOut()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun handleAuthResult(result: AuthState) {
        _authState.value = result
        _isLoading.value = result is AuthState.Loading
        
        if (result is AuthState.Error) {
            _errorMessage.value = result.message
        }
    }
}
