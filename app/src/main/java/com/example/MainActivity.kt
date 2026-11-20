package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.PreferencesRepository
import com.example.data.ScamAiRepository
import com.example.data.ScamRepository
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.ScanType
import com.example.model.ThemePreference
import com.example.security.configureAppCheck
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.screens.AccountSettingsScreen
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.EmergencyHelpScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.ScanHistoryScreen
import com.example.ui.screens.ScannerDetailScreen
import com.example.ui.screens.ScannersHubScreen
import com.example.ui.screens.ShieldAiScreen
import com.example.ui.theme.ScamShieldTheme
import com.example.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

private enum class TopLevelTab(val icon: ImageVector) {
    HOME(Icons.Default.Home),
    SCANNERS(Icons.Default.Security),
    COMMUNITY(Icons.Default.Groups),
    SHIELD_AI(Icons.Default.Shield),
    EMERGENCY(Icons.Default.Warning)
}

private sealed interface AppScreen {
    data object MainTabs : AppScreen
    data class ScannerDetail(val scanType: ScanType) : AppScreen
    data object ScanHistory : AppScreen
    data object AccountSettings : AppScreen
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        val preferencesRepository = PreferencesRepository(applicationContext)

        setContent {
            val language by preferencesRepository.languageFlow
                .collectAsStateWithLifecycle(initialValue = AppLanguage.EN)
            val themePreference by preferencesRepository.themeFlow
                .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)
            val scope = rememberCoroutineScope()

            ScamShieldTheme(themePreference = themePreference) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        language = language,
                        themePreference = themePreference,
                        onSelectLanguage = { lang -> scope.launch { preferencesRepository.setLanguage(lang) } }
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    language: AppLanguage,
    themePreference: ThemePreference,
    onSelectLanguage: (AppLanguage) -> Unit,
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var isGuestMode by remember { mutableStateOf(false) }
    val user = currentUser

    if (user != null) {
        key(user.uid) {
            ScamShieldAppContent(
                sessionKey = user.uid,
                isGuestMode = false,
                displayName = user.displayName ?: user.phoneNumber ?: "Friend",
                userEmail = user.email,
                userPhone = user.phoneNumber,
                themePreference = themePreference,
                onExitSession = { /* Handled via Firebase signOut */ }
            )
        }
    } else if (isGuestMode) {
        key("guest_demo_session") {
            ScamShieldAppContent(
                sessionKey = "guest_demo_session",
                isGuestMode = true,
                displayName = "Guest",
                userEmail = null,
                userPhone = null,
                themePreference = themePreference,
                onExitSession = { isGuestMode = false }
            )
        }
    } else {
        AuthScreen(
            selectedLanguage = language,
            onSelectLanguage = onSelectLanguage,
            onAuthSuccess = { isGuestMode = false },
            onEnterGuestMode = { isGuestMode = true }
        )
    }
}

