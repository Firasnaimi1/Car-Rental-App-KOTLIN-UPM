package com.example.myapplication.ui.car

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.model.User
import com.example.myapplication.di.AppModule
import com.example.myapplication.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCarsScreen(
    navController: NavController,
    currentUser: User,
    carViewModel: CarViewModel = viewModel(
        factory = CarViewModel.Factory(AppModule.provideCarRepository(androidx.compose.ui.platform.LocalContext.current))
    )
) {
    val userCarsState by carViewModel.userCarsState.collectAsState()

    // Load owner's cars
    LaunchedEffect(currentUser.userId) {
        carViewModel.loadUserCars(currentUser.userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Cars") },
                actions = {
                    // Profile button
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_car") }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Car")
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }},
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already on My Cars screen */ },
                    icon = { Icon(Icons.Outlined.DirectionsCar, contentDescription = "My Cars") },
                    label = { Text("My Cars") }
                )
                
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate("reservation_requests") },
                    icon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Requests") },
                    label = { Text("Requests") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = userCarsState) {
                is CarsState.Loading -> LoadingState()
                
                is CarsState.Empty -> EmptyState(
                    message = "You haven't listed any cars yet.\nClick the + button to add your first car."
                )
                
                is CarsState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(state.cars) { car ->
                            CarItem(
                                car = car,
                                onClick = { navController.navigate("car_details/${car.carId}") }
                            )
                        }
                    }
                }
                
                is CarsState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { carViewModel.loadUserCars(currentUser.userId) }
                )
            }
        }
    }
} 