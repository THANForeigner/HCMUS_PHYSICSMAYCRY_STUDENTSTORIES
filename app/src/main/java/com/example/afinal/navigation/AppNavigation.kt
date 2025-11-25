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
import com.example.afinal.ui.screen.*
import com.example.afinal.ui.theme.FINALTheme

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN_APP = "main_app"
    const val HOME = "home"
    const val MAP = "map"
    const val AUDIOS = "audios"
    const val USER = "user"
    const val BAROMETER = "barometer"
    const val  AUDIO_PLAYER = "audio_player"
    const val ARG_STORY_ID = "storyID"
}

@Composable
fun AppNavigation(startIntent: Intent? = null) {
    val navController = rememberNavController()
    val notificationStoryId = startIntent?.getStringExtra("notification_story_id")
    val startDest = if (notificationStoryId != null) {
        // Nếu có ID, ta sẽ xử lý navigate ở trong LaunchedEffect bên dưới
        Routes.MAIN_APP
    } else {
        Routes.LOGIN
    }
    LaunchedEffect(notificationStoryId) {
        if (notificationStoryId != null) {
            // Mở thẳng vào Main App rồi vào Player
            navController.navigate(Routes.MAIN_APP)
            navController.navigate("${Routes.AUDIO_PLAYER}/$notificationStoryId")
        }
    }
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(navController = navController)
        }
        composable(Routes.REGISTER) {
            RegisterScreen(navController = navController)
        }
        composable(Routes.MAIN_APP) {
            MainAppScreen(mainNavController = navController)
        }
        composable(
            route = "${Routes.AUDIO_PLAYER}/{${Routes.ARG_STORY_ID}}",
            arguments = listOf(navArgument(Routes.ARG_STORY_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            // Lấy storyId từ route
            val storyId = backStackEntry.arguments?.getString(Routes.ARG_STORY_ID)
            if (storyId != null) {
                AudioPlayerScreen(
                    navController = navController,
                    storyId = storyId
                )
            } else {
                // Xử lý lỗi (ví dụ: quay lại)
                navController.popBackStack()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(mainNavController: NavHostController) {
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
            composable(Routes.MAP) { MapScreen(navController = mainNavController) }
            composable(Routes.AUDIOS) { AudiosScreen(navController = mainNavController) }
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
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid re-launching the same screen
                        launchSingleTop = true
                        // Restore state when re-selecting a previously selected item
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
        MainAppScreen(mainNavController = rememberNavController())
    }
}