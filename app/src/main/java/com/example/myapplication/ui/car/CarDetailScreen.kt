package com.example.myapplication.ui.car

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.myapplication.data.model.Car
import com.example.myapplication.data.model.CarImage
import com.example.myapplication.data.model.User
import com.example.myapplication.data.model.UserType
import com.example.myapplication.di.AppModule
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.rating.RatingViewModel
import com.example.myapplication.ui.rating.RatingsState
import com.example.myapplication.ui.reservation.CreateReservationState
import com.example.myapplication.ui.reservation.ReservationViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Remove

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarDetailScreen(
    navController: NavController,
    carId: String,
    currentUser: User,
    carViewModel: CarViewModel = viewModel(
        factory = CarViewModel.Factory(AppModule.provideCarRepository(androidx.compose.ui.platform.LocalContext.current))
    ),
    reservationViewModel: ReservationViewModel = viewModel(
        factory = ReservationViewModel.Factory(
            AppModule.provideReservationRepository(androidx.compose.ui.platform.LocalContext.current),
            AppModule.provideCarRepository(androidx.compose.ui.platform.LocalContext.current)
        )
    ),
    ratingViewModel: RatingViewModel = viewModel(
        factory = RatingViewModel.Factory(
            AppModule.provideRatingRepository(androidx.compose.ui.platform.LocalContext.current)
        )
    )
) {
    val carDetailState by carViewModel.carDetailState.collectAsState()
    val createReservationState by reservationViewModel.createReservationState.collectAsState()
    val ratingsState by ratingViewModel.carRatingsState.collectAsState()

    LaunchedEffect(carDetailState) {
        when (carDetailState) {
            is CarDetailState.Error -> {
                println("CarDetailScreen ERROR: ${(carDetailState as CarDetailState.Error).message}")
            }
            is CarDetailState.Loading -> {
                println("CarDetailScreen: Loading car details")
            }
            is CarDetailState.Success -> {
                println("CarDetailScreen: Successfully loaded car details")
            }
        }
    }

    var showReservationDialog by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }

    LaunchedEffect(carId) {
        carViewModel.loadCarDetail(carId)
        ratingViewModel.loadCarRatings(carId)
    }

    LaunchedEffect(Unit) {
        reservationViewModel.resetCreateReservationState()
    }

    LaunchedEffect(createReservationState) {
        if (createReservationState is CreateReservationState.Success) {
            showReservationDialog = false
            navController.navigate("my_reservations")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Car Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = carDetailState) {
                is CarDetailState.Loading -> LoadingState()
                
                is CarDetailState.Success -> {
                    val car = state.car
                    val images = state.images
                    
                    CarDetailsContent(
                        car = car,
                        images = images,
                        currentUser = currentUser,
                        onReserveClick = { showReservationDialog = true },
                        ratingsState = ratingsState
                    )
                }
                
                is CarDetailState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { carViewModel.loadCarDetail(carId) }
                )
            }
        }
    }

    if (showReservationDialog && carDetailState is CarDetailState.Success) {
        val car = (carDetailState as CarDetailState.Success).car
        
        AlertDialog(
            onDismissRequest = { showReservationDialog = false },
            title = { Text("Reserve ${car.brand} ${car.model}") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Start Date:", style = MaterialTheme.typography.bodyLarge)
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                startDate = startDate.minusDays(1) 
                                if (startDate.isBefore(LocalDate.now())) {
                                    startDate = LocalDate.now()
                                }
                                if (endDate.isBefore(startDate) || endDate == startDate) {
                                    endDate = startDate.plusDays(1)
                                }
                            }
                        ) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Day")
                        }
                        
                        Text(
                            text = startDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                        
                        IconButton(
                            onClick = { 
                                startDate = startDate.plusDays(1) 
                                if (endDate.isBefore(startDate) || endDate == startDate) {
                                    endDate = startDate.plusDays(1)
                                }
                            }
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Day")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("End Date:", style = MaterialTheme.typography.bodyLarge)
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                endDate = endDate.minusDays(1) 
                                if (endDate.isBefore(startDate) || endDate == startDate) {
                                    endDate = startDate.plusDays(1)
                                }
                            }
                        ) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Day")
                        }
                        
                        Text(
                            text = endDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                        
                        IconButton(
                            onClick = { endDate = endDate.plusDays(1) }
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Day")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val daysCount = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate)
                    val totalPrice = daysCount * car.pricePerDay
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Duration:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "$daysCount days",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Price:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "$${String.format("%.2f", totalPrice)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    if (createReservationState is CreateReservationState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (createReservationState as CreateReservationState.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        reservationViewModel.createReservation(
                            carId = carId,
                            renterId = currentUser.userId,
                            startDate = startDate,
                            endDate = endDate
                        )
                    },
                    enabled = createReservationState !is CreateReservationState.Loading
                ) {
                    if (createReservationState is CreateReservationState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Reserve")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showReservationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CarDetailsContent(
    car: Car,
    images: List<CarImage>,
    currentUser: User,
    onReserveClick: () -> Unit,
    ratingsState: RatingsState
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            val imageUrl = if (images.isNotEmpty()) {
                val primaryImage = images.find { it.isPrimary }
                primaryImage?.imageUrl ?: images.first().imageUrl
            } else {
                "https://via.placeholder.com/800x600?text=No+Image"
            }
            
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(android.R.drawable.ic_menu_gallery)
                    .crossfade(true)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .fallback(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .build(),
                contentDescription = "${car.brand} ${car.model}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${car.brand} ${car.model}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
            
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "$${car.pricePerDay}/day",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${car.brand} ${car.model}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                if (car.rating > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", car.rating),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = "Location",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = car.location,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = "Year",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${car.year}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Description",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = car.description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (currentUser.userType == UserType.RENTER && currentUser.userId != car.ownerId) {
                Button(
                    onClick = onReserveClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reserve Now")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Ratings & Reviews",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            when (val state = ratingsState) {
                is RatingsState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                is RatingsState.Empty -> {
                    Text(
                        text = "No reviews yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                is RatingsState.Success -> {
                    Column {
                        state.ratings.forEach { rating ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Anonymous User",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        Row {
                                            repeat(5) { index ->
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = if (index < rating.score) MaterialTheme.colorScheme.primary 
                                                           else MaterialTheme.colorScheme.surfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    if (rating.comment != null) {
                                        Text(
                                            text = rating.comment,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text(
                                        text = rating.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                is RatingsState.Error -> {
                    Text(
                        text = "Failed to load reviews: ${state.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
} 