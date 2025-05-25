package com.example.myapplication.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.data.model.User
import com.example.myapplication.data.model.UserType
import com.example.myapplication.di.AppModule
import com.example.myapplication.ui.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    currentUser: User,
    viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(
            AppModule.provideUserRepository(androidx.compose.ui.platform.LocalContext.current),
            androidx.compose.ui.platform.LocalContext.current
        )
    ),
    authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(AppModule.provideUserRepository(androidx.compose.ui.platform.LocalContext.current))
    )
) {
    val profileState by viewModel.profileState.collectAsState()
    val editProfileState by viewModel.editProfileState.collectAsState()
    
    // Dialog state
    var showEditDialog by remember { mutableStateOf(false) }
    
    // Edit form state
    var editFullName by remember { mutableStateOf("") }
    var editPhoneNumber by remember { mutableStateOf("") }
    var editAddress by remember { mutableStateOf("") }
    var editError by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(currentUser.userId) {
        viewModel.loadUserProfile(currentUser.userId)
    }
    
    // Reset dialog when edit is successful
    LaunchedEffect(editProfileState) {
        if (editProfileState is EditProfileState.Success) {
            showEditDialog = false
            viewModel.resetEditProfileState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
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
                .padding(padding)
        ) {
            when (val state = profileState) {
                is ProfileState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                
                is ProfileState.Success -> {
                    val user = state.user
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile image with upload capability
                        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
                        val imagePicker = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.GetContent()
                        ) { uri: Uri? ->
                            selectedImageUri = uri
                            if (uri != null) {
                                // Update profile with new image
                                viewModel.updateUserProfile(
                                    user = user,
                                    newFullName = user.fullName,
                                    newPhoneNumber = user.phoneNumber,
                                    newAddress = user.address,
                                    imageUri = uri
                                )
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable { imagePicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (user.profileImageUri != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(user.profileImageUri.toUri()),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.size(100.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // Camera icon overlay
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(36.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Change Profile Picture",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(20.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = user.fullName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Surface(
                            onClick = { },
                            modifier = Modifier.padding(vertical = 8.dp),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = if (user.userType == UserType.OWNER) "Car Owner" else "Renter",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        ProfileDetailItem(
                            icon = Icons.Default.Email,
                            label = "Email",
                            value = user.email
                        )
                        
                        ProfileDetailItem(
                            icon = Icons.Default.Phone,
                            label = "Phone",
                            value = user.phoneNumber
                        )
                        
                        ProfileDetailItem(
                            icon = Icons.Default.Home,
                            label = "Address",
                            value = user.address
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        OutlinedButton(
                            onClick = { 
                                // Initialize edit form with current values
                                editFullName = user.fullName
                                editPhoneNumber = user.phoneNumber
                                editAddress = user.address
                                editError = null
                                showEditDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Profile")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (user.userType == UserType.OWNER) {
                            OutlinedButton(
                                onClick = { navController.navigate("reservation_history") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reservation History")
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        Button(
                            onClick = { 
                                authViewModel.logout()
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Logout")
                        }
                    }
                    
                    // Edit Profile Dialog
                    if (showEditDialog) {
                        var selectedEditImageUri by remember { mutableStateOf<Uri?>(null) }
                        val editImagePicker = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.GetContent()
                        ) { uri: Uri? ->
                            selectedEditImageUri = uri
                        }
                        
                        AlertDialog(
                            onDismissRequest = { showEditDialog = false },
                            title = { Text("Edit Profile") },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Profile image selection
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            .clickable { editImagePicker.launch("image/*") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selectedEditImageUri != null) {
                                            Image(
                                                painter = rememberAsyncImagePainter(selectedEditImageUri),
                                                contentDescription = "Profile Picture",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else if (user.profileImageUri != null) {
                                            Image(
                                                painter = rememberAsyncImagePainter(user.profileImageUri.toUri()),
                                                contentDescription = "Profile Picture",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = "Profile Picture",
                                                modifier = Modifier.size(80.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        
                                        // Camera icon overlay
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(30.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = "Change Profile Picture",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier
                                                    .padding(6.dp)
                                                    .size(18.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    OutlinedTextField(
                                        value = editFullName,
                                        onValueChange = { editFullName = it },
                                        label = { Text("Full Name") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        enabled = editProfileState != EditProfileState.Loading
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    OutlinedTextField(
                                        value = editPhoneNumber,
                                        onValueChange = { editPhoneNumber = it },
                                        label = { Text("Phone Number") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        enabled = editProfileState != EditProfileState.Loading
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    OutlinedTextField(
                                        value = editAddress,
                                        onValueChange = { editAddress = it },
                                        label = { Text("Address") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        enabled = editProfileState != EditProfileState.Loading
                                    )
                                    
                                    if (editError != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = editError!!,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    
                                    if (editProfileState is EditProfileState.Error) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = (editProfileState as EditProfileState.Error).message,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        editError = null
                                        
                                        if (editFullName.isBlank() || editPhoneNumber.isBlank() || editAddress.isBlank()) {
                                            editError = "All fields are required"
                                            return@Button
                                        }
                                        
                                        viewModel.updateUserProfile(
                                            user = user,
                                            newFullName = editFullName,
                                            newPhoneNumber = editPhoneNumber,
                                            newAddress = editAddress,
                                            imageUri = selectedEditImageUri
                                        )
                                    },
                                    enabled = editProfileState != EditProfileState.Loading
                                ) {
                                    if (editProfileState is EditProfileState.Loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Text("Save")
                                    }
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showEditDialog = false },
                                    enabled = editProfileState != EditProfileState.Loading
                                ) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
                
                is ProfileState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { viewModel.loadUserProfile(currentUser.userId) }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
} 