@Composable
private fun ScamShieldAppContent(
    sessionKey: String,
    isGuestMode: Boolean,
    displayName: String,
    userEmail: String?,
    userPhone: String?,
    themePreference: ThemePreference,
    onExitSession: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: MainViewModel = viewModel(
        key = sessionKey,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val repo = if (isGuestMode) {
                    null
                } else {
                    val databaseId = app.getString(R.string.firestore_database_id)
                    ScamRepository(FirebaseFirestore.getInstance(databaseId))
                }
                MainViewModel(
                    scamRepository = repo,
                    aiRepository = ScamAiRepository(),
                    preferencesRepository = PreferencesRepository(app),
                    currentUserId = sessionKey,
                    initialDisplayName = displayName,
                    initialPhoneNumber = userPhone
                )
            }
        }
    )

    val language by viewModel.language.collectAsStateWithLifecycle()
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val scanHistoryState by viewModel.scanHistoryState.collectAsStateWithLifecycle()
    val communityReportsState by viewModel.communityReportsState.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val phoneLookupState by viewModel.phoneLookupState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatStreaming by viewModel.isChatStreaming.collectAsStateWithLifecycle()
    val actionFeedback by viewModel.actionFeedback.collectAsStateWithLifecycle()

    val strings = AppStrings.forLanguage(language)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionFeedback) {
        val msg = actionFeedback
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionFeedback()
        }
    }

    var currentTab by remember { mutableStateOf(TopLevelTab.HOME) }
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.MainTabs) }
    var pendingReportTarget by remember { mutableStateOf<String?>(null) }

    val openScanner: (ScanType) -> Unit = { type ->
        viewModel.resetScanState()
        currentScreen = AppScreen.ScannerDetail(type)
    }

    if (currentScreen !is AppScreen.MainTabs) {
        BackHandler {
            currentScreen = AppScreen.MainTabs
        }
    }

    fun tabLabel(tab: TopLevelTab): String = when (tab) {
        TopLevelTab.HOME -> strings.navHome
        TopLevelTab.SCANNERS -> strings.navScanners
        TopLevelTab.COMMUNITY -> strings.navCallsAndCommunity
        TopLevelTab.SHIELD_AI -> strings.navShieldAi
        TopLevelTab.EMERGENCY -> strings.navEmergency
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useRail && currentScreen is AppScreen.MainTabs) {
                    NavigationBar {
                        TopLevelTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tabLabel(tab)) },
                                label = { Text(tabLabel(tab), maxLines = 1) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && currentScreen is AppScreen.MainTabs) {
                    NavigationRail {
                        TopLevelTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tabLabel(tab)) },
                                label = { Text(tabLabel(tab)) },
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (val screen = currentScreen) {
                        is AppScreen.MainTabs -> {
                            when (currentTab) {
                                TopLevelTab.HOME -> HomeDashboardScreen(
                                    displayName = displayName,
                                    language = language,
                                    scanHistoryState = scanHistoryState,
                                    onOpenScanner = openScanner,
                                    onOpenAllScanners = { currentTab = TopLevelTab.SCANNERS },
                                    onOpenHistory = { currentScreen = AppScreen.ScanHistory },
                                    onOpenAccount = { currentScreen = AppScreen.AccountSettings },
                                    onOpenEmergency = { currentTab = TopLevelTab.EMERGENCY }
                                )
                                TopLevelTab.SCANNERS -> ScannersHubScreen(
                                    onOpenScanner = openScanner
                                )
                                TopLevelTab.COMMUNITY -> CommunityScreen(
                                    currentUserId = sessionKey,
                                    reportsState = communityReportsState,
                                    initialReportTarget = pendingReportTarget,
                                    onClearInitialTarget = { pendingReportTarget = null },
                                    onOpenPhoneScanner = { openScanner(ScanType.PHONE) },
                                    onSubmitReport = viewModel::submitCommunityReport,
                                    onDeleteReport = viewModel::deleteCommunityReport
                                )
                                TopLevelTab.SHIELD_AI -> ShieldAiScreen(
                                    language = language,
                                    messages = chatMessages,
                                    isStreaming = isChatStreaming,
                                    onSendMessage = viewModel::sendShieldAiMessage
                                )
                                TopLevelTab.EMERGENCY -> EmergencyHelpScreen(
                                    language = language
                                )
                            }
                        }

                        is AppScreen.ScannerDetail -> {
                            ScannerDetailScreen(
                                scanType = screen.scanType,
                                language = language,
                                scanState = scanState,
                                phoneLookupState = phoneLookupState,
                                onBack = { currentScreen = AppScreen.MainTabs },
                                onRunTextScan = viewModel::runTextScan,
                                onRunImageScan = viewModel::runImageScan,
                                onLookupPhone = viewModel::lookupPhoneNumber,
                                onNavigateToReportNumber = { target ->
                                    pendingReportTarget = target
                                    currentScreen = AppScreen.MainTabs
                                    currentTab = TopLevelTab.COMMUNITY
                                }
                            )
                        }

                        is AppScreen.ScanHistory -> {
                            ScanHistoryScreen(
                                language = language,
                                historyState = scanHistoryState,
                                onBack = { currentScreen = AppScreen.MainTabs },
                                onDeleteScan = viewModel::deleteScanRecord,
                                onClearAll = viewModel::clearScanHistory
                            )
                        }

                        is AppScreen.AccountSettings -> {
                            AccountSettingsScreen(
                                isGuestMode = isGuestMode,
                                userEmail = userEmail,
                                userPhone = userPhone,
                                profileState = userProfileState,
                                language = language,
                                themePreference = themePreference,
                                onBack = { currentScreen = AppScreen.MainTabs },
                                onSelectLanguage = viewModel::setLanguage,
                                onSelectTheme = viewModel::setTheme,
                                onSaveProfile = viewModel::updateProfileSettings,
                                onSignOut = {
                                    if (isGuestMode) {
                                        onExitSession()
                                    } else {
                                        signOut(credentialManager, scope)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
