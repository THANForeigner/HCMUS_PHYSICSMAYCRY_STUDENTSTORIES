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
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.afinal.models.LocationViewModel
import com.example.afinal.data.model.Story
import com.example.afinal.models.StoryViewModel
import com.example.afinal.navigation.AppNavigation
import com.example.afinal.navigation.Routes
import com.example.afinal.ui.screen.MiniPlayer
import com.example.afinal.ui.theme.FINALTheme
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val locationViewModel: LocationViewModel by viewModels()
    private val storyViewModel: StoryViewModel by viewModels()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FINALTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current

                    // Permission State & Launcher
                    var hasAllPermissions by remember {
                        mutableStateOf(
                            (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                                    (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                                    } else true)
                        )
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                        if (fine) hasAllPermissions = true
                    }

                    LaunchedEffect(Unit) {
                        if (!hasAllPermissions) {
                            val req = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) req.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) req.add(Manifest.permission.POST_NOTIFICATIONS)
                            permissionLauncher.launch(req.toTypedArray())
                        }
                    }

                    LaunchedEffect(hasAllPermissions) {
                        if (hasAllPermissions) {
                            try {
                                val client = LocationServices.getFusedLocationProviderClient(context)
                                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                                    .setMinUpdateDistanceMeters(20f)
                                    .build()
                                val intent = Intent(context, LocationReceiver::class.java)
                                val locationPendingIntent = PendingIntent.getBroadcast(
                                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                                )
                                client.requestLocationUpdates(locationRequest, locationPendingIntent)
                            } catch (e: SecurityException) {
                                Log.e("MainActivity", "Permission missing", e)
                            }
                        }
                    }

                    // --- 3. AUDIO SERVICE & UI LOGIC ---
                    var audioService by remember { mutableStateOf<AudioPlayerService?>(null) }
                    var isBound by remember { mutableStateOf(false) }
                    var currentPlayingStory by remember { mutableStateOf<Story?>(null) }

                    var isPlaying by remember { mutableStateOf(false) }

                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

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
                        context.startService(intent)
                        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
                        onDispose { if (isBound) context.unbindService(connection) }
                    }

                    LaunchedEffect(audioService) {
                        while (true) {
                            if (audioService != null && isBound) isPlaying = audioService!!.isPlaying
                            delay(500)
                        }
                    }

                    val isAudioPlayerScreen = currentRoute?.startsWith(Routes.AUDIO_PLAYER) == true
                    val showMiniPlayer = currentPlayingStory != null && !isAudioPlayerScreen
                    val bottomPadding = if (currentRoute == Routes.MAIN_APP) 80.dp else 0.dp

                    Scaffold(
                        containerColor = Color.Transparent // 1. Set Scaffold trong suốt
                    ){ innerPadding ->
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            AppNavigation(
                                navController = navController,
                                startIntent = intent,
                                locationViewModel = locationViewModel,
                                storyViewModel = storyViewModel,
                                audioService = audioService,
                                onStorySelected = { story -> currentPlayingStory = story }
                            )

                            if (showMiniPlayer) {
                                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomPadding)) {
                                    MiniPlayer(
                                        title = currentPlayingStory!!.title,
                                        user = currentPlayingStory!!.user_name,
                                        image = currentPlayingStory!!.imageUrl,
                                        isPlaying = isPlaying,
                                        onPlayPause = { if (isPlaying) audioService?.pauseAudio() else audioService?.resumeAudio() },
                                        onClose = { audioService?.pauseAudio(); currentPlayingStory = null },
                                        onClick = { navController.navigate("${Routes.AUDIO_PLAYER}/${currentPlayingStory!!.id}") }
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