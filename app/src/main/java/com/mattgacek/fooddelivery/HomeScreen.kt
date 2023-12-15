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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

// Inside RestaurantDetailScreen, within the "Place Order" button onClick
                Button(onClick = {
                    val orderDetails = hashMapOf(
                        "restaurantName" to restaurantName,
                        "items" to restaurant?.menu?.map { menuItem ->
                            hashMapOf(
                                "name" to (menuItem["name"] as String),
                                "price" to (menuItem["price"] as String),
                                "quantity" to (menuItemQuantities[menuItem["name"] as String] ?: 0)
                            )
                        },
                        "deliveryAddress" to deliveryAddress,
                        "orderTime" to System.currentTimeMillis(), // Save the current timestamp
                        "userId" to FirebaseAuth.getInstance().currentUser?.uid // Optionally, save the user ID
                    )

                    FirebaseFirestore.getInstance().collection("placed")
                        .add(orderDetails)
                        .addOnSuccessListener {
                            navController.navigate("orderDetails")
                        }
                        .addOnFailureListener {
                            // Handle failure
                        }
                }, modifier = Modifier.padding(16.dp)) {
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

@Composable
fun OrderDetailsScreen(navController: NavController) {
    var order by remember { mutableStateOf<Map<String, Any>?>(null) }

    LaunchedEffect(Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid // Fetch the current user's ID
        FirebaseFirestore.getInstance().collection("placed")
            .whereEqualTo("userId", userId) // Filter by user ID
            .orderBy("orderTime", Query.Direction.DESCENDING) // Order by timestamp
            .limit(1) // Limit to the most recent order
            .get()
            .addOnSuccessListener { querySnapshot ->
                val documents = querySnapshot.documents
                if (documents.isNotEmpty()) {
                    order = documents.first().data
                }
            }
            .addOnFailureListener {
                // Handle failure
            }
    }

    order?.let {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Order Details", style = MaterialTheme.typography.h4)
            Spacer(modifier = Modifier.height(16.dp))

            // Display order details
            (it["items"] as? List<Map<String, Any>>)?.forEach { item ->
                OrderItemCard(item)
            }

            // Additional details like address, order time, etc.
            Text("From: ${it["restaurantName"]}")
            Text("Address: ${it["deliveryAddress"]}")
            // Convert the timestamp to readable date/time
            Text("Date/Time: ${formatTimestamp(it["orderTime"] as Long)}")
        }
    } ?: Text("Loading order details...", modifier = Modifier.fillMaxWidth().padding(16.dp))
}

@Composable
fun OrderItemCard(item: Map<String, Any>) {
    Card(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Item: ${item["name"]}", style = MaterialTheme.typography.body1)
            Text("Price: ${item["price"]}", style = MaterialTheme.typography.body1)
            Text("Quantity: ${item["quantity"]}", style = MaterialTheme.typography.body1)
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
    val date = java.util.Date(timestamp)
    return sdf.format(date)
}








