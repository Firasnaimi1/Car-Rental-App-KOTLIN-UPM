package com.example.myapplication.di

import android.content.Context
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.repository.CarRepository
import com.example.myapplication.data.repository.RatingRepository
import com.example.myapplication.data.repository.ReservationRepository
import com.example.myapplication.data.repository.UserRepository


object AppModule {
    
    private var database: AppDatabase? = null
    private var userRepository: UserRepository? = null
    private var carRepository: CarRepository? = null
    private var reservationRepository: ReservationRepository? = null
    private var ratingRepository: RatingRepository? = null
    
    fun provideDatabase(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            val instance = AppDatabase.getDatabase(context)
            database = instance
            instance
        }
    }
    
    fun provideUserRepository(context: Context): UserRepository {
        return userRepository ?: synchronized(this) {
            val db = provideDatabase(context)
            val instance = UserRepository(db.userDao())
            userRepository = instance
            instance
        }
    }
    
    fun provideCarRepository(context: Context): CarRepository {
        return carRepository ?: synchronized(this) {
            val db = provideDatabase(context)
            val instance = CarRepository(db.carDao(), context)
            carRepository = instance
            instance
        }
    }
    
    fun provideReservationRepository(context: Context): ReservationRepository {
        return reservationRepository ?: synchronized(this) {
            val db = provideDatabase(context)
            val instance = ReservationRepository(db.reservationDao())
            reservationRepository = instance
            instance
        }
    }
    
    fun provideRatingRepository(context: Context): RatingRepository {
        return ratingRepository ?: synchronized(this) {
            val db = provideDatabase(context)
            val userRepo = provideUserRepository(context)
            val carRepo = provideCarRepository(context)
            val instance = RatingRepository(
                db.ratingDao(),
                db.reservationDao(),
                carRepo,
                userRepo
            )
            ratingRepository = instance
            instance
        }
    }
} 