package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "ratings",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Car::class,
            parentColumns = ["carId"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Reservation::class,
            parentColumns = ["reservationId"],
            childColumns = ["reservationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Rating(
    @PrimaryKey
    val ratingId: String,
    val userId: String,
    val carId: String,
    val reservationId: String,
    val score: Float,
    val comment: String?,
    val date: LocalDate = LocalDate.now()
) 