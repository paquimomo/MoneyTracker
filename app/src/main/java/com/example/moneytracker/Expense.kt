package com.example.moneytracker

import com.google.firebase.Timestamp

data class Expense(
    val id: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val categoryId: String = "",
    val date: String = "",
    val time: String = "",
    val imagePath: String? = null,
    val audioPath: String? = null,
    val userId: String = "",
    val location: Map<String, Double>? = null,
    val createdAt: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "description" to description,
            "amount" to amount,
            "category" to category,
            "categoryId" to categoryId,
            "date" to date,
            "time" to time,
            "imagePath" to imagePath,
            "audioPath" to audioPath,
            "location" to location,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): Expense {
            return Expense(
                id = map["id"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                description = map["description"] as? String ?: "",
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                category = map["category"] as? String ?: "",
                categoryId = map["categoryId"] as? String ?: "",
                date = map["date"] as? String ?: "",
                time = map["time"] as? String ?: "",
                imagePath = map["imagePath"] as? String?,
                audioPath = map["audioPath"] as? String?,
                location = map["location"] as? Map<String, Double>?,
                createdAt = map["createdAt"] as? com.google.firebase.Timestamp
                    ?: com.google.firebase.Timestamp.now()
            )
        }
    }
}



