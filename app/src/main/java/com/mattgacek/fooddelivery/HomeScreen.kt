package com.mattgacek.fooddelivery

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*


import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects

data class Restaurant(
    val name: String = "",
    val imageUrls: List<String> = emptyList(), // Make sure this matches the Firestore field name
    val menu: List<Map<String, Any>> = emptyList() // Make sure this matches the Firestore field name
)


@Composable
fun HomeScreen(navController: NavController) {
    var restaurants by remember { mutableStateOf<List<Restaurant>>(listOf()) }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("restaurants")
            .get()
            .addOnSuccessListener { snapshot ->
                restaurants = snapshot.toObjects()
                Log.d("HomeScreen", "Fetched restaurants: $restaurants")
            }
            .addOnFailureListener { e ->
                Log.e("HomeScreen", "Error fetching restaurants", e)
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
                navController.navigate("restaurantDetail/${restaurant.name}")
            }
    ) {
        Column {
            Button(
                onClick = { navController.navigate("restaurantDetail/${restaurant.name}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(restaurant.name, style = MaterialTheme.typography.h6)
            }
        }
    }
}

@Composable
fun RestaurantDetailScreen(restaurantName: String?, navController: NavController) {
    var restaurant by remember { mutableStateOf<Restaurant?>(null) }

    LaunchedEffect(restaurantName) {
        FirebaseFirestore.getInstance().collection("restaurants")
            .whereEqualTo("name", restaurantName)
            .get()
            .addOnSuccessListener { snapshot ->
                restaurant = snapshot.toObjects<Restaurant>().firstOrNull()
            }
    }

    restaurant?.let { res ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, // Align children to the center horizontally
            modifier = Modifier.fillMaxSize()
        ) {
            res.imageUrls.firstOrNull()?.let { imageUrl ->
                Image(
                    painter = rememberImagePainter(imageUrl),
                    contentDescription = "Restaurant Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(res.name, style = MaterialTheme.typography.h6, modifier = Modifier.padding(8.dp))
            res.menu.forEach { menuItem ->
                MenuItemCard(menuItem)
            }
            Button(
                onClick = { /* Implement Checkout Action */ },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Checkout")
            }
        }
    } ?: Text("Loading or no data available", modifier = Modifier.fillMaxWidth().padding(16.dp))
}


@Composable
fun MenuItemCard(menuItem: Map<String, Any>) {
    var quantity by remember { mutableStateOf(0) }

    Card(modifier = Modifier.padding(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(menuItem["name"] as String, style = MaterialTheme.typography.body1)
            Text(menuItem["price"] as String, style = MaterialTheme.typography.body1)
            Row {
                IconButton(onClick = { if (quantity > 0) quantity-- }) {
                    Icon(Icons.Default.Delete, contentDescription = "Decrease")
                }
                Text("$quantity", style = MaterialTheme.typography.body1)
                IconButton(onClick = { quantity++ }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase")
                }
            }
        }
    }
}

