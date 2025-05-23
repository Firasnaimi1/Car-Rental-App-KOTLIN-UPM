package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val userId: String,
    val email: String,
    val password: String,
    val fullName: String,
    val phoneNumber: String,
    val address: String,
    val userType: UserType,
    val profileImageUrl: String? = null,
    val rating: Float = 0f,
    val ratingCount: Int = 0
)

enum class UserType {
    OWNER,
    RENTER
} 