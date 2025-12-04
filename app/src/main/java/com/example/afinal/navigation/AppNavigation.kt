package com.example.afinal.navigation

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.afinal.ui.screen.*
import com.example.afinal.ui.theme.FINALTheme
import com.example.afinal.models.LocationViewModel
import com.example.afinal.models.StoryViewModel
import com.example.afinal.logic.AudioPlayerService
import com.example.afinal.models.StoryModel
import com.google.firebase.auth.FirebaseAuth

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN_APP = "main_app"
    const val HOME = "home"
    const val MAP = "map"
    const val AUDIOS = "audios"
    const val USER = "user"
    const val BAROMETER = "barometer"
    const val AUDIO_PLAYER = "audio_player"
    const val ARG_STORY_ID = "storyID"
    const val ADD_POST = "add_post"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startIntent: Intent? = null,
    locationViewModel: LocationViewModel,
    storyViewModel: StoryViewModel,
    audioService: AudioPlayerService?,
    onStorySelected: (StoryModel) -> Unit
) {
    // This key matches the one used in LocationReceiver.sendDiscoveryNotification
    val notificationStoryId = startIntent?.getStringExtra("notification_story_id")

    // Determine where to start based on Auth and Notifications
    val appStartDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        Routes.MAIN_APP
    } else {
        Routes.LOGIN
    }

    val finalStartDestination = if (notificationStoryId != null) {
        Routes.MAIN_APP // Load Main App first to ensure correct back stack
    } else {
        appStartDestination
    }

    // Handle Deep Link from Notification (Discovery)
    LaunchedEffect(notificationStoryId) {
        if (notificationStoryId != null) {
            // Ensure we are in the main app, then push the player on top
            navController.navigate(Routes.MAIN_APP) {
                popUpTo(Routes.MAIN_APP) { inclusive = true }
            }
            navController.navigate("${Routes.AUDIO_PLAYER}/$notificationStoryId")
        }
    }

    NavHost(navController = navController, startDestination = finalStartDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(navController = navController)
        }
        composable(Routes.REGISTER) {
            RegisterScreen(navController = navController)
        }
        composable(Routes.MAIN_APP) {
            MainAppScreen(
                mainNavController = navController,
                locationViewModel = locationViewModel,
                storyViewModel = storyViewModel
            )
        }
        composable(Routes.ADD_POST) {
            // Pass shared ViewModels to ensure we have the Location data
            AddPostScreen(
                navController = navController,
                locationViewModel = locationViewModel,
                storyViewModel = storyViewModel
            )
        }
        composable(
            route = "${Routes.AUDIO_PLAYER}/{${Routes.ARG_STORY_ID}}",
            arguments = listOf(navArgument(Routes.ARG_STORY_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val storyId = backStackEntry.arguments?.getString(Routes.ARG_STORY_ID)
            if (storyId != null) {
                AudioPlayerScreen(
                    navController = navController,
                    storyId = storyId,
                    storyViewModel = storyViewModel,
                    audioService = audioService,
                    onStoryLoaded = onStorySelected
                )
            } else {
                navController.popBackStack()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    mainNavController: NavHostController,
    locationViewModel: LocationViewModel,
    storyViewModel: StoryViewModel
) {
    val bottomNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = bottomNavController)
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) { HomeScreen(navController = mainNavController) }

            composable(Routes.MAP) {
                MapScreen(navController = bottomNavController, storyViewModel = storyViewModel)
            }

            composable(Routes.AUDIOS) {
                // IMPORTANT: Pass the shared locationViewModel here
                AudiosScreen(
                    navController = mainNavController,
                    storyViewModel = storyViewModel,
                    locationViewModel = locationViewModel
                )
            }

            composable(Routes.USER) { UserScreen(mainNavController = mainNavController) }
            composable(Routes.BAROMETER) { BarometerScreen() }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("Home", Routes.HOME, Icons.Default.Home),
        BottomNavItem("Map", Routes.MAP, Icons.Default.Map),
        BottomNavItem("Audio", Routes.AUDIOS, Icons.Default.Headphones),
        BottomNavItem("User", Routes.USER, Icons.Default.Person),
        BottomNavItem("Barometer", Routes.BAROMETER, Icons.Default.Thermostat)
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class BottomNavItem(val title: String, val route: String, val icon: ImageVector)

@Preview(showBackground = true)
@Composable
fun PreviewMainAppScreen() {
    FINALTheme() {
        MainAppScreen(
            mainNavController = rememberNavController(),
            locationViewModel = viewModel(),
            storyViewModel = viewModel()
        )
    }
}