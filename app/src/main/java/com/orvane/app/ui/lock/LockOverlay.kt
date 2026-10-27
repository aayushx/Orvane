package com.orvane.app.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch

/**
 * Full-screen lock gate. Accepts the saved 4-8 digit PIN, and offers
 * fingerprint/biometric unlock when the device supports it and the user
 * enabled it. Lowers to a friendly error on wrong attempts.
 */
@Composable
fun LockOverlay(
    biometricEnabled: Boolean,
    verifyPin: suspend (String) -> Boolean,
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pin by remember { mutableStateOf("") }
    var attempt by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        scope.launch {
            if (verifyPin(pin)) {
                onUnlocked()
            } else {
                error = "Wrong PIN. Try again."
                attempt += 1
                pin = ""
            }
        }
    }

    // Biometric unlock when available.
    LaunchedEffect(attempt) {
        if (attempt == 0 && biometricEnabled) {
            val fragmentActivity = context as? FragmentActivity ?: return@LaunchedEffect
            val manager = BiometricManager.from(context)
            val canAuth = manager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK,
            )
            if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
                val prompt = BiometricPrompt(
                    fragmentActivity,
                    androidx.core.content.ContextCompat.getMainExecutor(context),
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            onUnlocked()
                        }
                    },
                )
                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Orvane")
                    .setNegativeButtonText("Use PIN")
                    .build()
                prompt.authenticate(info)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🔒", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(8.dp))
        Text("Orvane is locked", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        // PIN dots
        Row {
            repeat(if (pin.length < 4) 4 else pin.length) { i ->
                Box(
                    Modifier
                        .padding(horizontal = 6.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < pin.length) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                )
            }
        }
        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
        Keypad(
            onDigit = { d ->
                if (pin.length < 8) {
                    pin += d
                    error = null
                }
            },
            onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
            onSubmit = ::submit,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Enter your PIN, then press ✓",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Keypad(onDigit: (Char) -> Unit, onBackspace: () -> Unit, onSubmit: () -> Unit) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("⌫", "0", "✓"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in keys) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                for (key in row) {
                    val action: () -> Unit = when (key) {
                        "⌫" -> onBackspace
                        "✓" -> onSubmit
                        else -> { { onDigit(key[0]) } }
                    }
                    Box(
                        Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { action() }
                            .semantics { contentDescription = "Key $key" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(key, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}
