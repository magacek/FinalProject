package com.mattgacek.fooddelivery

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * RecentOrdersScreen.kt
 * This file contains the RecentOrdersScreen Composable function, showcasing a list of recent orders
 * made by the user, fetched from Firebase Firestore.
 */

@Composable
fun RecentOrdersScreen(navController: NavController) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var orders by remember { mutableStateOf<List<Map<String, Any>>>(listOf()) }

    LaunchedEffect(userId) {
        FirebaseFirestore.getInstance().collection("orders") // Changed from "placed" to "orders"
            .whereEqualTo("userId", userId)
            .orderBy("orderTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                orders = snapshot.documents.mapNotNull { it.data }
            }
            .addOnFailureListener {
                // Handle failure
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        orders.forEach { order ->
            OrderCard(order = order, navController, userId)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun OrderCard(order: Map<String, Any>, navController: NavController, userId: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Existing code for displaying order details
            Text("Order from: ${order["restaurantName"]}", style = MaterialTheme.typography.h6)

            // Displaying the items ordered
            (order["items"] as? List<Map<String, Any>>)?.let { items ->
                items.forEach { item ->
                    Text("${item["name"]} - ${item["quantity"]} x ${item["price"]}")
                }
            }

            // Displaying order time
            Text("Ordered on: ${formatTimestamp(order["orderTime"] as Long)}", style = MaterialTheme.typography.body2)

            // Displaying delivery address
            Text("Delivery address: ${order["deliveryAddress"]}", style = MaterialTheme.typography.body2)

            // Optionally, display any special instructions
            order["specialInstructions"]?.let {
                Text("Special instructions: $it", style = MaterialTheme.typography.body2)
            }
            Button(onClick = { placeOrderAgain(order, userId, navController) }) {
                Text("Place this order again")
            }
        }
    }
}

fun placeOrderAgain(order: Map<String, Any>, userId: String?, navController: NavController) {
    // Extract restaurant address from the original order
    val restaurantAddress = order["restaurantAddress"] as? String ?: return

    val newOrder = order.toMutableMap().apply {
        // Update the order time
        put("orderTime", System.currentTimeMillis())
        // Add the user ID
        if (userId != null) {
            put("userId", userId)
        }
    }

    FirebaseFirestore.getInstance().collection("orders")
        .add(newOrder)
        .addOnSuccessListener { documentReference ->
            // Navigate to OrderDetailsScreen with the new order ID
            navController.navigate("orderDetails/${documentReference.id}")
        }
        .addOnFailureListener {
            // Handle failure (e.g., show an error message)
        }
}



