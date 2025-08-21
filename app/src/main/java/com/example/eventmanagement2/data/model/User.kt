package com.example.eventmanagement2.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import java.util.Date

data class User(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    // Convert Firestore Timestamp to Date
    constructor(
        id: String = "",
        name: String = "",
        email: String = "",
        createdAt: Timestamp = Timestamp.now(),
        updatedAt: Timestamp = Timestamp.now()
    ) : this(
        id = id,
        name = name,
        email = email,
        createdAt = createdAt.toDate(),
        updatedAt = updatedAt.toDate()
    )

    // Convert to Map for Firestore
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "email" to email,
        "createdAt" to Timestamp(createdAt),
        "updatedAt" to Timestamp(updatedAt)
    )
}
