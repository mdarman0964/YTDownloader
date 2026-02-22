package com.ytdownloader

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.ytdownloader.R

sealed class Screen(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int
) {
    object Home : Screen("home", Icons.Filled.Home, R.string.home)
    object History : Screen("history", Icons.Filled.History, R.string.history)
    object Settings : Screen("settings", Icons.Filled.Settings, R.string.settings)
}
