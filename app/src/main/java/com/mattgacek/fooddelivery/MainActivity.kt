package com.mattgacek.fooddelivery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

/**
 * MainActivity.kt
 * The main activity of the application, which sets up the navigation graph and manages the overall
 * app navigation using Jetpack Compose.
 */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val authState = remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val shouldShowTopBar = authState.value != null && currentRoute !in listOf("login", "signUp", "imageUpload")

    // AuthStateListener
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            authState.value = auth.currentUser
        }
        FirebaseAuth.getInstance().addAuthStateListener(listener)
        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(listener)
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            if (shouldShowTopBar) {
                TopAppBar(
                    title = { Text("") },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch { // Use coroutineScope to call suspend function
                                if (scaffoldState.drawerState.isClosed) {
                                    scaffoldState.drawerState.open()
                                } else {
                                    scaffoldState.drawerState.close()
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        },
        drawerContent = {
            if (authState.value != null) {
                DrawerContent(authState.value, navController, scaffoldState.drawerState)
            }
        }
    ) {
        NavHost(navController = navController, startDestination = "login") {
            composable("login") { LoginScreen(navController) }
            composable("signUp") { SignUpScreen(navController) }
            composable("imageUpload") { ImageUploadScreen(navController) }
            composable("home") {
                HomeScreen(navController) // Pass NavController here
            }
            composable("restaurantDetail/{restaurantName}") { backStackEntry ->
                val restaurantName = backStackEntry.arguments?.getString("restaurantName") ?: ""
                RestaurantDetailScreen(restaurantName, navController)
            }
            composable("orderDetails") {
                OrderDetailsScreen(navController)
            }
            composable("recentOrders") {
                RecentOrdersScreen(navController)
            }
            composable("orderDetails/{orderId}") { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId")
                OrderDetailsScreen(navController, orderId)
            }

            composable("orderTracking/{restaurantAddress}/{deliveryAddress}") { backStackEntry ->
                val restaurantAddress = backStackEntry.arguments?.getString("restaurantAddress") ?: ""
                val deliveryAddress = backStackEntry.arguments?.getString("deliveryAddress") ?: ""
                OrderTrackingScreen(navController, deliveryAddress, restaurantAddress)
            }

            composable("calendar") {
                CalendarScreen(navController)
            }



        }
    }
}

/**
 * DrawerContent.kt
 * Provides the DrawerContent Composable function, which is used in the main activity to display
 * the navigation drawer content, including user profile information and navigation options.
 */


@Composable
fun DrawerContent(user: FirebaseUser?, navController: NavController, drawerState: DrawerState) {
    val coroutineScope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(16.dp)) {
            Column {
                user?.photoUrl?.let {
                    Image(
                        painter = rememberImagePainter(it),
                        contentDescription = "User Image",
                        modifier = Modifier.size(100.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(user?.displayName ?: "Name")
                Spacer(modifier = Modifier.height(4.dp))
                Text(user?.email ?: "Email")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("home") }) {
            Text("Home")
        }
        Button(onClick = { navController.navigate("recentOrders") }) {
            Text("Recent Orders")
        }
        Button(onClick = { navController.navigate("calendar") }) {
            Text("Calendar")
        }

        // Your other drawer items...
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = {
            FirebaseAuth.getInstance().signOut()
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
            coroutineScope.launch {
                drawerState.close() // Close the drawer
            }
        }) {
            Text("Sign Out")
        }
    }
}
