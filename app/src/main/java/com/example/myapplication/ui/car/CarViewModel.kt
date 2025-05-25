package com.example.myapplication.ui.car

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Car
import com.example.myapplication.data.model.CarImage
import com.example.myapplication.data.repository.CarRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CarViewModel(private val carRepository: CarRepository) : ViewModel() {

    private val _carsState = MutableStateFlow<CarsState>(CarsState.Loading)
    val carsState: StateFlow<CarsState> = _carsState

    private val _carDetailState = MutableStateFlow<CarDetailState>(CarDetailState.Loading)
    val carDetailState: StateFlow<CarDetailState> = _carDetailState
    
    private val _carState = MutableStateFlow<CarState>(CarState.Loading)
    val carState: StateFlow<CarState> = _carState

    private val _userCarsState = MutableStateFlow<CarsState>(CarsState.Loading)
    val userCarsState: StateFlow<CarsState> = _userCarsState
    
    private val _carCreationState = MutableStateFlow<CarCreationState>(CarCreationState.Idle)
    val carCreationState: StateFlow<CarCreationState> = _carCreationState

    private val _filterCity = MutableStateFlow<String?>(null)
    private val _filterMaxPrice = MutableStateFlow<Double?>(null)
    private val _filterModel = MutableStateFlow<String?>(null)

    private val filtersFlow = combine(
        _filterCity,
        _filterMaxPrice,
        _filterModel
    ) { city, maxPrice, model ->
        CarsFilter(city, maxPrice, model)
    }

    init {
        loadAllCars()

        viewModelScope.launch {
            filtersFlow.collect { filter ->
                applyFilters(filter)
            }
        }
    }

    fun loadAllCars() {
        viewModelScope.launch {
            _carsState.value = CarsState.Loading
            try {
                carRepository.getAvailableCars()
                    .collect { cars ->
                        if (cars.isEmpty()) {
                            _carsState.value = CarsState.Empty
                        } else {
                            _carsState.value = CarsState.Success(cars)
                        }
                    }
            } catch (e: Exception) {
                _carsState.value = CarsState.Error(e.message ?: "Failed to load cars")
            }
        }
    }

    fun loadCarDetail(carId: String) {
        viewModelScope.launch {
            _carDetailState.value = CarDetailState.Loading
            try {
                println("CarViewModel: Loading car details for $carId")
                val car = carRepository.getCarById(carId)
                if (car != null) {
                    // Use an empty list for now and update once we collect from Flow
                    val initialImages = emptyList<CarImage>()
                    println("CarViewModel: Got car, now loading images")
                    _carDetailState.value = CarDetailState.Success(car, initialImages)
                    
                    // Collect images from Flow in a separate coroutine
                    try {
                        carRepository.getCarImages(carId).collect { images ->
                            println("CarViewModel: Collected ${images.size} images")
                            _carDetailState.value = CarDetailState.Success(car, images)
                        }
                    } catch (e: Exception) {
                        println("CarViewModel ERROR loading images: ${e.message}")
                        // We already set the success state with empty images, so no need to update here
                    }
                    
                    println("CarViewModel: Setting success state with car: ${car.brand} ${car.model}")
                } else {
                    println("CarViewModel ERROR: Car not found for ID $carId")
                    _carDetailState.value = CarDetailState.Error("Car not found")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("CarViewModel ERROR: ${e.message}")
                _carDetailState.value = CarDetailState.Error(e.message ?: "Failed to load car details")
            }
        }
    }
    
    fun loadCarById(carId: String) {
        viewModelScope.launch {
            _carState.value = CarState.Loading
            try {
                val car = carRepository.getCarById(carId)
                if (car != null) {
                    _carState.value = CarState.Success(car)
                } else {
                    _carState.value = CarState.Error("Car not found")
                }
            } catch (e: Exception) {
                _carState.value = CarState.Error(e.message ?: "Failed to load car")
            }
        }
    }

    fun loadUserCars(userId: String) {
        viewModelScope.launch {
            _userCarsState.value = CarsState.Loading
            try {
                carRepository.getCarsByOwner(userId).collect { cars ->
                    if (cars.isEmpty()) {
                        _userCarsState.value = CarsState.Empty
                    } else {
                        _userCarsState.value = CarsState.Success(cars)
                    }
                }
            } catch (e: Exception) {
                _userCarsState.value = CarsState.Error(e.message ?: "Failed to load your cars")
            }
        }
    }

    private fun applyFilters(filter: CarsFilter) {
        viewModelScope.launch {
            _carsState.value = CarsState.Loading
            try {
                when {
                    filter.city != null && filter.maxPrice != null && filter.model != null -> {
                        carRepository.searchCarsByCityAndPrice(filter.city, filter.maxPrice)
                            .map { cars -> cars.filter { it.model.contains(filter.model, true) } }
                            .collect { cars ->
                                if (cars.isEmpty()) _carsState.value = CarsState.Empty
                                else _carsState.value = CarsState.Success(cars)
                            }
                    }
                    filter.city != null && filter.maxPrice != null -> {
                        carRepository.searchCarsByCityAndPrice(filter.city, filter.maxPrice)
                            .collect { cars ->
                                if (cars.isEmpty()) _carsState.value = CarsState.Empty
                                else _carsState.value = CarsState.Success(cars)
                            }
                    }
                    filter.city != null -> {
                        carRepository.searchCarsByCity(filter.city)
                            .collect { cars ->
                                if (cars.isEmpty()) _carsState.value = CarsState.Empty
                                else _carsState.value = CarsState.Success(cars)
                            }
                    }
                    filter.maxPrice != null -> {
                        carRepository.searchCarsByMaxPrice(filter.maxPrice)
                            .collect { cars ->
                                if (cars.isEmpty()) _carsState.value = CarsState.Empty
                                else _carsState.value = CarsState.Success(cars)
                            }
                    }
                    filter.model != null -> {
                        carRepository.searchCarsByModel(filter.model)
                            .collect { cars ->
                                if (cars.isEmpty()) _carsState.value = CarsState.Empty
                                else _carsState.value = CarsState.Success(cars)
                            }
                    }
                    else -> {
                        loadAllCars()
                    }
                }
            } catch (e: Exception) {
                _carsState.value = CarsState.Error(e.message ?: "Failed to filter cars")
            }
        }
    }

    fun setFilterCity(city: String?) {
        _filterCity.value = city
    }

    fun setFilterMaxPrice(maxPrice: Double?) {
        _filterMaxPrice.value = maxPrice
    }

    fun setFilterModel(model: String?) {
        _filterModel.value = model
    }

    fun setFilters(city: String? = null, maxPrice: Double? = null, model: String? = null) {
        _filterCity.value = city
        _filterMaxPrice.value = maxPrice
        _filterModel.value = model
    }

    fun clearFilters() {
        _filterCity.value = null
        _filterMaxPrice.value = null
        _filterModel.value = null
    }
    
    fun resetCarCreationState() {
        _carCreationState.value = CarCreationState.Idle
    }

    fun addCar(
        ownerId: String,
        brand: String,
        model: String,
        year: Int,
        description: String,
        pricePerDay: Double,
        location: String,
        imageUri: android.net.Uri?
    ) {
        viewModelScope.launch {
            _carCreationState.value = CarCreationState.Loading
            try {
                val car = carRepository.createCar(
                    ownerId = ownerId,
                    brand = brand,
                    model = model,
                    year = year,
                    description = description,
                    pricePerDay = pricePerDay,
                    location = location,
                    imageUri = imageUri
                )
                loadUserCars(ownerId)
                _carCreationState.value = CarCreationState.Success(car)
            } catch (e: Exception) {
                _carCreationState.value = CarCreationState.Error(e.message ?: "Failed to add car")
            }
        }
    }

    fun updateCar(
        carId: String,
        ownerId: String,
        brand: String,
        model: String,
        year: Int,
        description: String,
        pricePerDay: Double,
        location: String,
        imageUri: android.net.Uri?
    ) {
        viewModelScope.launch {
            _carCreationState.value = CarCreationState.Loading
            try {
                val existingCar = carRepository.getCarById(carId)
                if (existingCar == null || existingCar.ownerId != ownerId) {
                    _carCreationState.value = CarCreationState.Error("You can only update your own cars")
                    return@launch
                }
                
                val updatedCar = existingCar.copy(
                    brand = brand,
                    model = model,
                    year = year,
                    description = description,
                    pricePerDay = pricePerDay,
                    location = location,
                    imageUri = if (imageUri == null) existingCar.imageUri else null
                )
                
                carRepository.updateCar(updatedCar)
                
                // Update image if new one was provided
                if (imageUri != null) {
                    carRepository.updateCarImage(carId, imageUri)
                }
                
                loadUserCars(ownerId)
                _carCreationState.value = CarCreationState.Success(updatedCar)
            } catch (e: Exception) {
                _carCreationState.value = CarCreationState.Error(e.message ?: "Failed to update car")
            }
        }
    }
    
    fun deleteCar(carId: String) {
        viewModelScope.launch {
            try {
                // Get the car first to check ownership
                val car = carRepository.getCarById(carId)
                if (car != null) {
                    carRepository.deleteCar(car)
                    // Refresh user cars
                    loadUserCars(car.ownerId)
                }
            } catch (e: Exception) {
                // Handle error
                println("Error deleting car: ${e.message}")
            }
        }
    }
    
    fun updateCar(car: Car) {
        viewModelScope.launch {
            try {
                // First update the car data
                carRepository.updateCar(car)
                
                // If the car has an imageUri, ensure it's properly stored in both the car and car_images tables
                if (car.imageUri != null && car.imageUri.isNotEmpty()) {
                    try {
                        // Convert the string URI back to a Uri object
                        val uri = Uri.parse(car.imageUri)
                        // Save the image in car_images table and update the car's imageUri
                        carRepository.updateCarImage(car.carId, uri)
                    } catch (e: Exception) {
                        // Just log the error and continue - the car data is already saved
                        e.printStackTrace()
                        println("Error saving car image: ${e.message}")
                    }
                }
                
                // Update all states
                _carState.value = CarState.Success(car)
                loadCarDetail(car.carId)
                // Also update user cars list
                loadUserCars(car.ownerId)
                // Update creation state to indicate success
                _carCreationState.value = CarCreationState.Success(car)
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error updating car: ${e.message}")
                _carDetailState.value = CarDetailState.Error(e.message ?: "Failed to update car")
                _carState.value = CarState.Error(e.message ?: "Failed to update car")
                _carCreationState.value = CarCreationState.Error(e.message ?: "Failed to update car")
            }
        }
    }

    class Factory(private val carRepository: CarRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CarViewModel::class.java)) {
                return CarViewModel(carRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

sealed class CarsState {
    object Loading : CarsState()
    object Empty : CarsState()
    data class Success(val cars: List<Car>) : CarsState()
    data class Error(val message: String) : CarsState()
}

sealed class CarDetailState {
    object Loading : CarDetailState()
    data class Success(val car: Car, val images: List<CarImage>) : CarDetailState()
    data class Error(val message: String) : CarDetailState()
}

sealed class CarCreationState {
    object Idle : CarCreationState()
    object Loading : CarCreationState()
    data class Success(val car: Car) : CarCreationState()
    data class Error(val message: String) : CarCreationState()
}

data class CarsFilter(
    val city: String? = null,
    val maxPrice: Double? = null,
    val model: String? = null
)

// State for individual car loading
sealed class CarState {
    object Loading : CarState()
    data class Success(val car: Car) : CarState()
    data class Error(val message: String) : CarState()
}