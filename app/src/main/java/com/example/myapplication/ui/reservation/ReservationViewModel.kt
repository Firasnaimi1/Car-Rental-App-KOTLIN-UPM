package com.example.myapplication.ui.reservation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Car
import com.example.myapplication.data.model.Reservation
import com.example.myapplication.data.model.ReservationStatus
import com.example.myapplication.data.repository.CarRepository
import com.example.myapplication.data.repository.ReservationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReservationViewModel(
    private val reservationRepository: ReservationRepository,
    private val carRepository: CarRepository
) : ViewModel() {

    // Renter reservations
    private val _renterReservationsState = MutableStateFlow<ReservationsState>(ReservationsState.Loading)
    val renterReservationsState: StateFlow<ReservationsState> = _renterReservationsState

    // Owner reservation requests
    private val _ownerReservationsState = MutableStateFlow<ReservationsState>(ReservationsState.Loading)
    val ownerReservationsState: StateFlow<ReservationsState> = _ownerReservationsState

    // Reservation creation state
    private val _createReservationState = MutableStateFlow<CreateReservationState>(CreateReservationState.Idle)
    val createReservationState: StateFlow<CreateReservationState> = _createReservationState

    fun loadRenterReservations(renterId: String) {
        viewModelScope.launch {
            _renterReservationsState.value = ReservationsState.Loading
            try {
                reservationRepository.getReservationsByRenterId(renterId)
                    .collect { reservations ->
                        if (reservations.isEmpty()) {
                            _renterReservationsState.value = ReservationsState.Empty
                        } else {
                            _renterReservationsState.value = ReservationsState.Success(reservations)
                        }
                    }
            } catch (e: Exception) {
                _renterReservationsState.value = ReservationsState.Error(e.message ?: "Failed to load reservations")
            }
        }
    }

    fun loadOwnerPendingReservations(ownerId: String) {
        viewModelScope.launch {
            _ownerReservationsState.value = ReservationsState.Loading
            try {
                reservationRepository.getOwnerReservationsByStatus(ownerId, ReservationStatus.PENDING)
                    .collect { reservations ->
                        if (reservations.isEmpty()) {
                            _ownerReservationsState.value = ReservationsState.Empty
                        } else {
                            _ownerReservationsState.value = ReservationsState.Success(reservations)
                        }
                    }
            } catch (e: Exception) {
                _ownerReservationsState.value = ReservationsState.Error(e.message ?: "Failed to load reservation requests")
            }
        }
    }

    fun createReservation(
        carId: String,
        renterId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ) {
        if (startDate.isAfter(endDate)) {
            _createReservationState.value = CreateReservationState.Error("End date cannot be before start date")
            return
        }

        if (startDate.isBefore(LocalDate.now())) {
            _createReservationState.value = CreateReservationState.Error("Start date cannot be in the past")
            return
        }

        _createReservationState.value = CreateReservationState.Loading
        viewModelScope.launch {
            try {
                // Get car details to calculate price
                val car = carRepository.getCarById(carId)
                if (car == null) {
                    _createReservationState.value = CreateReservationState.Error("Car not found")
                    return@launch
                }

                // Create reservation
                val reservation = reservationRepository.createReservation(
                    carId = carId,
                    renterId = renterId,
                    startDate = startDate,
                    endDate = endDate,
                    pricePerDay = car.pricePerDay
                )

                if (reservation != null) {
                    _createReservationState.value = CreateReservationState.Success(reservation)
                } else {
                    _createReservationState.value = CreateReservationState.Error("Selected dates are already booked")
                }
            } catch (e: Exception) {
                _createReservationState.value = CreateReservationState.Error(e.message ?: "Failed to create reservation")
            }
        }
    }

    fun approveReservation(reservationId: String, ownerId: String) {
        viewModelScope.launch {
            try {
                // First verify the user is the owner of the car
                val reservation = reservationRepository.getReservationById(reservationId)
                if (reservation != null) {
                    val car = carRepository.getCarById(reservation.carId)
                    if (car != null && car.ownerId == ownerId) {
                        val success = reservationRepository.processReservationResponse(reservationId, true)
                        if (success) {
                            // Refresh the owner's pending reservations
                            loadOwnerPendingReservations(ownerId)
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error - could update state for UI feedback
            }
        }
    }

    fun rejectReservation(reservationId: String, ownerId: String) {
        viewModelScope.launch {
            try {
                // First verify the user is the owner of the car
                val reservation = reservationRepository.getReservationById(reservationId)
                if (reservation != null) {
                    val car = carRepository.getCarById(reservation.carId)
                    if (car != null && car.ownerId == ownerId) {
                        val success = reservationRepository.processReservationResponse(reservationId, false)
                        if (success) {
                            // Refresh the owner's pending reservations
                            loadOwnerPendingReservations(ownerId)
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error - could update state for UI feedback
            }
        }
    }

    fun cancelReservation(reservationId: String, renterId: String) {
        viewModelScope.launch {
            try {
                // Verify the user is the actual renter
                val reservation = reservationRepository.getReservationById(reservationId)
                if (reservation != null && reservation.renterId == renterId) {
                    val success = reservationRepository.cancelReservation(reservationId)
                    if (success) {
                        // Refresh the renter's reservations
                        loadRenterReservations(renterId)
                    }
                }
            } catch (e: Exception) {
                // Handle error - could update state for UI feedback
            }
        }
    }

    fun payForReservation(reservationId: String, renterId: String) {
        viewModelScope.launch {
            try {
                // Verify the user is the actual renter
                val reservation = reservationRepository.getReservationById(reservationId)
                if (reservation != null && reservation.renterId == renterId) {
                    reservationRepository.markReservationAsPaid(reservationId)
                    // Refresh the renter's reservations
                    loadRenterReservations(renterId)
                }
            } catch (e: Exception) {
                // Handle payment error
            }
        }
    }

    fun resetCreateReservationState() {
        _createReservationState.value = CreateReservationState.Idle
    }

    // Factory for creating the ViewModel with dependencies
    class Factory(
        private val reservationRepository: ReservationRepository,
        private val carRepository: CarRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReservationViewModel::class.java)) {
                return ReservationViewModel(reservationRepository, carRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// States for reservation list UI
sealed class ReservationsState {
    object Loading : ReservationsState()
    object Empty : ReservationsState()
    data class Success(val reservations: List<Reservation>) : ReservationsState()
    data class Error(val message: String) : ReservationsState()
}

// States for reservation creation
sealed class CreateReservationState {
    object Idle : CreateReservationState()
    object Loading : CreateReservationState()
    data class Success(val reservation: Reservation) : CreateReservationState()
    data class Error(val message: String) : CreateReservationState()
} 