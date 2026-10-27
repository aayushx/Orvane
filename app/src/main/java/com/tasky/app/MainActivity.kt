package com.tasky.app

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasky.app.data.db.SmartList
import com.tasky.app.data.settings.AppSettings
import com.tasky.app.data.settings.SettingsRepository
import com.tasky.app.ui.calendar.CalendarScreen
import com.tasky.app.ui.drawer.DrawerContent
import com.tasky.app.ui.focus.FocusScreen
import com.tasky.app.ui.habits.HabitsScreen
import com.tasky.app.ui.lock.LockOverlay
import com.tasky.app.ui.onboarding.OnboardingScreen
import com.tasky.app.ui.quickadd.QuickAddSheet
import com.tasky.app.ui.quickadd.QuickAddViewModel
import com.tasky.app.ui.search.SearchScreen
import com.tasky.app.ui.settings.SettingsScreen
import com.tasky.app.ui.tasks.TaskDetailScreen
import com.tasky.app.ui.tasks.TasksScreen
import com.tasky.app.ui.tasks.TasksViewModel
import com.tasky.app.ui.theme.TaskyTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Tab(val title: String) {
    Today("Today"),
    Calendar("Calendar"),
    Habits("Habits"),
    Focus("Focus"),
}

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    companion object {
        /** When launched with this extra, the app opens straight into quick add. */
        const val EXTRA_OPEN_QUICK_ADD = "com.tasky.app.extra.OPEN_QUICK_ADD"
    }

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // Manifest declares Theme.Tasky.Splash; installSplashScreen() applies
        // postSplashScreenTheme (Theme.Tasky) and dismisses the splash view.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val quickAdd = intent?.getBooleanExtra(EXTRA_OPEN_QUICK_ADD, false) == true
        setContent {
            TaskyTheme {
                AppRoot(openQuickAddOnStart = quickAdd)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_QUICK_ADD, false)) {
            QuickAddBus.requestQuickAdd()
        }
    }
}

/** Bridges widget → activity to open quick add on warm launches. */
object QuickAddBus {
    val requests = kotlinx.coroutines.flow.MutableStateFlow(0)
    fun requestQuickAdd() {
        requests.value += 1
    }
}

/** Exposes app-level settings to the activity shell (lock / onboarding gates). */
@HiltViewModel
class ShellViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
) : ViewModel() {
    val settings: StateFlow<AppSettings?> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), null)

    suspend fun verifyPin(pin: String): Boolean = settingsRepo.pinMatches(pin)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(openQuickAddOnStart: Boolean) {
    val shell: ShellViewModel = hiltViewModel()
    val settings by shell.settings.collectAsState()
    val s = settings

    // ---- Lock gate: re-locks when the app comes back from the background. ----
    val locked = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var lockArmed by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner, s?.appLockEnabled == true) {
        val lockEnabled = s?.appLockEnabled == true && !s.pinHash.isNullOrBlank()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START && lockEnabled) {
                locked.value = true
                lockArmed = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (s != null && s.appLockEnabled && s.pinHash.isNotBlank() && !lockArmed) {
        LaunchedEffect(Unit) { locked.value = true }
    }

    if (s == null) {
        Surface(Modifier.fillMaxSize()) {}
        return
    }
    if (!s.onboarded) {
        OnboardingScreen(onDone = {})
        return
    }
    if (locked.value) {
        LockOverlay(
            biometricEnabled = s.biometricEnabled,
            verifyPin = { pin -> shell.verifyPin(pin) },
            onUnlocked = { locked.value = false },
        )
        return
    }

    val vm: TasksViewModel = hiltViewModel()
    val quickVm: QuickAddViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var tab by remember { mutableStateOf(Tab.Today) }
    var quickAddRequested by remember { mutableStateOf(openQuickAddOnStart) }
    val busRequests by QuickAddBus.requests.collectAsState()
    androidx.compose.runtime.LaunchedEffect(busRequests) {
        if (busRequests > 0) quickAddRequested = true
    }

    var showSettings by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var openTaskId by remember { mutableStateOf<String?>(null) }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            DrawerContent(
                currentKey = state.viewKey,
                onOpenList = { key ->
                    vm.selectView(key)
                    tab = Tab.Today
                    scope.launch { drawer.close() }
                },
            )
        },
    ) {
        Scaffold(
            topBar = {
                when {
                    openTaskId != null -> {} // detail screen has its own header
                    showSettings -> {} // settings has its own header
                    showSearch -> {} // search has its own field
                    else -> TopAppBar(
                        title = { Text(state.viewTitle) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawer.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Navigation menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showSearch = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                }
            },
            bottomBar = {
                if (openTaskId == null && !showSettings) {
                    BottomBar(
                        tab = tab,
                        onTabSelected = { selected ->
                            tab = selected
                            if (selected == Tab.Today) vm.selectView(SmartList.TODAY)
                        },
                        onCenterAction = { quickAddRequested = true },
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when {
                    openTaskId != null -> {
                        TaskDetailScreen(
                            taskId = openTaskId!!,
                            snackbarHostState = snackbarHostState,
                            onBack = { openTaskId = null },
                        )
                    }
                    showSettings -> SettingsScreen(onBack = { showSettings = false })
                    showSearch -> SearchScreen(
                        onBack = { showSearch = false },
                        onOpenTask = { id -> showSearch = false; openTaskId = id },
                    )
                    else -> when (tab) {
                        Tab.Today -> TasksScreen(
                            vm = vm,
                            snackbarHostState = snackbarHostState,
                            onOpenTask = { id -> openTaskId = id },
                        )
                        Tab.Calendar -> CalendarScreen()
                        Tab.Habits -> HabitsScreen()
                        Tab.Focus -> FocusScreen()
                    }
                }
            }
        }
    }

    if (quickAddRequested) {
        QuickAddSheet(
            onDismiss = { quickAddRequested = false },
            vm = quickVm,
        )
    }
}

@Composable
private fun BottomBar(tab: Tab, onTabSelected: (Tab) -> Unit, onCenterAction: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        NavigationBar {
            NavigationBarItem(
                selected = tab == Tab.Today,
                onClick = { onTabSelected(Tab.Today) },
                icon = { Icon(Icons.Default.Today, contentDescription = null) },
                label = { Text("Today") },
            )
            NavigationBarItem(
                selected = tab == Tab.Calendar,
                onClick = { onTabSelected(Tab.Calendar) },
                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                label = { Text("Calendar") },
            )
            NavigationBarItem(
                selected = false,
                onClick = {},
                enabled = false,
                icon = {},
                label = { Text("") },
            )
            NavigationBarItem(
                selected = tab == Tab.Habits,
                onClick = { onTabSelected(Tab.Habits) },
                icon = { Icon(Icons.Filled.SelfImprovement, contentDescription = null) },
                label = { Text("Habits") },
            )
            NavigationBarItem(
                selected = tab == Tab.Focus,
                onClick = { onTabSelected(Tab.Focus) },
                icon = { Icon(Icons.Filled.Timer, contentDescription = null) },
                label = { Text("Focus") },
            )
        }
        FloatingActionButton(
            onClick = onCenterAction,
            modifier = Modifier
                .align(Alignment.Center)
                .width(56.dp)
                .height(56.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Default.Add, contentDescription = "Quick add")
        }
    }
}
