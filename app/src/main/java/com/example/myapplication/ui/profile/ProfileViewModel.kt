package com.example.myapplication.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.User
import com.example.myapplication.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(private val userRepository: UserRepository) : ViewModel() {

    // Profile state
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState

    // Edit profile state
    private val _editProfileState = MutableStateFlow<EditProfileState>(EditProfileState.Idle)
    val editProfileState: StateFlow<EditProfileState> = _editProfileState

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                val user = userRepository.getUserById(userId)
                if (user != null) {
                    _profileState.value = ProfileState.Success(user)
                } else {
                    _profileState.value = ProfileState.Error("User not found")
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    fun updateUserProfile(
        user: User,
        newFullName: String,
        newPhoneNumber: String,
        newAddress: String
    ) {
        if (newFullName.isBlank() || newPhoneNumber.isBlank() || newAddress.isBlank()) {
            _editProfileState.value = EditProfileState.Error("All fields are required")
            return
        }

        _editProfileState.value = EditProfileState.Loading
        viewModelScope.launch {
            try {
                val updatedUser = user.copy(
                    fullName = newFullName,
                    phoneNumber = newPhoneNumber,
                    address = newAddress
                )

                userRepository.updateUser(updatedUser)
                _editProfileState.value = EditProfileState.Success(updatedUser)
                
                // Update profile state
                _profileState.value = ProfileState.Success(updatedUser)
            } catch (e: Exception) {
                _editProfileState.value = EditProfileState.Error(e.message ?: "Failed to update profile")
            }
        }
    }

    fun resetEditProfileState() {
        _editProfileState.value = EditProfileState.Idle
    }

    // Factory for creating the ViewModel with dependencies
    class Factory(private val userRepository: UserRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                return ProfileViewModel(userRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// States for profile UI
sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val user: User) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

// States for profile editing
sealed class EditProfileState {
    object Idle : EditProfileState()
    object Loading : EditProfileState()
    data class Success(val user: User) : EditProfileState()
    data class Error(val message: String) : EditProfileState()
} 