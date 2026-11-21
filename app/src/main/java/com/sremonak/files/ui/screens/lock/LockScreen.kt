package com.sremonak.files.ui.screens.lock

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sremonak.files.FilesApp
import com.sremonak.files.R
import com.sremonak.files.viewmodel.LockViewModel

@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    viewModel: LockViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val biometricEnabled by FilesApp.instance.settings.biometricEnabled
        .collectAsStateWithLifecycle(initialValue = false)

    fun showBiometric() {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onUnlocked()
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Files Pro")
            .setNegativeButtonText("Cancel")
            .build()
        prompt.authenticate(info)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.lock_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.lock_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        // PIN dots
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            repeat(maxOf(4, state.pin.length)) { index ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < state.pin.length) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
        if (state.error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (state.lockedOut) "Too many attempts. Wait 30 seconds and try again." else stringResource(R.string.lock_wrong_pin),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (state.checking) {
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "⌫")
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.width(300.dp).height(320.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(keys) { key ->
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .clickable {
                            when (key) {
                                "C" -> viewModel.clear()
                                "⌫" -> viewModel.backspace()
                                else -> viewModel.appendDigit(key.first())
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (key == "⌫") {
                        Icon(Icons.Filled.Backspace, contentDescription = "Delete")
                    } else {
                        Text(key, style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.submit(onUnlocked) },
            enabled = state.pin.length >= 4
        ) {
            Text("Unlock")
        }

        if (biometricEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { showBiometric() }) {
                Icon(Icons.Filled.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(R.string.lock_biometric))
            }
        }
    }
}
