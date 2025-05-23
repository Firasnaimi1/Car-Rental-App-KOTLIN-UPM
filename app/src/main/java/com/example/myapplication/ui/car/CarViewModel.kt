package com.example.myapplication.ui.car

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
                    val carImages = try {
                        println("CarViewModel: Got car, now loading images")
                        emptyList<CarImage>()
                    } catch (e: Exception) {
                        println("CarViewModel ERROR loading images: ${e.message}")
                        emptyList<CarImage>()
                    }
                    
                    println("CarViewModel: Setting success state with car: ${car.brand} ${car.model}")
                    _carDetailState.value = CarDetailState.Success(car, carImages)
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
        imageUrls: List<String>
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
                    imageUrls = imageUrls
                )
                loadUserCars(ownerId)
                _carCreationState.value = CarCreationState.Success(car)
            } catch (e: Exception) {
                _carCreationState.value = CarCreationState.Error(e.message ?: "Failed to add car")
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