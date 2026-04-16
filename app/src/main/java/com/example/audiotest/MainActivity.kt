package com.example.audiotest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.audiotest.audio.AudioEngine
import com.example.audiotest.ui.*
import com.example.audiotest.ui.theme.AudioTestTheme

class MainActivity : ComponentActivity() {
    private val audioViewModel: AudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the AudioEngine to prepare paths and channels
        AudioEngine.init(this)
        
        enableEdgeToEdge()
        setContent {
            AudioTestTheme {
                MainAppScreen(audioViewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: AudioViewModel) {
    val navController = rememberNavController()

    val navItems = listOf(
        Triple("Playback", "playback", Icons.Filled.PlayArrow),
        Triple("Record", "record", Icons.Filled.Mic),
        Triple("VoIP", "voip", Icons.Filled.Call),
        Triple("MultiTest", "multitest", Icons.Filled.GridView)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                navItems.forEach { (title, route, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = title) },
                        label = { Text(title) },
                        selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                        onClick = {
                            navController.navigate(route) {
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "playback",
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            composable("playback") { PlaybackScreen(viewModel) }
            composable("record") { RecordScreen(viewModel) }
            composable("voip") { VoipScreen(viewModel) }
            composable("multitest") { MultiTestScreen(viewModel) }
        }
    }
}