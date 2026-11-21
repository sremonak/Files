package com.sremonak.files

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sremonak.files.navigation.AppNavHost
import com.sremonak.files.ui.theme.FilesProTheme
import com.sremonak.files.ui.theme.ThemeMode

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settings = FilesApp.instance.settings

        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val dynamicColor by settings.dynamicColor.collectAsStateWithLifecycle(initialValue = true)
            val accentArgb by settings.accentColorArgb.collectAsStateWithLifecycle(initialValue = 0xFF2563EB.toInt())
            val language by settings.language.collectAsStateWithLifecycle(initialValue = "system")

            LaunchedEffect(language) {
                val target = if (language == "system") {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(language)
                }
                if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != target.toLanguageTags()) {
                    AppCompatDelegate.setApplicationLocales(target)
                }
            }

            FilesProTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                accentColor = Color(accentArgb)
            ) {
                AppNavHost()
            }
        }
    }
}
