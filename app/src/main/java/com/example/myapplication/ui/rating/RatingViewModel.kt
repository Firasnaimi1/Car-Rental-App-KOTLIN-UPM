package com.example.myapplication.ui.rating

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.Rating
import com.example.myapplication.data.repository.RatingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RatingViewModel(private val ratingRepository: RatingRepository) : ViewModel() {

    // Car ratings state
    private val _carRatingsState = MutableStateFlow<RatingsState>(RatingsState.Loading)
    val carRatingsState: StateFlow<RatingsState> = _carRatingsState

    // Rating submission state
    private val _submitRatingState = MutableStateFlow<SubmitRatingState>(SubmitRatingState.Idle)
    val submitRatingState: StateFlow<SubmitRatingState> = _submitRatingState

    fun loadCarRatings(carId: String) {
        viewModelScope.launch {
            _carRatingsState.value = RatingsState.Loading
            try {
                ratingRepository.getRatingsByCarId(carId).collect { ratings ->
                    if (ratings.isEmpty()) {
                        _carRatingsState.value = RatingsState.Empty
                    } else {
                        _carRatingsState.value = RatingsState.Success(ratings)
                    }
                }
            } catch (e: Exception) {
                _carRatingsState.value = RatingsState.Error(e.message ?: "Failed to load ratings")
            }
        }
    }

    fun submitRating(
        userId: String,
        carId: String,
        reservationId: String,
        score: Float,
        comment: String?
    ) {
        // Validate inputs
        if (score < 1 || score > 5) {
            _submitRatingState.value = SubmitRatingState.Error("Rating must be between 1 and 5")
            return
        }

        _submitRatingState.value = SubmitRatingState.Loading
        viewModelScope.launch {
            try {
                val rating = ratingRepository.addRating(
                    userId = userId,
                    carId = carId,
                    reservationId = reservationId,
                    score = score,
                    comment = comment
                )

                if (rating != null) {
                    _submitRatingState.value = SubmitRatingState.Success(rating)
                    // Refresh ratings for this car
                    loadCarRatings(carId)
                } else {
                    _submitRatingState.value = SubmitRatingState.Error("You can only rate after a completed reservation")
                }
            } catch (e: Exception) {
                _submitRatingState.value = SubmitRatingState.Error(e.message ?: "Failed to submit rating")
            }
        }
    }

    fun checkIfUserCanRate(userId: String, reservationId: String) {
        viewModelScope.launch {
            try {
                // Check if rating already exists
                val existingRating = ratingRepository.getRatingByReservationId(reservationId)
                if (existingRating != null) {
                    _submitRatingState.value = SubmitRatingState.AlreadyRated
                }
            } catch (e: Exception) {
                // Ignore errors here, as the user will just see the rating form
            }
        }
    }

    fun resetSubmitRatingState() {
        _submitRatingState.value = SubmitRatingState.Idle
    }

    // Factory for creating the ViewModel with dependencies
    class Factory(private val ratingRepository: RatingRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RatingViewModel::class.java)) {
                return RatingViewModel(ratingRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// States for ratings list UI
sealed class RatingsState {
    object Loading : RatingsState()
    object Empty : RatingsState()
    data class Success(val ratings: List<Rating>) : RatingsState()
    data class Error(val message: String) : RatingsState()
}

// States for rating submission
sealed class SubmitRatingState {
    object Idle : SubmitRatingState()
    object Loading : SubmitRatingState()
    object AlreadyRated : SubmitRatingState()
    data class Success(val rating: Rating) : SubmitRatingState()
    data class Error(val message: String) : SubmitRatingState()
} 