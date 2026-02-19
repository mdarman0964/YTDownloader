package com.ytdownloader

import android.app.Application
import androidx.work.Configuration
import com.ytdownloader.data.AppDatabase
import com.ytdownloader.util.SettingsManager
import com.ytdownloader.util.YTDLPManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class YTDApplication : Application(), Configuration.Provider {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        
        // Initialize database
        AppDatabase.getDatabase(this)
        
        // Check for YT-DLP updates if auto-update is enabled
        applicationScope.launch {
            val settingsManager = SettingsManager(this@YTDApplication)
            val ytdlpManager = YTDLPManager(this@YTDApplication)
            
            settingsManager.autoUpdateYTDLP.collect { autoUpdate ->
                if (autoUpdate) {
                    val currentVersion = ytdlpManager.getCurrentVersion()
                    val latestVersion = ytdlpManager.getLatestVersion()
                    
                    if (latestVersion != null && currentVersion != latestVersion) {
                        ytdlpManager.updateYTDLP { }
                    }
                }
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
