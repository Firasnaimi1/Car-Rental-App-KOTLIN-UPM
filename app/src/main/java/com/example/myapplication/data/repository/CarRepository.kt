package com.example.myapplication.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import com.example.myapplication.data.database.CarDao
import com.example.myapplication.data.model.Car
import com.example.myapplication.data.model.CarImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

class CarRepository(private val carDao: CarDao, private val context: Context) {

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
    
    private suspend fun saveImageToInternalStorage(imageUri: Uri, filename: String): String = withContext(Dispatchers.IO) {
        try {
            // Create a directory for car images if it doesn't exist
            val directory = File(context.filesDir, "car_images")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            // Create a file to save the image
            val file = File(directory, "${UUID.randomUUID()}.jpg")
            
            // Get the bitmap from the URI
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source)
            }
            
            // Save the bitmap to file
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            // Return the file path
            return@withContext file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            // Return empty string instead of throwing exception
            return@withContext ""
        }
    }
    
    suspend fun updateCarImage(carId: String, imageUri: Uri) = withContext(Dispatchers.IO) {
        try {
            // Simply store the original URI string
            val savedImageUri = saveImageToInternalStorage(imageUri, "car_image")
            
            // Get existing primary image
            val existingImage = carDao.getCarPrimaryImage(carId)
            
            if (existingImage != null) {
                // Update the existing image with new URI
                val updatedImage = existingImage.copy(imageUrl = savedImageUri)
                carDao.updateCarImage(updatedImage)
            } else {
                // Create a new image if none exists
                val newImage = CarImage(
                    imageId = UUID.randomUUID().toString(),
                    carId = carId,
                    imageUrl = savedImageUri,
                    isPrimary = true
                )
                carDao.insertCarImage(newImage)
            }
            
            // Most importantly, update the imageUri in the Car object itself
            val car = carDao.getCarById(carId)
            if (car != null) {
                val updatedCar = car.copy(imageUri = savedImageUri)
                carDao.updateCar(updatedCar)
            }
        } catch (e: Exception) {
            // Log error but don't crash
            e.printStackTrace()
        }
    }
    
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
        imageUri: Uri?
    ): Car {
        try {
            val carId = UUID.randomUUID().toString()
            var savedImagePath = ""
            
            // Save the image to internal storage if provided
            if (imageUri != null) {
                savedImagePath = saveImageToInternalStorage(imageUri, "car_${carId}_${System.currentTimeMillis()}")
            }
            
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
                ratingCount = 0,
                imageUri = if (savedImagePath.isNotEmpty()) savedImagePath else null
            )
            
            val result = insertCar(car)
            if (result <= 0) {
                throw Exception("Failed to insert car into database")
            }
            
            // Save the image reference in the car_images table if we have a saved image
            if (savedImagePath.isNotEmpty()) {
                try {
                    val carImage = CarImage(
                        imageId = UUID.randomUUID().toString(),
                        carId = carId,
                        imageUrl = savedImagePath,
                        isPrimary = true
                    )
                    insertCarImage(carImage)
                } catch (e: Exception) {
                    // Just log the error but continue
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