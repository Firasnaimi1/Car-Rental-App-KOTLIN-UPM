package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.di.AppModule
import com.example.myapplication.ui.auth.AuthViewModel
import com.example.myapplication.ui.auth.LoginScreen
import com.example.myapplication.ui.auth.RegisterScreen
import com.example.myapplication.ui.car.AddCarScreen
import com.example.myapplication.ui.car.CarDetailScreen
import com.example.myapplication.ui.car.MyCarsScreen
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.ui.reservation.MyReservationsScreen
import com.example.myapplication.ui.reservation.ReservationRequestsScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.profile.ProfileScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CarRentalApp()
                }
            }
        }
    }
}

@Composable
fun CarRentalApp() {
    val navController = rememberNavController()
    
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(
            AppModule.provideUserRepository(LocalContext.current)
        )
    )
    
    val currentUser by authViewModel.currentUser.collectAsState()
    
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            println("MainActivity: Current user is ${currentUser?.fullName}")
        } else {
            println("MainActivity: Current user is NULL")
        }
    }
    
    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(navController, authViewModel)
            }
            
            composable("register") {
                RegisterScreen(navController, authViewModel)
            }
            
            composable("home") {
                currentUser?.let { user ->
                    HomeScreen(navController, user)
                } ?: run {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            }
            
            composable("car_details/{carId}") { backStackEntry ->
                val carId = backStackEntry.arguments?.getString("carId") ?: ""
                currentUser?.let { user ->
                    CarDetailScreen(navController, carId, user)
                } ?: run {
                    navController.navigate("login") {
                        popUpTo("car_details/{carId}") { inclusive = true }
                    }
                }
            }
            
            composable("add_car") {
                currentUser?.let { user ->
                    AddCarScreen(navController, user)
                } ?: run {
                    navController.navigate("login")
                }
            }
            
            composable("my_cars") {
                currentUser?.let { user ->
                    MyCarsScreen(navController, user)
                } ?: run {
                    navController.navigate("login")
                }
            }
            
            composable("my_reservations") {
                currentUser?.let { user ->
                    MyReservationsScreen(navController, user)
                } ?: run {
                    navController.navigate("login")
                }
            }
            
            composable("reservation_requests") {
                currentUser?.let { user ->
                    ReservationRequestsScreen(navController, user)
                } ?: run {
                    navController.navigate("login")
                }
            }
            
            composable("profile") {
                currentUser?.let { user ->
                    ProfileScreen(navController, user)
                } ?: run {
                    navController.navigate("login")
                }
            }
        }
    }
}