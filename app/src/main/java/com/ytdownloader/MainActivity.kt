package com.ytdownloader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.ytdownloader.navigation.Screen
import com.ytdownloader.ui.screens.HistoryScreen
import com.ytdownloader.ui.screens.HomeScreen
import com.ytdownloader.ui.screens.SettingsScreen
import com.ytdownloader.ui.theme.YTDownloaderTheme
import com.ytdownloader.viewmodel.HistoryViewModel
import com.ytdownloader.viewmodel.MainViewModel
import com.ytdownloader.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions()
        mainViewModel.handleIntent(intent)

        setContent {
            YTDownloaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        mainViewModel,
                        historyViewModel,
                        settingsViewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        mainViewModel.handleIntent(intent)
    }

    private fun checkPermissions() {
        val list = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT <= 28 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (list.isNotEmpty()) {
            permissionLauncher.launch(list.toTypedArray())
        }
    }
}

@Composable
fun AppNavigation(
    mainViewModel: MainViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination

    val items = listOf(Screen.Home, Screen.History, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { screen ->
                    NavigationBarItem(
                        selected = destination?.route == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(screen.icon, null) },
                        label = { Text(stringResource(screen.label)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(mainViewModel)
            }
            composable(Screen.History.route) {
                HistoryScreen(historyViewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(settingsViewModel)
            }
        }
    }
}
