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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCarScreen(
    navController: NavController,
    currentUser: User,
    carId: String,
    carViewModel: CarViewModel = viewModel(
        factory = CarViewModel.Factory(AppModule.provideCarRepository(androidx.compose.ui.platform.LocalContext.current))
    )
) {
    val context = LocalContext.current
    val carRepository = AppModule.provideCarRepository(context)
    val carDetailState by carViewModel.carDetailState.collectAsState()
    val carCreationState by carViewModel.carCreationState.collectAsState()
    
    // Load car details
    LaunchedEffect(carId) {
        carViewModel.loadCarDetail(carId)
    }
    
    // Effect to navigate back on successful car update
    LaunchedEffect(carCreationState) {
        when (carCreationState) {
            is CarCreationState.Success -> {
                navController.navigateUp()
                carViewModel.resetCarCreationState()
            }
            else -> { /* Do nothing for other states */ }
        }
    }
    
    // Form state
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var pricePerDay by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf<String?>(null) }
    
    // Update form when car details are loaded
    LaunchedEffect(carDetailState) {
        if (carDetailState is CarDetailState.Success) {
            val car = (carDetailState as CarDetailState.Success).car
            brand = car.brand
            model = car.model
            year = car.year.toString()
            description = car.description
            pricePerDay = car.pricePerDay.toString()
            location = car.location
            
            // Get the car's primary image
            val primaryImage = carRepository.getCarPrimaryImage(car.carId)
            existingImageUrl = primaryImage?.imageUrl
        }
    }
    
    // Image picker launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    
    // Validation state
    var formError by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Car") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = carDetailState) {
            is CarDetailState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is CarDetailState.Success -> {
                val car = state.car
                
                // Verify car ownership
                if (car.ownerId != currentUser.userId || currentUser.userType != UserType.OWNER) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "You can only edit your own cars",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    return@Scaffold
                }
                
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
                        // Show newly selected image
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
                    } else if (existingImageUrl != null) {
                        // Show existing image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(existingImageUrl),
                                contentDescription = "Current Car Image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(1.dp, MaterialTheme.colorScheme.outline),
                                contentScale = ContentScale.Crop
                            )
                            
                            Button(
                                onClick = { imagePicker.launch("image/*") },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Change Image"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Change Image")
                            }
                        }
                    } else {
                        // No image case
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
                            
                            // Update the car
                            carViewModel.updateCar(
                                carId = car.carId,
                                ownerId = currentUser.userId,
                                brand = brand,
                                model = model,
                                year = yearInt,
                                description = description,
                                pricePerDay = price,
                                location = location,
                                imageUri = selectedImageUri
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
                            Text("Save Changes")
                        }
                    }
                }
            }
            
            is CarDetailState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { carViewModel.loadCarDetail(carId) }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
} 