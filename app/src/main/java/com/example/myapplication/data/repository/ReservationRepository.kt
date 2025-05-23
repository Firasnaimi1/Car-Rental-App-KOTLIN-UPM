package com.example.myapplication.data.repository

import com.example.myapplication.data.database.ReservationDao
import com.example.myapplication.data.model.Reservation
import com.example.myapplication.data.model.ReservationStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

class ReservationRepository(private val reservationDao: ReservationDao) {

    suspend fun insertReservation(reservation: Reservation) = 
        reservationDao.insertReservation(reservation)
    
    suspend fun updateReservation(reservation: Reservation) = 
        reservationDao.updateReservation(reservation)
    
    suspend fun deleteReservation(reservation: Reservation) = 
        reservationDao.deleteReservation(reservation)
    
    suspend fun getReservationById(reservationId: String) = 
        reservationDao.getReservationById(reservationId)
    
    fun getReservationsByCarId(carId: String) = 
        reservationDao.getReservationsByCarId(carId)
    
    fun getReservationsByRenterId(renterId: String) = 
        reservationDao.getReservationsByRenterId(renterId)
    
    suspend fun getConflictingReservations(carId: String, startDate: LocalDate, endDate: LocalDate) = 
        reservationDao.getConflictingReservations(carId, startDate, endDate)
    
    suspend fun updateReservationStatus(reservationId: String, status: ReservationStatus) = 
        reservationDao.updateReservationStatus(reservationId, status)
    
    suspend fun markReservationAsPaid(reservationId: String) = 
        reservationDao.markReservationAsPaid(reservationId)
    
    fun getOwnerReservationsByStatus(ownerId: String, status: ReservationStatus) = 
        reservationDao.getOwnerReservationsByStatus(ownerId, status)
    
    /**
     * Create a new reservation for a car
     * @return the created reservation if successful, null if there are conflicts
     */
    suspend fun createReservation(
        carId: String,
        renterId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        pricePerDay: Double
    ): Reservation? {
        // Check for conflicting reservations
        val conflicts = getConflictingReservations(carId, startDate, endDate)
        if (conflicts.isNotEmpty()) {
            return null
        }
        
        // Calculate total price
        val days = ChronoUnit.DAYS.between(startDate, endDate) + 1
        val totalPrice = pricePerDay * days
        
        val reservation = Reservation(
            reservationId = UUID.randomUUID().toString(),
            carId = carId,
            renterId = renterId,
            startDate = startDate,
            endDate = endDate,
            totalPrice = totalPrice,
            status = ReservationStatus.PENDING,
            isPaid = false
        )
        
        insertReservation(reservation)
        return reservation
    }
    
    /**
     * Process the owner's response to a reservation request
     */
    suspend fun processReservationResponse(
        reservationId: String,
        approved: Boolean
    ): Boolean {
        val reservation = getReservationById(reservationId) ?: return false
        
        // Only pending reservations can be approved or rejected
        if (reservation.status != ReservationStatus.PENDING) {
            return false
        }
        
        val newStatus = if (approved) ReservationStatus.APPROVED else ReservationStatus.REJECTED
        updateReservationStatus(reservationId, newStatus)
        return true
    }
    
    /**
     * Cancel a reservation
     * @return true if cancellation was successful
     */
    suspend fun cancelReservation(reservationId: String): Boolean {
        val reservation = getReservationById(reservationId) ?: return false
        
        // Only pending or approved reservations can be cancelled
        if (reservation.status != ReservationStatus.PENDING && 
            reservation.status != ReservationStatus.APPROVED) {
            return false
        }
        
        updateReservationStatus(reservationId, ReservationStatus.CANCELLED)
        return true
    }
    
    /**
     * Mark a reservation as completed after the rental period
     */
    suspend fun completeReservation(reservationId: String): Boolean {
        val reservation = getReservationById(reservationId) ?: return false
        
        // Only approved and paid reservations can be completed
        if (reservation.status != ReservationStatus.APPROVED || !reservation.isPaid) {
            return false
        }
        
        updateReservationStatus(reservationId, ReservationStatus.COMPLETED)
        return true
    }
} 