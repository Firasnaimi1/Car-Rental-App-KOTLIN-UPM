package com.example.myapplication.ui.reservation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.model.Reservation
import com.example.myapplication.data.model.ReservationStatus
import com.example.myapplication.data.model.User
import com.example.myapplication.di.AppModule
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.rating.RatingViewModel
import com.example.myapplication.ui.rating.SubmitRatingState
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReservationsScreen(
    navController: NavController,
    currentUser: User,
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
    val renterReservationsState by reservationViewModel.renterReservationsState.collectAsState()
    val submitRatingState by ratingViewModel.submitRatingState.collectAsState()
    
    // State for rating dialog
    var showRatingDialog by remember { mutableStateOf(false) }
    var selectedReservation by remember { mutableStateOf<Reservation?>(null) }
    var ratingScore by remember { mutableStateOf(5f) }
    var ratingComment by remember { mutableStateOf("") }

    // Load renter's reservations
    LaunchedEffect(currentUser.userId) {
        reservationViewModel.loadRenterReservations(currentUser.userId)
    }

    // Handle successful rating submission
    LaunchedEffect(submitRatingState) {
        if (submitRatingState is SubmitRatingState.Success) {
            showRatingDialog = false
            ratingViewModel.resetSubmitRatingState()
            // Refresh reservations to update status
            reservationViewModel.loadRenterReservations(currentUser.userId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Reservations") },
                actions = {
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile"
                        )
                    }
                }
            )
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
                    onClick = { /* Already on My Reservations screen */ },
                    icon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = "My Reservations") },
                    label = { Text("Reservations") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = renterReservationsState) {
                is ReservationsState.Loading -> LoadingState()
                
                is ReservationsState.Empty -> EmptyState(
                    message = "You don't have any reservations yet.\nBrowse cars to make your first reservation."
                )
                
                is ReservationsState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(state.reservations) { reservation ->
                            ReservationItem(
                                reservation = reservation,
                                onCancel = { 
                                    if (reservation.status == ReservationStatus.PENDING) {
                                        reservationViewModel.cancelReservation(
                                            reservation.reservationId, 
                                            currentUser.userId
                                        )
                                    }
                                },
                                onPay = {
                                    if (reservation.status == ReservationStatus.APPROVED && !reservation.isPaid) {
                                        reservationViewModel.payForReservation(
                                            reservation.reservationId,
                                            currentUser.userId
                                        )
                                    }
                                },
                                onRate = {
                                    if (reservation.status == ReservationStatus.COMPLETED) {
                                        selectedReservation = reservation
                                        showRatingDialog = true
                                        // Check if already rated
                                        ratingViewModel.checkIfUserCanRate(currentUser.userId, reservation.reservationId)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
                
                is ReservationsState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { reservationViewModel.loadRenterReservations(currentUser.userId) }
                )
            }
        }
    }

    // Rating dialog
    if (showRatingDialog && selectedReservation != null) {
        AlertDialog(
            onDismissRequest = { showRatingDialog = false },
            title = { Text("Rate Your Experience") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    when (submitRatingState) {
                        is SubmitRatingState.AlreadyRated -> {
                            Text("You've already rated this reservation.")
                        }
                        
                        is SubmitRatingState.Error -> {
                            ErrorMessage(message = (submitRatingState as SubmitRatingState.Error).message)
                        }
                        
                        else -> {
                            // Rating slider
                            Text("Rating: ${ratingScore.toInt()}/5")
                            Slider(
                                value = ratingScore,
                                onValueChange = { ratingScore = it },
                                valueRange = 1f..5f,
                                steps = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Comment field
                            OutlinedTextField(
                                value = ratingComment,
                                onValueChange = { ratingComment = it },
                                label = { Text("Comment (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (submitRatingState !is SubmitRatingState.AlreadyRated) {
                            ratingViewModel.submitRating(
                                userId = currentUser.userId,
                                carId = selectedReservation!!.carId,
                                reservationId = selectedReservation!!.reservationId,
                                score = ratingScore,
                                comment = if (ratingComment.isBlank()) null else ratingComment
                            )
                        } else {
                            showRatingDialog = false
                        }
                    },
                    enabled = submitRatingState !is SubmitRatingState.Loading
                ) {
                    if (submitRatingState is SubmitRatingState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (submitRatingState is SubmitRatingState.AlreadyRated) "Close" else "Submit")
                    }
                }
            },
            dismissButton = {
                if (submitRatingState !is SubmitRatingState.AlreadyRated) {
                    TextButton(onClick = { showRatingDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReservationItem(
    reservation: Reservation,
    onCancel: () -> Unit,
    onPay: () -> Unit,
    onRate: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Reservation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                val (statusColor, statusText) = when (reservation.status) {
                    ReservationStatus.PENDING -> MaterialTheme.colorScheme.tertiary to "Pending"
                    ReservationStatus.APPROVED -> if (reservation.isPaid) MaterialTheme.colorScheme.primary to "Paid" 
                                                else MaterialTheme.colorScheme.secondary to "Approved"
                    ReservationStatus.COMPLETED -> MaterialTheme.colorScheme.primary to "Completed"
                    ReservationStatus.CANCELLED -> MaterialTheme.colorScheme.error to "Cancelled"
                    ReservationStatus.REJECTED -> MaterialTheme.colorScheme.error to "Rejected"
                }
                
                Surface(
                    color = statusColor,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Car info would be shown here in a real app
            // For now, just showing the car ID
            Text(
                text = "Car ID: ${reservation.carId}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Date info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${reservation.startDate.format(dateFormatter)} - ${reservation.endDate.format(dateFormatter)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Price info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AttachMoney,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Total: $${String.format("%.2f", reservation.totalPrice)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons based on status
            when (reservation.status) {
                ReservationStatus.PENDING -> {
                    // Can cancel pending reservations
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Cancel Reservation")
                    }
                }
                
                ReservationStatus.APPROVED -> {
                    if (!reservation.isPaid) {
                        // Can pay for approved reservations
                        Button(
                            onClick = onPay,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pay Now")
                        }
                    } else {
                        // Already paid, just show info
                        Text(
                            text = "Your reservation is confirmed. Enjoy your trip!",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                ReservationStatus.COMPLETED -> {
                    // Can rate completed reservations
                    Button(
                        onClick = onRate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Rate Your Experience")
                    }
                }
                
                else -> {
                    // No actions for cancelled or rejected reservations
                }
            }
        }
    }
} 