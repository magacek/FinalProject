package com.mattgacek.fooddelivery

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects

data class Restaurant(
    val name: String = "",
    val imageUrls: List<String> = emptyList(),
    val menu: List<Map<String, Any>> = emptyList()
)

@Composable
fun HomeScreen(navController: NavController) {
    var restaurants by remember { mutableStateOf<List<Restaurant>>(listOf()) }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("restaurants")
            .get()
            .addOnSuccessListener { snapshot ->
                restaurants = snapshot.toObjects()
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        restaurants.forEach { restaurant ->
            RestaurantCard(restaurant, navController)
        }
    }
}

@Composable
fun RestaurantCard(restaurant: Restaurant, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                // Implement navigation to the restaurant-specific screen here
                // For example: navController.navigate("restaurantDetails/${restaurant.name}")
            }
    ) {
        Column {
            Button(
                onClick = { /* Implement navigation action if needed */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(restaurant.name, style = MaterialTheme.typography.h6)
            }
            // Optionally add more details about the restaurant below the button
        }
    }
}
