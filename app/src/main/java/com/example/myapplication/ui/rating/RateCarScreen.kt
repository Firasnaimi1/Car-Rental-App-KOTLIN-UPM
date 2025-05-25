package com.example.myapplication.ui.rating

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.model.User
import com.example.myapplication.di.AppModule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateCarScreen(
    navController: NavController,
    currentUser: User,
    carId: String,
    reservationId: String,
    viewModel: RatingViewModel = viewModel(
        factory = RatingViewModel.Factory(
            AppModule.provideRatingRepository(androidx.compose.ui.platform.LocalContext.current)
        )
    )
) {
    val submitRatingState by viewModel.submitRatingState.collectAsState()
    var rating by remember { mutableStateOf(5f) }
    var comment by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Check if user can rate
    LaunchedEffect(Unit) {
        viewModel.checkIfUserCanRate(currentUser.userId, reservationId)
    }
    
    // Navigate back after successful submission
    LaunchedEffect(submitRatingState) {
        when (submitRatingState) {
            is SubmitRatingState.Success -> {
                navController.navigateUp()
                viewModel.resetSubmitRatingState()
            }
            is SubmitRatingState.Error -> {
                errorMessage = (submitRatingState as SubmitRatingState.Error).message
            }
            is SubmitRatingState.AlreadyRated -> {
                errorMessage = "You have already rated this reservation"
            }
            else -> { /* Do nothing */ }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rate Your Experience") },
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
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "How was your experience?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Star rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (i in 1..5) {
                    IconButton(
                        onClick = { rating = i.toFloat() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (i <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "Star $i",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
            
            Text(
                text = when {
                    rating >= 5 -> "Excellent!"
                    rating >= 4 -> "Great!"
                    rating >= 3 -> "Good"
                    rating >= 2 -> "Fair"
                    else -> "Poor"
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Comment field
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Your Comments (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 5
            )
            
            // Error message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Submit button
            Button(
                onClick = {
                    errorMessage = null
                    viewModel.submitRating(
                        userId = currentUser.userId,
                        carId = carId,
                        reservationId = reservationId,
                        score = rating,
                        comment = if (comment.isBlank()) null else comment
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = submitRatingState != SubmitRatingState.Loading
            ) {
                if (submitRatingState is SubmitRatingState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Submit Rating")
                }
            }
        }
    }
}
