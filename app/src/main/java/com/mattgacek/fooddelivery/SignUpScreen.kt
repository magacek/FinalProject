package com.mattgacek.fooddelivery

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
/**
 * SignUpScreen.kt
 * Manages the user registration process, including collecting user details and registering the user
 * with Firebase Authentication and Firebase Realtime Database.
 */

@Composable
fun SignUpScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val auth = FirebaseAuth.getInstance()
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Update the user's profile
                    val userProfileChangeRequest = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    task.result?.user?.updateProfile(userProfileChangeRequest)?.addOnCompleteListener { profileUpdateTask ->
                        if (profileUpdateTask.isSuccessful) {
                            // Proceed with navigation and database updates
                            val userId = auth.currentUser?.uid.orEmpty()
                            val userMap = mapOf("name" to name, "email" to email)
                            FirebaseDatabase.getInstance().getReference("users")
                                .child(userId)
                                .setValue(userMap)
                                .addOnCompleteListener { dbTask ->
                                    if (dbTask.isSuccessful) {
                                        navController.navigate("imageUpload")
                                    } else {
                                        errorMessage = "Failed to save user info: ${dbTask.exception?.message}"
                                    }
                                }
                        } else {
                            errorMessage = "Failed to update profile: ${profileUpdateTask.exception?.message}"
                        }
                    }
                } else {
                    errorMessage = "Sign Up Failed: ${task.exception?.message}"
                }
            }
        }) {
            Text("Create Account")
        }

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colors.error)
        }
    }
}

