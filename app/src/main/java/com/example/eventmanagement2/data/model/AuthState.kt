package com.example.eventmanagement2.data.model

/**
 * Represents the authentication state of the user.
 */
sealed class AuthState {
    /** User is authenticated */
    data class Authenticated(val user: User) : AuthState()
    
    /** User is not authenticated */
    object Unauthenticated : AuthState()
    
    /** Authentication is in progress */
    object Loading : AuthState()
    
    /** Authentication failed with an error */
    data class Error(val message: String) : AuthState()
    
    /** Password reset email sent successfully */
    object PasswordResetSent : AuthState()
}
