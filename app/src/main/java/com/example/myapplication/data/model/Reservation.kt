package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "reservations",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["renterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Car::class,
            parentColumns = ["carId"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Reservation(
    @PrimaryKey
    val reservationId: String,
    val carId: String,
    val renterId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalPrice: Double,
    val status: ReservationStatus,
    val isPaid: Boolean = false,
    val createdAt: LocalDate = LocalDate.now()
)

enum class ReservationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED,
    COMPLETED
} 