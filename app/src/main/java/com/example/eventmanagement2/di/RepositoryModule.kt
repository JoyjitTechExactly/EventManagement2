package com.example.eventmanagement2.di

import android.content.Context
import com.example.eventmanagement2.data.repository.AuthRepository
import com.example.eventmanagement2.data.repository.EventRepository
import com.example.eventmanagement2.data.repository.FirestoreAuthRepository
import com.example.eventmanagement2.data.repository.FirestoreEventRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context
    ): AuthRepository = FirestoreAuthRepository(auth, firestore, context)

    @Provides
    @Singleton
    fun provideEventRepository(
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context
    ): EventRepository = FirestoreEventRepository(firestore, context)
}
