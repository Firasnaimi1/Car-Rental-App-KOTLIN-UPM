package com.example.myapplication.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.User
import com.example.myapplication.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

class ProfileViewModel(private val userRepository: UserRepository, private val context: Context) : ViewModel() {

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
        newAddress: String,
        imageUri: Uri? = null
    ) {
        if (newFullName.isBlank() || newPhoneNumber.isBlank() || newAddress.isBlank()) {
            _editProfileState.value = EditProfileState.Error("All fields are required")
            return
        }

        _editProfileState.value = EditProfileState.Loading
        viewModelScope.launch {
            try {
                var profileImageUri = user.profileImageUri
                
                // Handle new image if provided
                if (imageUri != null) {
                    val filename = "profile_${user.userId}_${UUID.randomUUID()}.jpg"
                    profileImageUri = saveImageToInternalStorage(imageUri, filename)
                }
                
                val updatedUser = user.copy(
                    fullName = newFullName,
                    phoneNumber = newPhoneNumber,
                    address = newAddress,
                    profileImageUri = profileImageUri
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
    
    private suspend fun saveImageToInternalStorage(imageUri: Uri, filename: String): String = withContext(Dispatchers.IO) {
        try {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source)
            }
            
            val file = File(context.filesDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            return@withContext file.toUri().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Failed to save image: ${e.message}")
        }
    }

    fun resetEditProfileState() {
        _editProfileState.value = EditProfileState.Idle
    }

    // Factory for creating the ViewModel with dependencies
    class Factory(private val userRepository: UserRepository, private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                return ProfileViewModel(userRepository, context) as T
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