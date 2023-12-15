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
    var isInCheckoutMode by remember { mutableStateOf(false) }
    var deliveryAddress by remember { mutableStateOf("") }
    var specialInstructions by remember { mutableStateOf("") }
    val menuItemQuantities = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(restaurantName) {
        FirebaseFirestore.getInstance().collection("restaurants")
            .whereEqualTo("name", restaurantName)
            .get()
            .addOnSuccessListener { snapshot ->
                restaurant = snapshot.toObjects<Restaurant>().firstOrNull()
                restaurant?.menu?.forEach { menuItem ->
                    menuItemQuantities[menuItem["name"] as String] = 1 // Initialize with default quantity
                }
            }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        restaurant?.let { res ->
            if (!isInCheckoutMode) {
                res.imageUrls.firstOrNull()?.let { imageUrl ->
                    Image(
                        painter = rememberImagePainter(imageUrl),
                        contentDescription = "Restaurant Image",
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(res.name, style = MaterialTheme.typography.h6, modifier = Modifier.padding(8.dp))

                res.menu.forEach { menuItem ->
                    val itemName = menuItem["name"] as String
                    MenuItemCard(menuItem, menuItemQuantities[itemName] ?: 1) { newQuantity ->
                        menuItemQuantities[itemName] = newQuantity
                    }
                }
                Button(onClick = { isInCheckoutMode = true }, modifier = Modifier.padding(16.dp)) {
                    Text("Checkout")
                }
            } else {
                Text(res.name, style = MaterialTheme.typography.h4, modifier = Modifier.padding(8.dp))
                res.menu.forEach { menuItem ->
                    val itemName = menuItem["name"] as String
                    CheckoutMenuItemCard(menuItem, menuItemQuantities[itemName] ?: 1)
                }

                TextField(
                    value = deliveryAddress,
                    onValueChange = { deliveryAddress = it },
                    label = { Text("Delivery Address") },
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )
                TextField(
                    value = specialInstructions,
                    onValueChange = { specialInstructions = it },
                    label = { Text("Special Instructions") },
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )

                Button(onClick = { /* Implement order processing logic */ }, modifier = Modifier.padding(16.dp)) {
                    Text("Place Order")
                }
                Button(onClick = { isInCheckoutMode = false }, modifier = Modifier.padding(16.dp)) {
                    Text("Modify Order")
                }
            }
        }
    } ?: Text("Loading or no data available", modifier = Modifier.fillMaxWidth().padding(16.dp))
}

@Composable
fun MenuItemCard(menuItem: Map<String, Any>, quantity: Int, onQuantityChange: (Int) -> Unit) {
    Card(modifier = Modifier.padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(menuItem["name"] as String, style = MaterialTheme.typography.body1)
            Text(menuItem["price"] as String, style = MaterialTheme.typography.body1)
            Row {
                IconButton(onClick = { if (quantity > 0) onQuantityChange(quantity - 1) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Decrease")
                }
                Text("$quantity", style = MaterialTheme.typography.body1)
                IconButton(onClick = { onQuantityChange(quantity + 1) }) {
                    Icon(Icons.Default.Add, contentDescription = "Increase")
                }
            }
        }
    }
}

@Composable
fun CheckoutMenuItemCard(menuItem: Map<String, Any>, quantity: Int) {
    Card(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(menuItem["name"] as String, style = MaterialTheme.typography.body1)
                Text("${menuItem["price"]}", style = MaterialTheme.typography.body1)
            }
            Text("$quantity", style = MaterialTheme.typography.body1)
        }
    }
}









