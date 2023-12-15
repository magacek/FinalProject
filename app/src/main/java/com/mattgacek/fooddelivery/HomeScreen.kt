package com.mattgacek.fooddelivery

import android.content.Context
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.google.android.gms.maps.model.LatLngBounds

import java.util.*

import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import java.util.Random

data class Restaurant(
    val name: String = "",
    val imageUrls: List<String> = emptyList(),
    val menu: List<Map<String, Any>> = emptyList(),
    val address: String = "" // Add this line
)



@Composable
fun HomeScreen(navController: NavController) {
    var restaurants by remember { mutableStateOf<List<Restaurant>>(listOf()) }
    var favoriteRestaurants by remember { mutableStateOf<List<String>>(listOf()) }

    // Fetch all restaurants
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance().collection("restaurants")
            .get()
            .addOnSuccessListener { snapshot ->
                restaurants = snapshot.toObjects()
            }
            .addOnFailureListener { e ->
                Log.e("HomeScreen", "Error fetching restaurants", e)
            }
    }

    // Fetch user's favorite restaurants from their order history
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    LaunchedEffect(userId) {
        FirebaseFirestore.getInstance().collection("orders")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val orderedRestaurants = snapshot.documents.mapNotNull { it["restaurantName"] as? String }
                favoriteRestaurants = orderedRestaurants.toSet().toList() // Convert to set to remove duplicates, then back to list
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Display favorite restaurants if available
        if (favoriteRestaurants.isNotEmpty()) {
            Text("Favorites", style = MaterialTheme.typography.h6, modifier = Modifier.padding(8.dp))
            HorizontalScrollableFavorites(favoriteRestaurants, navController)
        }

        // Display all restaurants
        Text("All Restaurants", style = MaterialTheme.typography.h6, modifier = Modifier.padding(8.dp))
        restaurants.forEach { restaurant ->
            RestaurantCard(restaurant, navController)
        }
    }
}

@Composable
fun HorizontalScrollableFavorites(favoriteRestaurants: List<String>, navController: NavController) {
    LazyRow(modifier = Modifier.padding(horizontal = 8.dp)) {
        items(favoriteRestaurants) { restaurantName ->
            FavoriteRestaurantCard(restaurantName, navController)
        }
    }
}

@Composable
fun FavoriteRestaurantCard(restaurantName: String, navController: NavController) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .width(150.dp) // Set a fixed width for the card
            .height(100.dp) // Set a fixed height for the card
            .clickable { navController.navigate("restaurantDetail/$restaurantName") },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = restaurantName,
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center
            )
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
                    menuItemQuantities[menuItem["name"] as String] = 0
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
                        "restaurantAddress" to restaurant?.address,
                        "restaurantName" to restaurantName,
                        "items" to restaurant?.menu?.map { menuItem ->
                            hashMapOf(
                                "name" to menuItem["name"] as String,
                                "price" to menuItem["price"] as String,

                                "quantity" to (menuItemQuantities[menuItem["name"] as String] ?: 0)
                            )
                        },
                        "deliveryAddress" to deliveryAddress,
                        "orderTime" to System.currentTimeMillis(),
                        "userId" to FirebaseAuth.getInstance().currentUser?.uid
                    )

                    FirebaseFirestore.getInstance().collection("orders")
                        .add(orderDetails)
                        .addOnSuccessListener {
                            navController.navigate("orderTracking/${restaurant?.address}/$deliveryAddress")
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
fun OrderTrackingScreen(navController: NavController, deliveryAddress: String, restaurantAddress: String) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    val restaurantLatLng = getAddressLatLng(context, restaurantAddress)
    val deliveryLatLng = getAddressLatLng(context, deliveryAddress)


    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            googleMap = map
            val boundsBuilder = LatLngBounds.builder()

            // Add markers and draw a line between them
            restaurantLatLng?.let {
                map.addMarker(MarkerOptions().position(it).title("Restaurant"))
                    ?.also { marker -> boundsBuilder.include(marker.position) }
            }

            deliveryLatLng?.let {
                map.addMarker(MarkerOptions().position(it).title("Delivery Address"))
                    ?.also { marker -> boundsBuilder.include(marker.position) }
            }

            if (restaurantLatLng != null && deliveryLatLng != null) {
                map.addPolyline(PolylineOptions().add(restaurantLatLng, deliveryLatLng).width(5f).color(Color.RED))
            }

            val bounds = boundsBuilder.build()
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        }
    }

    val estimatedTime = if (restaurantLatLng != null && deliveryLatLng != null) {
        calculateEstimatedDeliveryTime(restaurantLatLng, deliveryLatLng)
    } else 0

    // Adjust the layout to show the map and the estimated time
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Estimated Delivery Time: $estimatedTime minutes", modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
        AndroidView({ mapView }, modifier = Modifier.weight(1f).fillMaxWidth())
    }
}




@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply { onCreate(Bundle()) }
    }
    DisposableEffect(mapView) {
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onStop()
            mapView.onPause()
        }
    }
    return mapView
}

