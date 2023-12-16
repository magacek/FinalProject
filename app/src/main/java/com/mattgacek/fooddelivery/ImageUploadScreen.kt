package com.mattgacek.fooddelivery

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import java.util.*

/**
 * ImageUploadScreen.kt
 * Manages the image uploading process for user profiles. It includes functionality for selecting
 * and uploading images to Firebase Storage.
 */

@Composable
fun ImageUploadScreen(navController: NavController) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri = result.data?.data
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Button(onClick = {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            launcher.launch(intent)
        }) {
            Text("Pick an Image")
        }

        Spacer(modifier = Modifier.height(16.dp))

        imageUri?.let {
            Image(
                painter = rememberImagePainter(it),
                contentDescription = "Selected Image",
                modifier = Modifier.size(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            imageUri?.let { uri ->
                val storageRef = FirebaseStorage.getInstance().reference
                val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                val imageRef = storageRef.child("images/$userId/profile_picture")

                val uploadTask = imageRef.putFile(uri)
                uploadTask.addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val userProfileChangeRequest = UserProfileChangeRequest.Builder()
                            .setPhotoUri(downloadUri)
                            .build()

                        FirebaseAuth.getInstance().currentUser?.updateProfile(userProfileChangeRequest)?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
                                navController.navigate("home") // Navigate to Home Screen
                            } else {
                                Toast.makeText(context, "Profile Update Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }.addOnFailureListener {
                    Toast.makeText(context, "Upload Failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }) {
            Text("Upload Image and Continue")
        }
    }
}
