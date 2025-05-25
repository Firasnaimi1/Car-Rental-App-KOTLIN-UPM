package com.example.myapplication.ui.car

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.data.model.User
import com.example.myapplication.data.model.UserType
import com.example.myapplication.di.AppModule
import com.example.myapplication.ui.components.ErrorMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCarScreen(
    navController: NavController,
    currentUser: User,
    editCarId: String = "",
    carViewModel: CarViewModel = viewModel(
        factory = CarViewModel.Factory(AppModule.provideCarRepository(androidx.compose.ui.platform.LocalContext.current))
    )
) {
    // Determine if we're editing or adding a car
    val isEditing = editCarId.isNotEmpty()
    val screenTitle = if (isEditing) "Edit Car" else "Add Car"
    
    // Load car data if editing
    LaunchedEffect(editCarId) {
        if (isEditing) {
            carViewModel.loadCarById(editCarId)
        }
    }
    
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
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        
        // If editing, get the car details to populate the form
        val carState by carViewModel.carState.collectAsState(initial = CarState.Loading)
        LaunchedEffect(carState) {
            if (isEditing && carState is CarState.Success) {
                val car = (carState as CarState.Success).car
                brand = car.brand
                model = car.model
                year = car.year.toString()
                description = car.description
                pricePerDay = car.pricePerDay.toString()
                location = car.location
                // Only use the imageUri if we don't already have a selected image
                if (selectedImageUri == null && car.imageUri != null && car.imageUri.isNotEmpty()) {
                    try {
                        selectedImageUri = Uri.parse(car.imageUri)
                    } catch (e: Exception) {
                        // If parsing fails, leave it null
                    }
                }
            }
        }
        
        // Image picker launcher
        val context = LocalContext.current
        val imagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            selectedImageUri = uri
        }
        
        // Validation state
        var formError by remember { mutableStateOf<String?>(null) }
        var isSubmitting by remember { mutableStateOf(false) }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(screenTitle) },
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Image Selection UI
                Text(
                    text = "Car Image",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (selectedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedImageUri),
                            contentDescription = "Selected Car Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, MaterialTheme.colorScheme.outline),
                            contentScale = ContentScale.Crop
                        )
                        
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Remove Image",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Image"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Car Image")
                    }
                }
                
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
                            formError = "All fields except image are required"
                            return@Button
                        }
                        
                        if (selectedImageUri == null) {
                            formError = "Please upload a car image"
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
                        
                        if (isEditing) {
                            // Get the car from state to update it
                            if (carState is CarState.Success) {
                                val car = (carState as CarState.Success).car
                                                val updatedCar = car.copy(
                                    brand = brand,
                                    model = model,
                                    year = yearInt,
                                    description = description,
                                    pricePerDay = price,
                                    location = location,
                                    imageUri = selectedImageUri?.toString() ?: car.imageUri
                                )
                                carViewModel.updateCar(updatedCar)
                            }
                        } else {
                            // Add a new car
                            carViewModel.addCar(
                                ownerId = currentUser.userId,
                                brand = brand,
                                model = model,
                                year = yearInt,
                                description = description,
                                pricePerDay = price,
                                location = location,
                                imageUri = selectedImageUri
                            )
                        }
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
                        Text(if (isEditing) "Update Car" else "Add Car")
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
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Only car owners can add cars",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
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