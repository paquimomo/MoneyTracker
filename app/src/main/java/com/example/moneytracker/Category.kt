package com.example.moneytracker

import com.google.firebase.Timestamp

data class Category(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val color: String = "#607D8B",
    val icon: String = "other",
    val monthlyLimit: Double = 0.0,
    val isDefault: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
) {

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "name" to name,
            "color" to color,
            "icon" to icon,
            "monthlyLimit" to monthlyLimit,
            "isDefault" to isDefault,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Category {
            return Category(
                id = map["id"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                color = map["color"] as? String ?: "#607D8B",
                icon = map["icon"] as? String ?: "other",
                monthlyLimit = (map["monthlyLimit"] as? Number)?.toDouble() ?: 0.0,
                isDefault = map["isDefault"] as? Boolean ?: false,
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now()
            )
        }

        fun getDefaultCategories(userId: String): List<Category> {
            return listOf(
                Category(
                    id = "default_food",
                    userId = userId,
                    name = "Alimentación",
                    color = "#FF5722",
                    icon = "restaurant",
                    monthlyLimit = 0.0,
                    isDefault = true
                ),
                Category(
                    id = "default_transport",
                    userId = userId,
                    name = "Transporte",
                    color = "#3F51B5",
                    icon = "directions_car",
                    monthlyLimit = 0.0,
                    isDefault = true
                ),
                Category(
                    id = "default_entertainment",
                    userId = userId,
                    name = "Entretenimiento",
                    color = "#E91E63",
                    icon = "local_movies",
                    monthlyLimit = 0.0,
                    isDefault = true
                ),
                Category(
                    id = "default_health",
                    userId = userId,
                    name = "Salud",
                    color = "#4CAF50",
                    icon = "local_hospital",
                    monthlyLimit = 0.0,
                    isDefault = true
                ),
                Category(
                    id = "default_shopping",
                    userId = userId,
                    name = "Compras",
                    color = "#9C27B0",
                    icon = "shopping_cart",
                    monthlyLimit = 0.0,
                    isDefault = true
                ),
                Category(
                    id = "default_services",
                    userId = userId,
                    name = "Servicios",
                    color = "#FF9800",
                    icon = "build",
                    monthlyLimit = 0.0,
                    isDefault = true
                ),
                Category(
                    id = "default_others",
                    userId = userId,
                    name = "Otros",
                    color = "#607D8B",
                    icon = "more_horiz",
                    monthlyLimit = 0.0,
                    isDefault = true
                )
            )
        }
    }
}