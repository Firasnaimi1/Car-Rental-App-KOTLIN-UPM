package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "cars",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["ownerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Car(
    @PrimaryKey
    val carId: String,
    val ownerId: String,
    val brand: String,
    val model: String,
    val year: Int,
    val description: String,
    val pricePerDay: Double,
    val location: String,
    val isAvailable: Boolean = true,
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val imageUri: String? = null
)

@Entity(
    tableName = "car_images",
    foreignKeys = [
        ForeignKey(
            entity = Car::class,
            parentColumns = ["carId"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CarImage(
    @PrimaryKey
    val imageId: String,
    val carId: String,
    val imageUrl: String,
    val isPrimary: Boolean = false
) 