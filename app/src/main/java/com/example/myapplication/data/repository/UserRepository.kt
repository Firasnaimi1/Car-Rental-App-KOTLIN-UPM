package com.example.myapplication.data.repository

import com.example.myapplication.data.database.UserDao
import com.example.myapplication.data.model.User
import com.example.myapplication.data.model.UserType
import kotlinx.coroutines.flow.Flow
import java.util.*

class UserRepository(private val userDao: UserDao) {

    suspend fun insertUser(user: User) = userDao.insertUser(user)
    
    suspend fun updateUser(user: User) = userDao.updateUser(user)
    
    suspend fun deleteUser(user: User) = userDao.deleteUser(user)
    
    suspend fun getUserById(userId: String) = userDao.getUserById(userId)
    
    suspend fun getUserByEmail(email: String) = userDao.getUserByEmail(email)
    
    fun getUsersByType(userType: UserType) = userDao.getUsersByType(userType)
    
    fun getAllUsers() = userDao.getAllUsers()
    
    suspend fun updateUserRating(userId: String, newRating: Float, newCount: Int) = 
        userDao.updateUserRating(userId, newRating, newCount)
    
    suspend fun createUser(
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String,
        address: String,
        userType: UserType
    ): User {
        val newUser = User(
            userId = UUID.randomUUID().toString(),
            email = email,
            password = password,
            fullName = fullName,
            phoneNumber = phoneNumber,
            address = address,
            userType = userType
        )
        insertUser(newUser)
        return newUser
    }
    
    suspend fun authenticateUser(email: String, password: String): User? {
        val user = getUserByEmail(email) ?: return null
        return if (user.password == password) user else null
    }

    suspend fun clearCurrentUser() {
    }
} 