package com.example.myapplication.ui.reservation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.model.ReservationStatus
import com.example.myapplication.data.model.User
import com.example.myapplication.data.model.UserType
import com.example.myapplication.di.AppModule
import com.example.myapplication.ui.components.ErrorMessage
import com.example.myapplication.ui.components.LoadingSpinner
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationHistoryScreen(
    navController: NavController,
    currentUser: User,
    viewModel: ReservationViewModel = viewModel(
        factory = ReservationViewModel.Factory(
            AppModule.provideReservationRepository(androidx.compose.ui.platform.LocalContext.current),
            AppModule.provideCarRepository(androidx.compose.ui.platform.LocalContext.current)
        )
    )
) {
    val ownerReservationsState by viewModel.ownerReservationsState.collectAsState()
    var selectedStatusFilter by remember { mutableStateOf<ReservationStatus?>(null) }
    
    LaunchedEffect(currentUser.userId) {
        if (currentUser.userType == UserType.OWNER) {
            viewModel.loadAllOwnerReservations(currentUser.userId)
        } else {
            navController.navigateUp() // Navigate back if not an owner
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reservation History") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Filter dropdown
                    var expanded by remember { mutableStateOf(false) }
                    
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Reservations") },
                            onClick = {
                                selectedStatusFilter = null
                                viewModel.loadAllOwnerReservations(currentUser.userId)
                                expanded = false
                            }
                        )
                        ReservationStatus.values().forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.name.capitalize()) },
                                onClick = {
                                    selectedStatusFilter = status
                                    viewModel.loadOwnerReservationsByStatus(currentUser.userId, status)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = ownerReservationsState) {
                is ReservationsState.Loading -> {
                    LoadingSpinner()
                }
                
                is ReservationsState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No reservations found",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                is ReservationsState.Success -> {
                    val reservations = state.reservations
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(reservations) { reservation ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    // Status indicator
                                    Surface(
                                        modifier = Modifier.align(Alignment.End),
                                        shape = MaterialTheme.shapes.small,
                                        color = when(reservation.status) {
                                            ReservationStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                                            ReservationStatus.APPROVED -> MaterialTheme.colorScheme.primary
                                            ReservationStatus.REJECTED -> MaterialTheme.colorScheme.error
                                            ReservationStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                                            ReservationStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
                                        }
                                    ) {
                                        Text(
                                            text = reservation.status.name,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = when(reservation.status) {
                                                ReservationStatus.PENDING -> MaterialTheme.colorScheme.onTertiary
                                                ReservationStatus.APPROVED -> MaterialTheme.colorScheme.onPrimary
                                                ReservationStatus.REJECTED -> MaterialTheme.colorScheme.onError
                                                ReservationStatus.CANCELLED -> MaterialTheme.colorScheme.onErrorContainer
                                                ReservationStatus.COMPLETED -> MaterialTheme.colorScheme.onSecondary
                                            },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Reservation ID
                                    Text(
                                        text = "Reservation #${reservation.reservationId.take(8)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // Car ID (In a real app, we would fetch and display the car details)
                                    Text(
                                        text = "Car ID: ${reservation.carId.take(8)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // Renter ID (In a real app, we would fetch and display the renter's name)
                                    Text(
                                        text = "Renter ID: ${reservation.renterId.take(8)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Dates
                                    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
                                    Text(
                                        text = "From: ${reservation.startDate.format(dateFormatter)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    
                                    Text(
                                        text = "To: ${reservation.endDate.format(dateFormatter)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Price
                                    Text(
                                        text = "Total: $${reservation.totalPrice}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    // Payment status
                                    if (reservation.isPaid) {
                                        Text(
                                            text = "Paid",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = "Not Paid",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    
                                    // Action buttons for pending reservations
                                    if (reservation.status == ReservationStatus.PENDING) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            OutlinedButton(
                                                onClick = { viewModel.rejectReservation(reservation.reservationId, currentUser.userId) },
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.error
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reject")
                                            }
                                            
                                            Button(
                                                onClick = { viewModel.approveReservation(reservation.reservationId, currentUser.userId) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Approve")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                is ReservationsState.Error -> {
                    ErrorMessage(
                        message = state.message,
                        onRetry = { viewModel.loadAllOwnerReservations(currentUser.userId) }
                    )
                }
            }
        }
    }
}

private fun String.capitalize(): String {
    return this.lowercase().replaceFirstChar { it.uppercase() }
}
