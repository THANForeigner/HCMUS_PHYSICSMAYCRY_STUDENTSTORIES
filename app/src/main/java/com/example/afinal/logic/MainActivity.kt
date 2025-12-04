package com.example.afinal.logic

import android.Manifest
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.afinal.models.LocationViewModel
import com.example.afinal.models.StoryModel
import com.example.afinal.models.StoryViewModel
import com.example.afinal.navigation.AppNavigation
import com.example.afinal.navigation.Routes
import com.example.afinal.ui.screen.MiniPlayer
import com.example.afinal.ui.theme.FINALTheme
import com.example.afinal.logic.LocationGPS
import com.example.afinal.ultis.LocationReceiver
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val locationViewModel: LocationViewModel by viewModels()
    private val storyViewModel: StoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FINALTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    val locationGPS = remember { LocationGPS(context) }
                    var hasForegroundPermission by remember {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        )
                    }
                    var hasAllPermissions by remember {
                        mutableStateOf(
                            (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                                    } else true)
                        )
                    }
                    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            Toast.makeText(context, "Background location enabled!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Background location needed for Geofencing", Toast.LENGTH_LONG).show()
                        }
                    }

                    val foregroundPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

                        if (fineLocationGranted || coarseLocationGranted) {
                            hasForegroundPermission = true
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val bgPermission = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                )
                                if (bgPermission != PackageManager.PERMISSION_GRANTED) {
                                    backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                }
                            }
                        }
                    }
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
                        } else true

                        hasAllPermissions = locationGranted && notificationGranted
                        if (locationGranted) {
                            locationGPS.requestLocationUpdate(locationViewModel)
                        }
                    }
                    LaunchedEffect(Unit) {
                        if (!hasAllPermissions) {
                            val permissionsToRequest = mutableListOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(permissionsToRequest.toTypedArray())
                        } else {
                            locationGPS.requestLocationUpdate(locationViewModel)
                        }
                    }
                    LaunchedEffect(hasAllPermissions) {
                        if (hasAllPermissions) {
                            // A. Setup the Request
                            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // 10 seconds
                                .setMinUpdateDistanceMeters(10f)
                                .build()

                            // B. Create the PendingIntent that wakes up the Receiver
                            val intent = Intent(context, LocationReceiver::class.java)
                            val locationPendingIntent = PendingIntent.getBroadcast(
                                context,
                                0,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                            )

                            // C. Request Updates (This persists even if app dies)
                            // Make sure you check for permissions before calling this
                            try {
                                val client = LocationServices.getFusedLocationProviderClient(context)
                                client.requestLocationUpdates(locationRequest, locationPendingIntent)
                                Toast.makeText(context, "Tracking started (Background Enabled)", Toast.LENGTH_SHORT).show()
                            } catch (e: SecurityException) {
                                Log.e("MainActivity", "Permission missing", e)
                            }
                        }
                    }
                    LaunchedEffect(Unit) {
                        val permissionsToRequest = mutableListOf<String>()
                        if (!hasForegroundPermission) {
                            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        if (permissionsToRequest.isNotEmpty()) {
                            foregroundPermissionLauncher.launch(permissionsToRequest.toTypedArray())
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                }
                            }
                        }
                    }
                    // --- 2. LOGIC AUDIO SERVICE & MINIPLAYER (PHẦN MỚI THÊM) ---
                    var audioService by remember { mutableStateOf<AudioPlayerService?>(null) }
                    var isBound by remember { mutableStateOf(false) }

                    // State để điều khiển UI MiniPlayer
                    var currentPlayingStory by remember { mutableStateOf<StoryModel?>(null) }
                    var isPlaying by remember { mutableStateOf(false) }

                    // [QUAN TRỌNG] Tạo NavController tại đây để quản lý luồng
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    // Kết nối Service
                    val connection = remember {
                        object : ServiceConnection {
                            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                                val binder = service as AudioPlayerService.LocalBinder
                                audioService = binder.getService()
                                isBound = true
                            }
                            override fun onServiceDisconnected(arg0: ComponentName) {
                                isBound = false; audioService = null
                            }
                        }
                    }

                    DisposableEffect(Unit) {
                        val intent = Intent(context, AudioPlayerService::class.java)
                        context.startService(intent) // Start Service
                        context.bindService(intent, connection, Context.BIND_AUTO_CREATE) // Bind Service
                        onDispose { if (isBound) context.unbindService(connection) }
                    }

                    // Cập nhật trạng thái Play/Pause liên tục
                    LaunchedEffect(audioService) {
                        while (true) {
                            if (audioService != null && isBound) {
                                isPlaying = audioService!!.isPlaying
                            }
                            delay(500)
                        }
                    }

                    // --- 3. UI SETUP (XẾP LỚP GIAO DIỆN) ---

                    // Điều kiện hiển thị MiniPlayer: Có bài hát VÀ KHÔNG ở màn hình Full Player
                    val isAudioPlayerScreen = currentRoute?.startsWith(Routes.AUDIO_PLAYER) == true
                    val showMiniPlayer = currentPlayingStory != null && !isAudioPlayerScreen

                    // Nếu ở MainApp (có BottomBar), đẩy MiniPlayer lên 80dp
                    val isMainAppScreen = currentRoute == Routes.MAIN_APP
                    val bottomPadding = if (isMainAppScreen) 80.dp else 0.dp

                    Scaffold { innerPadding ->
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                        ) {
                            // Layer 1: App Navigation (Nằm dưới cùng)
                            AppNavigation(
                                navController = navController, // Truyền Controller xuống
                                startIntent = intent,
                                locationViewModel = locationViewModel,
                                storyViewModel = storyViewModel,
                                audioService = audioService,       // Truyền Service xuống
                                onStorySelected = { story ->       // Nhận bài hát từ AudioScreen
                                    currentPlayingStory = story
                                }
                            )

                            // Layer 2: MiniPlayer (Nằm đè lên trên)
                            if (showMiniPlayer) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = bottomPadding) // Padding để tránh BottomBar
                                ) {
                                    MiniPlayer(
                                        title = currentPlayingStory!!.name,
                                        user = currentPlayingStory!!.user,
                                        isPlaying = isPlaying,
                                        onPlayPause = {
                                            if (isPlaying) audioService?.pauseAudio() else audioService?.resumeAudio()
                                        },
                                        onClose = {
                                            audioService?.pauseAudio()
                                            currentPlayingStory = null // Ẩn player
                                        },
                                        onClick = {
                                            // Mở lại màn hình Full Player
                                            val storyId = currentPlayingStory!!.id
                                            navController.navigate("${Routes.AUDIO_PLAYER}/$storyId")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}