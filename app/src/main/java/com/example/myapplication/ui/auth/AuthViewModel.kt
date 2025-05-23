package com.example.myapplication.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.User
import com.example.myapplication.data.model.UserType
import com.example.myapplication.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("Email and password cannot be empty")
            return
        }

        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val user = userRepository.authenticateUser(email, password)
                if (user != null) {
                    _currentUser.value = user
                    _loginState.value = LoginState.Success(user)
                } else {
                    _loginState.value = LoginState.Error("Invalid email or password")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Authentication failed")
            }
        }
    }

    fun register(
        email: String,
        password: String,
        confirmPassword: String,
        fullName: String,
        phoneNumber: String,
        address: String,
        userType: UserType
    ) {
        if (email.isBlank() || password.isBlank() || fullName.isBlank() || 
            phoneNumber.isBlank() || address.isBlank()
        ) {
            _registrationState.value = RegistrationState.Error("All fields are required")
            return
        }

        if (password != confirmPassword) {
            _registrationState.value = RegistrationState.Error("Passwords do not match")
            return
        }

        _registrationState.value = RegistrationState.Loading
        viewModelScope.launch {
            try {
                val existingUser = userRepository.getUserByEmail(email)
                if (existingUser != null) {
                    _registrationState.value = RegistrationState.Error("Email already registered")
                    return@launch
                }

                val newUser = userRepository.createUser(
                    email = email,
                    password = password,
                    fullName = fullName,
                    phoneNumber = phoneNumber,
                    address = address,
                    userType = userType
                )

                _currentUser.value = newUser
                _registrationState.value = RegistrationState.Success(newUser)
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                userRepository.clearCurrentUser()
                
                _loginState.value = LoginState.Idle
                _registrationState.value = RegistrationState.Idle
                _currentUser.value = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    fun resetRegistrationState() {
        _registrationState.value = RegistrationState.Idle
    }

    class Factory(private val userRepository: UserRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(userRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}

sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()
    data class Success(val user: User) : RegistrationState()
    data class Error(val message: String) : RegistrationState()
} 