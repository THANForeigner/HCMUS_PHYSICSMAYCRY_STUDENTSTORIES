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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
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
import com.example.afinal.models.AuthViewModel
import com.example.afinal.models.StoryModel
import com.google.firebase.auth.FirebaseAuth
import com.example.afinal.data.model.Story
import com.example.afinal.ui.theme.*

object Routes {
    const val LOGIN = "login"
    const val FORGOT_PASSWORD = "forgot_password"
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
    authViewModel: AuthViewModel = viewModel(),
    audioService: AudioPlayerService?,
    onStorySelected: (Story) -> Unit
) {
    val notificationStoryId = startIntent?.getStringExtra("notification_story_id")

    val appStartDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        Routes.MAIN_APP
    } else {
        Routes.LOGIN
    }

    val finalStartDestination = if (notificationStoryId != null) {
        Routes.MAIN_APP
    } else {
        appStartDestination
    }

    LaunchedEffect(notificationStoryId) {
        if (notificationStoryId != null) {
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
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(navController)
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
                    authViewModel = authViewModel,
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
            composable(Routes.HOME) {
                HomeScreen(
                    storyViewModel = storyViewModel,
                    // 1. Chuyển tab Map (Dùng bottomNavController)
                    onNavigateToMap = {
                        bottomNavController.navigate(Routes.MAP) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    // 2. Chuyển tab Audios (Dùng bottomNavController)
                    onNavigateToAudios = {
                        bottomNavController.navigate(Routes.AUDIOS) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    // 3. Mở Player (Dùng mainNavController - Vì Player nằm ngoài BottomBar)
                    onStoryClick = { storyId ->
                        mainNavController.navigate("${Routes.AUDIO_PLAYER}/$storyId")
                    }
                )
            }

            composable(Routes.MAP) {
                MapScreen(
                    navController = bottomNavController,
                    storyViewModel = storyViewModel
                )
            }

            composable(Routes.AUDIOS) {
                AudiosScreen(
                    navController = mainNavController, // AudiosScreen có thể giữ nguyên nếu nó chỉ mở Player
                    storyViewModel = storyViewModel,
                    locationViewModel = locationViewModel
                )
            }

            composable(Routes.USER) { UserScreen(mainNavController = mainNavController, storyViewModel = storyViewModel) }
            composable(Routes.BAROMETER) { BarometerScreen() }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    // Định nghĩa danh sách tab với màu chủ đạo riêng
    val items = listOf(
        BottomNavItem(
            title = "Home",
            route = Routes.HOME,
            icon = Icons.Default.Home,
            color = BluePrimary
        ),
        BottomNavItem(
            title = "Map",
            route = Routes.MAP,
            icon = Icons.Default.Map,
            color = TealAccent
        ),
        BottomNavItem(
            title = "Audio",
            route = Routes.AUDIOS,
            icon = Icons.Default.Headphones,
            color = BlueLight
        ),
        BottomNavItem(
            title = "User",
            route = Routes.USER,
            icon = Icons.Default.Person,
            color = BlueDark
        ),
    )

    NavigationBar(
        containerColor = Color.White, // Nền trắng sạch sẽ
        tonalElevation = 8.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = {
                    Text(
                        text = item.title,
                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                    )
                },
                selected = isSelected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                // Cấu hình màu sắc Highlight theo từng item
                colors = NavigationBarItemDefaults.colors(
                    // 1. Khi được chọn:
                    selectedIconColor = Color.White,       // Icon chuyển sang trắng
                    selectedTextColor = item.color,        // Chữ chuyển sang màu chủ đạo của tab
                    indicatorColor = item.color,           // Pill (nền icon) có màu chủ đạo

                    // 2. Khi KHÔNG chọn:
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}

// Cập nhật Data Class thêm trường color
data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
    val color: Color
)

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