package com.example.moneytracker

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val profileImage: String? = null,
    val createdAt: Timestamp = Timestamp.now()
) {

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "username" to username,
            "email" to email,
            "profileImage" to profileImage,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): User {
            return User(
                id = map["id"] as? String ?: "",
                username = map["username"] as? String ?: "",
                email = map["email"] as? String ?: "",
                password = "",  // No viene de Firestore
                profileImage = map["profileImage"] as? String,
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
}