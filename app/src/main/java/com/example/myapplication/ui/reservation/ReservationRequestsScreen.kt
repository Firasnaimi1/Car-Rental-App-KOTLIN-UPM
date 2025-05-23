package com.example.myapplication.ui.reservation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DirectionsCar
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
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationRequestsScreen(
    navController: NavController,
    currentUser: User,
    reservationViewModel: ReservationViewModel = viewModel(
        factory = ReservationViewModel.Factory(
            AppModule.provideReservationRepository(androidx.compose.ui.platform.LocalContext.current),
            AppModule.provideCarRepository(androidx.compose.ui.platform.LocalContext.current)
        )
    )
) {
    val ownerReservationsState by reservationViewModel.ownerReservationsState.collectAsState()

    // Load owner's pending reservation requests
    LaunchedEffect(currentUser.userId) {
        reservationViewModel.loadOwnerPendingReservations(currentUser.userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reservation Requests") },
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
                    selected = false,
                    onClick = { navController.navigate("my_cars") },
                    icon = { Icon(Icons.Outlined.DirectionsCar, contentDescription = "My Cars") },
                    label = { Text("My Cars") }
                )
                
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already on Requests screen */ },
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
            when (val state = ownerReservationsState) {
                is ReservationsState.Loading -> LoadingState()
                
                is ReservationsState.Empty -> EmptyState(
                    message = "You don't have any pending reservation requests."
                )
                
                is ReservationsState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(state.reservations) { reservation ->
                            ReservationRequestItem(
                                reservation = reservation,
                                onApprove = { 
                                    reservationViewModel.approveReservation(
                                        reservation.reservationId, 
                                        currentUser.userId
                                    ) 
                                },
                                onReject = { 
                                    reservationViewModel.rejectReservation(
                                        reservation.reservationId, 
                                        currentUser.userId
                                    ) 
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
                
                is ReservationsState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { reservationViewModel.loadOwnerPendingReservations(currentUser.userId) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReservationRequestItem(
    reservation: Reservation,
    onApprove: () -> Unit,
    onReject: () -> Unit
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
            // Car info would be shown here in a real app
            Text(
                text = "Reservation Request",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reject")
                }
                
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Approve")
                }
            }
        }
    }
} 