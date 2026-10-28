package com.orvane.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orvane.app.core.backup.AutoBackupWorker
import com.orvane.app.data.repo.BackupRepository
import com.orvane.app.data.repo.HabitRepository
import com.orvane.app.data.settings.AppSettings
import com.orvane.app.data.settings.PomodoroConfig
import com.orvane.app.data.settings.SettingsRepository
import com.orvane.app.data.settings.ThemeMode
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
    private val habitRepo: HabitRepository,
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
        viewModelScope.launch {
            settings.setNagCutoffHourHour(hour, minute)
            // Re-point every nag alarm at the new (possibly custom) cutoff time.
            habitRepo.rescheduleAll()
        }
    }

    fun setSnoozePresets(presets: List<Int>) {
        viewModelScope.launch { settings.setSnoozePresets(presets) }
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
                result < 0 -> "Restore failed — not a valid Orvane backup."
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
        LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SettingsCard("Theme", Icons.Default.ColorLens) {
                    ThemeRow(s, vm)
                    ToggleRow("Dynamic color (Material You)", s.dynamicColor) { vm.setDynamicColor(it) }
                }
            }
            item {
                SettingsCard("Calendar", Icons.Default.CalendarMonth) {
                    Text("First day of week", style = MaterialTheme.typography.bodyMedium)
                    // Segmented row with equal weights keeps all 7 chips on one line
                    // at any width and font scale (QA: "Sun" wrapped into 3 lines).
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        val days = listOf("Mon" to 1, "Tue" to 2, "Wed" to 3, "Thu" to 4, "Fri" to 5, "Sat" to 6, "Sun" to 7)
                        days.forEachIndexed { index, (label, value) ->
                            SegmentedButton(
                                selected = s.firstDayOfWeek == value,
                                onClick = { vm.setFirstDay(value) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = days.size),
                            ) {
                                Text(label, maxLines = 1)
                            }
                        }
                    }
                }
            }
            item {
                SettingsCard("Focus", Icons.Default.Timer) {
                    PomodoroRow(s.pomodoro, onSet = { vm.setPomodoro(it) })
                }
            }
            item {
                SettingsCard("Notifications", Icons.Default.Notifications) {
                    ToggleRow("Sounds", s.soundsEnabled) { vm.setSounds(it) }
                    ToggleRow("Vibration", s.vibrationEnabled) { vm.setVibration(it) }
                    Text("Snooze presets (minutes on task reminders)", style = MaterialTheme.typography.bodyMedium)
                    SnoozePresetRow(
                        presets = s.snoozePresets,
                        onUpdate = { vm.setSnoozePresets(it) },
                    )
                }
            }
            item {
                SettingsCard("Habits", Icons.Default.Alarm) {
                    Text("Evening nag cutoff", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Unchecked habits are re-reminded at this time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    com.orvane.app.ui.common.CustomTimeChipRow(
                        presetHours = listOf(19, 20, 21, 22),
                        currentHour = s.habitNagCutoffHour,
                        currentMinute = s.habitNagCutoffMinute,
                        onSelect = { h, m -> vm.setNagCutoff(h, m) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    )
                }
            }
            item {
                SettingsCard("App lock", Icons.Default.Lock) {
                    AppLockSection(s, vm)
                }
            }
            item {
                SettingsCard("Backup", Icons.Default.Backup) {
                    BackupSection(vm)
                }
            }
            item {
                SettingsCard("AI assistant (optional)", Icons.Default.SelfImprovement) {
                    AiSection(s, vm)
                }
            }
            item {
                SettingsCard("About", Icons.Default.Info) {
                    Text("Orvane — offline-first tasks, calendar, habits and focus.", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        rememberVersion(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Real package version, falling back to a plain label off the play path. */
@Composable
private fun rememberVersion(): String {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    return remember(ctx) {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }
            .getOrNull()?.let { "v$it" } ?: "v1.0.0"
    }
}

/**
 * Settings section as a rounded card with a leading icon on its title row
 * (Round 2 / C6): one icon family, calm tonal grouping, no divider chains.
 */
@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    androidx.compose.material3.Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        androidx.compose.foundation.layout.Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.Column(content = content)
        }
    }
}

/**
 * Snooze preset minutes (used on task-reminder notifications). Tap a preset to
 * remove it; type a custom number of minutes and Add to extend — Round 2 / B1
 * custom escape hatch for preset-only pickers.
 */
@Composable
private fun SnoozePresetRow(
    presets: List<Int>,
    onUpdate: (List<Int>) -> Unit,
) {
    var customText by rememberSaveable { mutableStateOf("") }
    Column(Modifier.padding(top = 6.dp)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (minutes in presets.sorted()) {
                FilterChip(
                    selected = false,
                    onClick = { onUpdate(presets - minutes) },
                    label = { Text(if (minutes >= 60) "${minutes / 60}h" else "${minutes}m") },
                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove ${minutes}m preset", modifier = Modifier.size(14.dp)) },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = customText,
                onValueChange = { customText = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text("Custom minutes…") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TextButton(
                enabled = customText.toIntOrNull() != null,
                onClick = {
                    val minutes = customText.toIntOrNull() ?: return@TextButton
                    if (minutes > 0 && minutes !in presets) {
                        onUpdate(presets + minutes)
                    }
                    customText = ""
                },
            ) { Text("Add") }
        }
    }
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
            "Export all data as JSON, or just tasks as CSV. Restore replaces matching entries from a Orvane JSON backup. " +
                "An automatic backup also runs every 24 hours and keeps the newest 7 files on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Button(onClick = { jsonLauncher.launch("orvane-backup.json") }) { Text("Backup JSON") }
            OutlinedButton(onClick = { csvLauncher.launch("orvane-tasks.csv") }) { Text("Export CSV") }
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
            "Point Orvane at any OpenAI-compatible endpoint. Your key stays on this device and is sent only to that endpoint. " +
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
