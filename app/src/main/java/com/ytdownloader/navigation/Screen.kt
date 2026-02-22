package com.ytdownloader.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.ytdownloader.R

sealed class Screen(
    val route: String,
    val label: Int,
    val icon: ImageVector
) {
    object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    object History : Screen("history", R.string.nav_history, Icons.Default.History)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
}
