package com.example.myapplication

import android.app.Application
import com.example.myapplication.di.AppModule

class CarRentalApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        AppModule.provideDatabase(this)
    }
} 