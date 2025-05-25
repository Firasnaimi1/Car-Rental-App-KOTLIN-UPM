package com.example.myapplication.data.database

import androidx.room.*
import com.example.myapplication.data.model.Car
import com.example.myapplication.data.model.CarImage
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCar(car: Car): Long

    @Update
    suspend fun updateCar(car: Car)

    @Delete
    suspend fun deleteCar(car: Car)

    @Query("SELECT * FROM cars WHERE carId = :carId")
    suspend fun getCarById(carId: String): Car?

    @Query("SELECT * FROM cars")
    fun getAllCars(): Flow<List<Car>>

    @Query("SELECT * FROM cars WHERE isAvailable = 1")
    fun getAvailableCars(): Flow<List<Car>>

    @Query("SELECT * FROM cars WHERE ownerId = :ownerId")
    fun getCarsByOwner(ownerId: String): Flow<List<Car>>

    @Query("SELECT * FROM cars WHERE brand LIKE '%' || :searchTerm || '%' OR model LIKE '%' || :searchTerm || '%'")
    fun searchCarsByModel(searchTerm: String): Flow<List<Car>>

    @Query("SELECT * FROM cars WHERE location LIKE '%' || :city || '%'")
    fun searchCarsByCity(city: String): Flow<List<Car>>

    @Query("SELECT * FROM cars WHERE pricePerDay <= :maxPrice")
    fun searchCarsByMaxPrice(maxPrice: Double): Flow<List<Car>>

    @Query("SELECT * FROM cars WHERE location LIKE '%' || :city || '%' AND pricePerDay <= :maxPrice")
    fun searchCarsByCityAndPrice(city: String, maxPrice: Double): Flow<List<Car>>

    @Query("UPDATE cars SET rating = :rating, ratingCount = :ratingCount WHERE carId = :carId")
    suspend fun updateCarRating(carId: String, rating: Float, ratingCount: Int)

    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarImage(carImage: CarImage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarImages(images: List<CarImage>)

    @Query("SELECT * FROM car_images WHERE carId = :carId")
    fun getCarImages(carId: String): Flow<List<CarImage>>

    @Query("SELECT * FROM car_images WHERE carId = :carId AND isPrimary = 1 LIMIT 1")
    suspend fun getCarPrimaryImage(carId: String): CarImage?

    @Delete
    suspend fun deleteCarImage(image: CarImage)

    @Update
    suspend fun updateCarImage(carImage: CarImage)
} 