fun getAddressLatLng(context: Context, address: String): LatLng? {
    val geocoder = Geocoder(context)
    return geocoder.getFromLocationName(address, 1)?.firstOrNull()?.let {
        LatLng(it.latitude, it.longitude)
    }
}
fun calculateEstimatedDeliveryTime(startLatLng: LatLng, endLatLng: LatLng): Int {
    val distanceInMeters = FloatArray(1)
    Location.distanceBetween(startLatLng.latitude, startLatLng.longitude, endLatLng.latitude, endLatLng.longitude, distanceInMeters)
    val distanceInKm = distanceInMeters[0] / 1000 // Convert meters to kilometers

    val averageSpeedInKmH = 40 // Average speed in km/h
    val estimatedTimeInHours = distanceInKm / averageSpeedInKmH

    return (estimatedTimeInHours * 60).toInt() // Convert hours to minutes and return
}


fun calculateDistance(startLatLng: LatLng, endLatLng: LatLng): Float {
    val results = FloatArray(1)
    Location.distanceBetween(
        startLatLng.latitude, startLatLng.longitude,
        endLatLng.latitude, endLatLng.longitude,
        results
    )
    return results[0] / 1000 // Convert to kilometers
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
fun OrderDetailsScreen(navController: NavController, orderId: String? = null) {
    var order by remember { mutableStateOf<Map<String, Any>?>(null) }

    LaunchedEffect(orderId) {
        if (orderId != null) {
            FirebaseFirestore.getInstance().collection("orders").document(orderId)
                .get()
                .addOnSuccessListener { document ->
                    order = document.data
                }
        } else {
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
        Button(onClick = {
            navController.navigate("orderTracking/${order!!["restaurantAddress"]}/${order!!["deliveryAddress"]}")
        }) {
            Text("Track Order")
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


@Composable
fun CalendarScreen(navController: NavController) {
    val currentMonth = YearMonth.now()
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var spendingForSelectedDate by remember { mutableStateOf<Double?>(null) }

    Column {
        Text("${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}", style = MaterialTheme.typography.h6)
        DaysOfWeekHeaders(cellSize = 54.dp)
        CalendarDaysGrid(
            currentMonth = currentMonth,
            daysInMonth = daysInMonth,
            firstDayOfWeek = firstDayOfWeek,
            selectedDate = selectedDate,
            cellSize = 48.dp, // Updated size
            onDateSelected = { date ->
                selectedDate = date
                fetchDailySpending(date) { spending ->
                    spendingForSelectedDate = spending
                }
            }
        )


        selectedDate?.let { date ->
            Text("Selected date: ${date.toString()}", style = MaterialTheme.typography.body1)
            spendingForSelectedDate?.let { spending ->
                Text("Spending: $${spending}", style = MaterialTheme.typography.body1)
            }
        }
    }
}

@Composable
fun DaysOfWeekHeaders(cellSize: Dp) {
    Row(modifier = Modifier.fillMaxWidth()) {
        DayOfWeek.values().forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier
                    .width(cellSize) // Set each header's width equal to cell size
                    .padding(vertical = 5.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CalendarDaysGrid(
    currentMonth: YearMonth,
    daysInMonth: Int,
    firstDayOfWeek: Int,
    selectedDate: LocalDate?,
    cellSize: Dp,
    onDateSelected: (LocalDate) -> Unit
) {
    val weeksInMonth = (daysInMonth + firstDayOfWeek - 2) / 7 + 1

    Column {
        for (week in 0 until weeksInMonth) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dayOfWeek in 1..7) {
                    val dayOfMonth = week * 7 + dayOfWeek - firstDayOfWeek + 2
                    if (dayOfMonth in 1..daysInMonth) {
                        DayCell(
                            date = currentMonth.atDay(dayOfMonth),
                            isSelected = selectedDate == currentMonth.atDay(dayOfMonth),
                            cellSize = cellSize,
                            onDayClicked = { onDateSelected(currentMonth.atDay(dayOfMonth)) }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(cellSize)) // Spacer for alignment
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(date: LocalDate, isSelected: Boolean, cellSize: Dp, onDayClicked: () -> Unit) {
    Button(
        onClick = onDayClicked,
        modifier = Modifier
            .padding(4.dp)
            .size(cellSize), // Use the new cell size
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface
        )
    ) {
        Text(text = "${date.dayOfMonth}", textAlign = TextAlign.Center)
    }
}


fun fetchDailySpending(date: LocalDate, onResult: (Double) -> Unit) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    FirebaseFirestore.getInstance().collection("orders")
        .whereEqualTo("userId", userId)
        .whereGreaterThanOrEqualTo("orderTime", startOfDay)
        .whereLessThan("orderTime", endOfDay)
        .get()
        .addOnSuccessListener { querySnapshot ->
            val totalSpending = querySnapshot.documents.sumOf { document ->
                val items = document.get("items") as? List<Map<String, Any>> ?: listOf()
                items.sumOf { item ->
                    val priceString = item["price"] as? String ?: return@sumOf 0.0
                    val quantity = (item["quantity"] as? Number)?.toInt() ?: 1
                    val price = priceString.drop(1).toDoubleOrNull() ?: 0.0
                    price * quantity
                }
            }
            onResult(totalSpending)
        }
}
