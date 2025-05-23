package com.example.myapplication.data.database

import androidx.room.*
import com.example.myapplication.data.model.Reservation
import com.example.myapplication.data.model.ReservationStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ReservationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservation(reservation: Reservation)

    @Update
    suspend fun updateReservation(reservation: Reservation)

    @Delete
    suspend fun deleteReservation(reservation: Reservation)

    @Query("SELECT * FROM reservations WHERE reservationId = :reservationId")
    suspend fun getReservationById(reservationId: String): Reservation?

    @Query("SELECT * FROM reservations WHERE carId = :carId")
    fun getReservationsByCarId(carId: String): Flow<List<Reservation>>

    @Query("SELECT * FROM reservations WHERE renterId = :renterId")
    fun getReservationsByRenterId(renterId: String): Flow<List<Reservation>>

    @Query("""
        SELECT * FROM reservations
        WHERE carId = :carId AND 
        (
            (startDate BETWEEN :startDate AND :endDate) OR
            (endDate BETWEEN :startDate AND :endDate) OR
            (:startDate BETWEEN startDate AND endDate) OR
            (:endDate BETWEEN startDate AND endDate)
        )
    """)
    suspend fun getConflictingReservations(carId: String, startDate: LocalDate, endDate: LocalDate): List<Reservation>

    @Query("UPDATE reservations SET status = :status WHERE reservationId = :reservationId")
    suspend fun updateReservationStatus(reservationId: String, status: ReservationStatus)
    
    @Query("UPDATE reservations SET isPaid = 1 WHERE reservationId = :reservationId")
    suspend fun markReservationAsPaid(reservationId: String)
    
    @Query("""
        SELECT * FROM reservations 
        WHERE carId IN (SELECT carId FROM cars WHERE ownerId = :ownerId) AND
        status = :status
    """)
    fun getOwnerReservationsByStatus(ownerId: String, status: ReservationStatus): Flow<List<Reservation>>
} 