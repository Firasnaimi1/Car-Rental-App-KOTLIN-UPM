package com.example.myapplication.ui.car

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.model.User
import com.example.myapplication.data.model.UserType
import com.example.myapplication.di.AppModule
import com.example.myapplication.ui.components.ErrorMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCarScreen(
    navController: NavController,
    currentUser: User,
    carViewModel: CarViewModel = viewModel(
        factory = CarViewModel.Factory(AppModule.provideCarRepository(androidx.compose.ui.platform.LocalContext.current))
    )
) {
    
    LaunchedEffect(Unit) {
        if (currentUser.userType != UserType.OWNER) {
            navController.navigateUp() // Navigate back if not an owner
        }
    }
    
    
    val carCreationState by carViewModel.carCreationState.collectAsState()
    
    // Effect to navigate back on successful car creation
    LaunchedEffect(carCreationState) {
        when (carCreationState) {
            is CarCreationState.Success -> {
               
                
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
                
                carViewModel.resetCarCreationState()
            }
            else -> { /* Do nothing for other states */ }
        }
    }
    
    // Only display the content if the user is an owner
    if (currentUser.userType == UserType.OWNER) {
        // Form state
        var brand by remember { mutableStateOf("") }
        var model by remember { mutableStateOf("") }
        var year by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var pricePerDay by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        var imageUrl by remember { mutableStateOf("") }
        
        // Validation state
        var formError by remember { mutableStateOf<String?>(null) }
        var isSubmitting by remember { mutableStateOf(false) }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Add New Car") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = carCreationState != CarCreationState.Loading
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = carCreationState != CarCreationState.Loading
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Year") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = carCreationState != CarCreationState.Loading
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5,
                    enabled = carCreationState != CarCreationState.Loading
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = pricePerDay,
                    onValueChange = { pricePerDay = it },
                    label = { Text("Price Per Day") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    enabled = carCreationState != CarCreationState.Loading
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location (City)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = carCreationState != CarCreationState.Loading
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image URL (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = carCreationState != CarCreationState.Loading
                )
                
                if (formError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (carCreationState is CarCreationState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = (carCreationState as CarCreationState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        // Validate inputs
                        formError = null // Clear previous errors
                        
                        if (brand.isBlank() || model.isBlank() || description.isBlank() || 
                            location.isBlank() || year.isBlank() || pricePerDay.isBlank()) {
                            formError = "All fields except image URL are required"
                            return@Button
                        }
                        
                        val yearInt = year.toIntOrNull()
                        if (yearInt == null) {
                            formError = "Year must be a valid number"
                            return@Button
                        }
                        
                        val price = pricePerDay.toDoubleOrNull()
                        if (price == null || price <= 0) {
                            formError = "Price must be a valid positive number"
                            return@Button
                        }
                        
                        // Additional verification that user is an owner
                        if (currentUser.userType != UserType.OWNER) {
                            formError = "Only car owners can add cars"
                            return@Button
                        }
                        
                        // Add the car
                        carViewModel.addCar(
                            ownerId = currentUser.userId,
                            brand = brand,
                            model = model,
                            year = yearInt,
                            description = description,
                            pricePerDay = price,
                            location = location,
                            imageUrls = if (imageUrl.isBlank()) emptyList() else listOf(imageUrl)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = carCreationState != CarCreationState.Loading
                ) {
                    if (carCreationState is CarCreationState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Add Car")
                    }
                }
            }
        }
    } else {
        // Show an unauthorized message if somehow a renter accesses this screen
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Unauthorized") },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Only car owners can add cars",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { navController.navigateUp() }
                    ) {
                        Text("Go Back")
                    }
                }
            }
        }
    }
} 