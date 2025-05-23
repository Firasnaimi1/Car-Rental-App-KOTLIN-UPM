package com.example.myapplication.data.repository

import com.example.myapplication.data.database.RatingDao
import com.example.myapplication.data.database.ReservationDao
import com.example.myapplication.data.model.Rating
import com.example.myapplication.data.model.ReservationStatus
import kotlinx.coroutines.flow.Flow
import java.util.*

class RatingRepository(
    private val ratingDao: RatingDao,
    private val reservationDao: ReservationDao,
    private val carRepository: CarRepository,
    private val userRepository: UserRepository
) {

    suspend fun insertRating(rating: Rating) = ratingDao.insertRating(rating)
    
    suspend fun updateRating(rating: Rating) = ratingDao.updateRating(rating)
    
    suspend fun deleteRating(rating: Rating) = ratingDao.deleteRating(rating)
    
    suspend fun getRatingById(ratingId: String) = ratingDao.getRatingById(ratingId)
    
    fun getRatingsByCarId(carId: String) = ratingDao.getRatingsByCarId(carId)
    
    fun getRatingsByUserId(userId: String) = ratingDao.getRatingsByUserId(userId)
    
    suspend fun getRatingByReservationId(reservationId: String) = 
        ratingDao.getRatingByReservationId(reservationId)
    
    suspend fun getAverageRatingForCar(carId: String) = ratingDao.getAverageRatingForCar(carId)
    
    suspend fun getRatingCountForCar(carId: String) = ratingDao.getRatingCountForCar(carId)
    
    /**
     * Add a rating for a car after a completed reservation
     * @return the created rating if successful, null if not allowed
     */
    suspend fun addRating(
        userId: String,
        carId: String,
        reservationId: String,
        score: Float,
        comment: String?
    ): Rating? {
        // Check if reservation exists and is completed
        val reservation = reservationDao.getReservationById(reservationId)
        if (reservation == null || reservation.status != ReservationStatus.COMPLETED) {
            return null
        }
        
        // Check if user is the renter
        if (reservation.renterId != userId) {
            return null
        }
        
        // Check if rating already exists
        val existingRating = getRatingByReservationId(reservationId)
        if (existingRating != null) {
            return null
        }
        
        // Create and save the rating
        val rating = Rating(
            ratingId = UUID.randomUUID().toString(),
            userId = userId,
            carId = carId,
            reservationId = reservationId,
            score = score,
            comment = comment
        )
        
        insertRating(rating)
        
        // Update car rating
        updateCarRating(carId)
        
        // Update car owner rating
        val car = carRepository.getCarById(carId)
        car?.let { updateOwnerRating(it.ownerId) }
        
        return rating
    }
    
    /**
     * Update the average rating for a car
     */
    private suspend fun updateCarRating(carId: String) {
        val avgRating = getAverageRatingForCar(carId)
        val count = getRatingCountForCar(carId)
        carRepository.updateCarRating(carId, avgRating, count)
    }
    
    /**
     * Update the average rating for a car owner based on all their cars
     */
    private suspend fun updateOwnerRating(ownerId: String) {
        var totalScore = 0f
        var totalCount = 0
        
        try {
            // Get all cars for this owner and collect ratings
            carRepository.getCarsByOwner(ownerId)
                .collect { cars ->
                    for (car in cars) {
                        if (car.ratingCount > 0) {
                            totalScore += car.rating * car.ratingCount
                            totalCount += car.ratingCount
                        }
                    }
                    
                    // Update owner rating
                    if (totalCount > 0) {
                        val averageRating = totalScore / totalCount
                        userRepository.updateUserRating(ownerId, averageRating, totalCount)
                    } else {
                        userRepository.updateUserRating(ownerId, 0f, 0)
                    }
                }
        } catch (e: Exception) {
            // Handle error silently for now
            // In a production app, we might log this or retry
        }
    }
} 