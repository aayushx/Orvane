package com.orvane.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orvane.app.data.repo.SampleDataSeeder
import com.orvane.app.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val seeder: SampleDataSeeder,
) : ViewModel() {

    fun finish(withSampleData: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            if (withSampleData) {
                seeder.seed()
            }
            settings.setOnboarded(true)
            onDone()
        }
    }
}

private val PAGES_TITLES = listOf(
    "Welcome to Orvane" to "Your offline-first to-do, calendar, habits and focus — everything stays on this device.",
    "Built for capture" to "Type \"Pay rent tomorrow 5pm !High #home\" and Orvane parses the date, priority and tags for you. Add by voice, widgets, or the big + button.",
    "Ready to go" to "Optionally add sample tasks and habits to explore. You can change everything later in Settings.",
)

/** Three-page first-run onboarding. [onDone] is called after settings are persisted. */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    vm: OnboardingViewModel = hiltViewModel(),
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var seeding by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val (title, body) = PAGES_TITLES[page]
        Spacer(Modifier.height(60.dp))
        Text(
            when (page) {
                0 -> "✅"
                1 -> "✍️"
                else -> "🌱"
            },
            style = MaterialTheme.typography.displayLarge,
        )
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(48.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(PAGES_TITLES.size) { i ->
                Text(
                    if (i == page) "●" else "○",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (page > 0) {
                OutlinedButton(onClick = { page -= 1 }) { Text("Back") }
            } else {
                Spacer(Modifier.height(1.dp))
            }
            when {
                page < PAGES_TITLES.size - 1 ->
                    Button(onClick = { page += 1 }) { Text("Next") }
                else -> {
                    OutlinedButton(
                        enabled = !seeding,
                        onClick = { seeding = true; vm.finish(false, onDone) },
                    ) { Text("Skip sample data") }
                    Button(
                        enabled = !seeding,
                        onClick = { seeding = true; vm.finish(true, onDone) },
                    ) { Text("Start with samples") }
                }
            }
        }
        if (seeding) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Preparing your data…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(48.dp))
    }
}
