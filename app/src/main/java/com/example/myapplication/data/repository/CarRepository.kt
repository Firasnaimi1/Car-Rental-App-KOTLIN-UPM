package com.example.myapplication.data.repository

import com.example.myapplication.data.database.CarDao
import com.example.myapplication.data.model.Car
import com.example.myapplication.data.model.CarImage
import kotlinx.coroutines.flow.Flow
import java.util.*

class CarRepository(private val carDao: CarDao) {

    suspend fun insertCar(car: Car) = carDao.insertCar(car)
    
    suspend fun updateCar(car: Car) = carDao.updateCar(car)
    
    suspend fun deleteCar(car: Car) = carDao.deleteCar(car)
    
    suspend fun getCarById(carId: String) = carDao.getCarById(carId)
    
    fun getAllCars() = carDao.getAllCars()
    
    fun getAvailableCars() = carDao.getAvailableCars()
    
    fun getCarsByOwner(ownerId: String) = carDao.getCarsByOwner(ownerId)
    
    fun searchCarsByModel(searchTerm: String) = carDao.searchCarsByModel(searchTerm)
    
    fun searchCarsByCity(city: String) = carDao.searchCarsByCity(city)
    
    fun searchCarsByMaxPrice(maxPrice: Double) = carDao.searchCarsByMaxPrice(maxPrice)
    
    fun searchCarsByCityAndPrice(city: String, maxPrice: Double) = 
        carDao.searchCarsByCityAndPrice(city, maxPrice)
    
    suspend fun updateCarRating(carId: String, rating: Float, ratingCount: Int) = 
        carDao.updateCarRating(carId, rating, ratingCount)
    
    // Car image operations
    suspend fun insertCarImage(carImage: CarImage) = carDao.insertCarImage(carImage)
    
    suspend fun insertCarImages(images: List<CarImage>) = carDao.insertCarImages(images)
    
    fun getCarImages(carId: String) = carDao.getCarImages(carId)
    
    suspend fun getCarPrimaryImage(carId: String) = carDao.getCarPrimaryImage(carId)
    
    suspend fun deleteCarImage(image: CarImage) = carDao.deleteCarImage(image)
    
    /**
     * Create a new car with the given information
     */
    suspend fun createCar(
        ownerId: String,
        brand: String,
        model: String,
        year: Int,
        description: String,
        pricePerDay: Double,
        location: String,
        imageUrls: List<String> = emptyList()
    ): Car {
        try {
            val carId = UUID.randomUUID().toString()
            
            val car = Car(
                carId = carId,
                ownerId = ownerId,
                brand = brand,
                model = model,
                year = year,
                description = description,
                pricePerDay = pricePerDay,
                location = location,
                isAvailable = true,
                rating = 0f,
                ratingCount = 0
            )
            
            val result = insertCar(car)
            if (result <= 0) {
                throw Exception("Failed to insert car into database")
            }
            
            if (imageUrls.isNotEmpty()) {
                try {
                    val carImages = imageUrls.mapIndexed { index, url ->
                        CarImage(
                            imageId = UUID.randomUUID().toString(),
                            carId = carId,
                            imageUrl = url,
                            isPrimary = index == 0
                        )
                    }
                    insertCarImages(carImages)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            return car
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to create car: ${e.message}")
        }
    }
} 