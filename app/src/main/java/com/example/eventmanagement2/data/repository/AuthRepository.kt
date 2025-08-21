package com.example.eventmanagement2.data.repository

import android.content.Context
import com.example.eventmanagement2.R
import com.example.eventmanagement2.data.model.AuthState
import com.example.eventmanagement2.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    val currentUser: FirebaseUser?
    val authState: Flow<AuthState>
    
    suspend fun signInWithEmailAndPassword(email: String, password: String): AuthState
    suspend fun signUpWithEmailAndPassword(name: String, email: String, password: String): AuthState
    suspend fun signOut()
    suspend fun resetPassword(email: String): AuthState
    fun getCurrentUser(): User?
}

@Singleton
class FirestoreAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val context: Context
) : AuthRepository {
    override val currentUser get() = auth.currentUser

    override val authState: Flow<AuthState> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in, fetch user data from Firestore
                firestore.collection(context.getString(R.string.db_collection_users))
                    .document(user.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        val userData = document.toObject(User::class.java)
                        if (userData != null) {
                            trySend(AuthState.Authenticated(userData.copy(id = user.uid)))
                        } else {
                            // User data doesn't exist in Firestore, create it
                            val newUser = User(
                                id = user.uid,
                                name = user.displayName ?: "",
                                email = user.email ?: "",
                                createdAt = Date(),
                                updatedAt = Date()
                            )
                            firestore.collection(context.getString(R.string.db_collection_users))
                                .document(user.uid)
                                .set(newUser)
                                .addOnSuccessListener {
                                    trySend(AuthState.Authenticated(newUser))
                                }
                                .addOnFailureListener { e ->
                                    trySend(AuthState.Error(context.getString(
                                        R.string.error_create_user_data,
                                        e.message ?: context.getString(R.string.error_unknown)
                                    )))
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        trySend(AuthState.Error(context.getString(
                            R.string.error_fetch_user_data,
                            e.message ?: context.getString(R.string.error_unknown)
                        )))
                    }
            } else {
                // User is signed out
                trySend(AuthState.Unauthenticated)
            }
        }

        auth.addAuthStateListener(authStateListener)
        awaitClose { auth.removeAuthStateListener(authStateListener) }
    }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): AuthState {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            AuthState.Loading
        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthInvalidCredentialsException -> {
                    AuthState.Error(context.getString(R.string.error_invalid_credentials))
                }
                else -> {
                    AuthState.Error(e.message ?: context.getString(R.string.error_auth_failed))
                }
            }
        }
    }

    override suspend fun signUpWithEmailAndPassword(name: String, email: String, password: String): AuthState {
        return try {
            // 1. Create user in Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            
            // 2. Create user data in Firestore
            val user = User(
                id = authResult.user?.uid ?: "",
                name = name,
                email = email,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            firestore.collection(context.getString(R.string.db_collection_users))
                .document(user.id)
                .set(user)
                .await()
            
            // 3. Update user profile with display name
            auth.currentUser?.updateProfile(
                com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
            )?.await()
            
            AuthState.Authenticated(user)
        } catch (e: Exception) {
            when (e) {
                is FirebaseAuthWeakPasswordException -> {
                    AuthState.Error(context.getString(R.string.error_weak_password))
                }
                is FirebaseAuthInvalidCredentialsException -> {
                    AuthState.Error(context.getString(R.string.error_invalid_email))
                }
                is FirebaseAuthUserCollisionException -> {
                    AuthState.Error(context.getString(R.string.error_email_already_in_use))
                }
                else -> {
                    AuthState.Error(e.message ?: context.getString(R.string.error_registration_failed))
                }
            }
        }
    }

    override suspend fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            // Log the error or handle it as needed
            throw e
        }
    }

    override suspend fun resetPassword(email: String): AuthState {
        return try {
            auth.sendPasswordResetEmail(email).await()
            AuthState.PasswordResetSent
        } catch (e: Exception) {
            AuthState.Error(e.message ?: context.getString(R.string.error_reset_password_failed))
        }
    }

    override fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        
        // In a real app, you might want to fetch this from Firestore
        // For simplicity, we're creating a basic user object here
        return User(
            id = firebaseUser.uid,
            name = firebaseUser.displayName ?: "",
            email = firebaseUser.email ?: "",
            createdAt = Date(),
            updatedAt = Date()
        )
    }
}
