package com.tasky.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasky.app.core.backup.AutoBackupWorker
import com.tasky.app.data.repo.BackupRepository
import com.tasky.app.data.settings.AppSettings
import com.tasky.app.data.settings.PomodoroConfig
import com.tasky.app.data.settings.SettingsRepository
import com.tasky.app.data.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val backupRepo: BackupRepository,
) : ViewModel() {

    val state: StateFlow<AppSettings?> = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), null)

    /** One-shot status message for backup operations. */
    val backupMessage = MutableStateFlow<String?>(null)

    fun setTheme(mode: String) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setDynamicColor(on: Boolean) {
        viewModelScope.launch { settings.setDynamicColor(on) }
    }

    fun setFirstDay(day: Int) {
        viewModelScope.launch { settings.setFirstDayOfWeek(day) }
    }

    fun setSounds(on: Boolean) {
        viewModelScope.launch { settings.setSounds(on) }
    }

    fun setVibration(on: Boolean) {
        viewModelScope.launch { settings.setVibration(on) }
    }

    fun setPomodoro(config: PomodoroConfig) {
        viewModelScope.launch { settings.setPomodoroConfig(config) }
    }

    fun setNagCutoff(hour: Int, minute: Int) {
        viewModelScope.launch { settings.setNagCutoffHourHour(hour, minute) }
    }

    // ---- App lock ----

    fun setPin(pin: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = pin.length in 4..8 && pin.all { it.isDigit() }
            if (ok) settings.setPin(pin)
            onDone(ok)
        }
    }

    fun setAppLock(enabled: Boolean) {
        viewModelScope.launch { settings.setAppLockEnabled(enabled) }
    }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch { settings.setBiometric(enabled) }
    }

    // ---- AI ----

    fun setAiConfig(baseUrl: String, apiKey: String, model: String) {
        viewModelScope.launch { settings.setAiConfig(baseUrl, apiKey, model) }
    }

    // ---- Backup ----

    fun exportBackup(uri: android.net.Uri) {
        viewModelScope.launch {
            val json = backupRepo.buildBackupJson()
            val ok = backupRepo.writeToUri(uri, json)
            backupMessage.value = if (ok) "Backup saved." else "Could not write the file."
        }
    }

    fun exportCsv(uri: android.net.Uri) {
        viewModelScope.launch {
            val csv = backupRepo.buildTasksCsv()
            val ok = backupRepo.writeToUri(uri, csv)
            backupMessage.value = if (ok) "CSV exported." else "Could not write the file."
        }
    }

    fun restoreBackup(uri: android.net.Uri) {
        viewModelScope.launch {
            val text = backupRepo.readFromUri(uri)
            val result = if (text == null) -1 else backupRepo.restoreBackupJson(text)
            backupMessage.value = when {
                result < 0 -> "Restore failed — not a valid Tasky backup."
                else -> "Restored $result tasks."
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val raw by vm.state.collectAsState()
    val s = raw ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp)) {
            item {
                SectionTitle("Theme")
                ThemeRow(s, vm)
                ToggleRow("Dynamic color (Material You)", s.dynamicColor) { vm.setDynamicColor(it) }
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
            }
            item {
                SectionTitle("Calendar")
                Text("First day of week", style = MaterialTheme.typography.bodyMedium)
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val days = listOf("Mon" to 1, "Tue" to 2, "Wed" to 3, "Thu" to 4, "Fri" to 5, "Sat" to 6, "Sun" to 7)
                    for ((label, value) in days) {
                        FilterChip(
                            selected = s.firstDayOfWeek == value,
                            onClick = { vm.setFirstDay(value) },
                            label = { Text(label) },
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
            }
            item {
                SectionTitle("Focus")
                PomodoroRow(s.pomodoro, onSet = { vm.setPomodoro(it) })
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
            }
            item {
                SectionTitle("Notifications")
                ToggleRow("Sounds", s.soundsEnabled) { vm.setSounds(it) }
                ToggleRow("Vibration", s.vibrationEnabled) { vm.setVibration(it) }
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
            }
            item {
                SectionTitle("Habits")
                Text("Evening nag cutoff", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Unchecked habits are re-reminded at this time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(19, 20, 21, 22).forEach { hour ->
                        FilterChip(
                            selected = s.habitNagCutoffHour == hour,
                            onClick = { vm.setNagCutoff(hour, 0) },
                            label = { Text("%02d:00".format(hour)) },
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
            }
            item {
                SectionTitle("App lock")
                AppLockSection(s, vm)
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
            }
            item {
                SectionTitle("Backup")
                BackupSection(vm)
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
            }
            item {
                SectionTitle("AI assistant (optional)")
                AiSection(s, vm)
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
            }
            item {
                SectionTitle("About")
                Text("Tasky — offline-first tasks, calendar, habits and focus.", style = MaterialTheme.typography.bodyMedium)
                Text("v1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ThemeRow(s: AppSettings, vm: SettingsViewModel) {
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf("System" to ThemeMode.SYSTEM, "Light" to ThemeMode.LIGHT, "Dark" to ThemeMode.DARK).forEach { (label, mode) ->
            FilterChip(selected = s.themeMode == mode, onClick = { vm.setTheme(mode) }, label = { Text(label) })
        }
    }
}

@Composable
private fun PomodoroRow(config: PomodoroConfig, onSet: (PomodoroConfig) -> Unit) {
    var focusText by remember(config.focusMinutes) { mutableStateOf(config.focusMinutes.toString()) }
    var shortText by remember(config.shortBreakMinutes) { mutableStateOf(config.shortBreakMinutes.toString()) }
    var longText by remember(config.longBreakMinutes) { mutableStateOf(config.longBreakMinutes.toString()) }
    var cycleText by remember(config.cyclesBeforeLongBreak) { mutableStateOf(config.cyclesBeforeLongBreak.toString()) }
    Column {
        Text("Pomodoro lengths (minutes)", style = MaterialTheme.typography.bodyMedium)
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = focusText,
                onValueChange = { v ->
                    focusText = v
                    v.toIntOrNull()?.let { onSet(config.copy(focusMinutes = it.coerceIn(1, 180))) }
                },
                label = { Text("Focus") },
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.OutlinedTextField(
                value = shortText,
                onValueChange = { v ->
                    shortText = v
                    v.toIntOrNull()?.let { onSet(config.copy(shortBreakMinutes = it.coerceIn(1, 60))) }
                },
                label = { Text("Short") },
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.OutlinedTextField(
                value = longText,
                onValueChange = { v ->
                    longText = v
                    v.toIntOrNull()?.let { onSet(config.copy(longBreakMinutes = it.coerceIn(1, 90))) }
                },
                label = { Text("Long") },
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.OutlinedTextField(
                value = cycleText,
                onValueChange = { v ->
                    cycleText = v
                    v.toIntOrNull()?.let { onSet(config.copy(cyclesBeforeLongBreak = it.coerceIn(1, 12))) }
                },
                label = { Text("× before") },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AppLockSection(s: AppSettings, vm: SettingsViewModel) {
    var pin by remember { mutableStateOf("") }
    var pinMessage by remember { mutableStateOf<String?>(null) }
    var hasPin by remember(s.pinHash) { mutableStateOf(s.pinHash.isNotBlank()) }

    Column(Modifier.padding(vertical = 4.dp)) {
        ToggleRow(
            "Lock app (4–8 digit PIN)",
            s.appLockEnabled,
        ) { enabled ->
            if (!enabled || hasPin) {
                vm.setAppLock(enabled)
            } else {
                pinMessage = "Set a PIN first."
            }
        }
        ToggleRow(
            "Also allow fingerprint unlock",
            s.biometricEnabled,
            onChange = vm::setBiometric,
            enabled = s.appLockEnabled && hasPin,
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(8) },
                label = { Text(if (hasPin) "Change PIN" else "New PIN") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    vm.setPin(pin) { ok ->
                        pinMessage = when {
                            !ok -> "PIN must be 4-8 digits."
                            else -> "PIN saved."
                        }
                        if (ok) {
                            pin = ""
                            vm.setAppLock(true)
                        }
                    }
                },
                enabled = pin.length in 4..8,
            ) { Text(if (hasPin) "Update" else "Set PIN") }
        }
        pinMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun BackupSection(vm: SettingsViewModel) {
    val message by vm.backupMessage.collectAsState()
    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { vm.exportBackup(it) } }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let { vm.exportCsv(it) } }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { vm.restoreBackup(it) } }

    Column(Modifier.padding(vertical = 4.dp)) {
        Text(
            "Export all data as JSON, or just tasks as CSV. Restore replaces matching entries from a Tasky JSON backup. " +
                "An automatic backup also runs every 24 hours and keeps the newest 7 files on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Button(onClick = { jsonLauncher.launch("tasky-backup.json") }) { Text("Backup JSON") }
            OutlinedButton(onClick = { csvLauncher.launch("tasky-tasks.csv") }) { Text("Export CSV") }
            OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/json", "text/*", "*/*")) }) {
                Text("Restore…")
            }
        }
        message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun AiSection(s: AppSettings, vm: SettingsViewModel) {
    var baseUrl by remember(s.aiBaseUrl) { mutableStateOf(s.aiBaseUrl) }
    var apiKey by remember(s.aiApiKey) { mutableStateOf(s.aiApiKey) }
    var model by remember(s.aiModel) { mutableStateOf(s.aiModel) }

    Column(Modifier.padding(vertical = 4.dp)) {
        Text(
            "Point Tasky at any OpenAI-compatible endpoint. Your key stays on this device and is sent only to that endpoint. " +
                "When configured, quick add shows an “AI assist” button.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("Base URL (e.g. https://api.openai.com/v1)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("Model") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        TextButton(
            onClick = { vm.setAiConfig(baseUrl, apiKey, model) },
            enabled = baseUrl.contains("://") || baseUrl.isBlank(),
            modifier = Modifier.padding(top = 2.dp),
        ) { Text("Save AI settings") }
    }
}
