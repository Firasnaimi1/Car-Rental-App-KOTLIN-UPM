package com.example.myapplication.data.database

import androidx.room.*
import com.example.myapplication.data.model.Rating
import kotlinx.coroutines.flow.Flow

@Dao
interface RatingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRating(rating: Rating)

    @Update
    suspend fun updateRating(rating: Rating)

    @Delete
    suspend fun deleteRating(rating: Rating)

    @Query("SELECT * FROM ratings WHERE ratingId = :ratingId")
    suspend fun getRatingById(ratingId: String): Rating?

    @Query("SELECT * FROM ratings WHERE carId = :carId")
    fun getRatingsByCarId(carId: String): Flow<List<Rating>>

    @Query("SELECT * FROM ratings WHERE userId = :userId")
    fun getRatingsByUserId(userId: String): Flow<List<Rating>>

    @Query("SELECT * FROM ratings WHERE reservationId = :reservationId")
    suspend fun getRatingByReservationId(reservationId: String): Rating?

    @Query("SELECT AVG(score) FROM ratings WHERE carId = :carId")
    suspend fun getAverageRatingForCar(carId: String): Float

    @Query("SELECT COUNT(*) FROM ratings WHERE carId = :carId")
    suspend fun getRatingCountForCar(carId: String): Int
} 