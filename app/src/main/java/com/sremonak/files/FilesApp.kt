package com.sremonak.files

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.sremonak.files.data.FileRepository
import com.sremonak.files.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FilesApp : Application() {

    lateinit var settings: SettingsRepository
        private set

    lateinit var files: FileRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = SettingsRepository(this)
        files = FileRepository(this)

        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .crossfade(true)
                .build()
        )

        // Auto-empty expired trash entries on startup.
        appScope.launch {
            val days = settings.trashAutoDays.first()
            files.purgeExpiredTrash(days)
        }
    }

    companion object {
        lateinit var instance: FilesApp
            private set
    }
}
