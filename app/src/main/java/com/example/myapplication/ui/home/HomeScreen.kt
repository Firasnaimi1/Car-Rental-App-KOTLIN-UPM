package com.example.myapplication.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.model.User
import com.example.myapplication.data.model.UserType
import com.example.myapplication.di.AppModule
import com.example.myapplication.ui.car.CarViewModel
import com.example.myapplication.ui.car.CarsState
import com.example.myapplication.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    currentUser: User,
    carViewModel: CarViewModel = viewModel(
        factory = CarViewModel.Factory(AppModule.provideCarRepository(androidx.compose.ui.platform.LocalContext.current))
    )
) {
    val carsState by carViewModel.carsState.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }
    
    var city by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Car Rental") },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.FilterAlt,
                            contentDescription = "Filter"
                        )
                    }
                    
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
            if (currentUser.userType == UserType.OWNER) {
                FloatingActionButton(
                    onClick = { navController.navigate("add_car") }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Car")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already on home screen */ },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                
                if (currentUser.userType == UserType.OWNER) {
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate("my_cars") },
                        icon = { Icon(Icons.Outlined.DirectionsCar, contentDescription = "My Cars") },
                        label = { Text("My Cars") }
                    )
                    
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate("reservation_requests") },
                        icon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Requests") },
                        label = { Text("Requests") }
                    )
                } else {
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate("my_reservations") },
                        icon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = "My Reservations") },
                        label = { Text("Reservations") }
                    )
                }
            }
        }
    ) { padding ->
        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Filter Cars") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = maxPrice,
                            onValueChange = { maxPrice = it },
                            label = { Text("Max Price Per Day") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Car Model") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            carViewModel.setFilters(
                                city = if (city.isBlank()) null else city,
                                maxPrice = maxPrice.toDoubleOrNull(),
                                model = if (model.isBlank()) null else model
                            )
                            showFilterDialog = false
                        }
                    ) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            city = ""
                            maxPrice = ""
                            model = ""
                            carViewModel.clearFilters()
                            showFilterDialog = false
                        }
                    ) {
                        Text("Clear")
                    }
                }
            )
        }
    
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (carsState) {
                is CarsState.Loading -> {
                    LoadingSpinner()
                }
                is CarsState.Empty -> {
                    EmptyListMessage(message = "No cars available")
                }
                is CarsState.Success -> {
                    val cars = (carsState as CarsState.Success).cars
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(cars) { car ->
                            CarItem(
                                car = car,
                                onClick = {
                                    navController.navigate("car_details/${car.carId}")
                                }
                            )
                        }
                    }
                }
                is CarsState.Error -> {
                    ErrorMessage(
                        message = (carsState as CarsState.Error).message,
                        onRetry = { carViewModel.loadAllCars() }
                    )
                }
            }
        }
    }
} 