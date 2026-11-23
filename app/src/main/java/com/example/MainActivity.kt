package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SadhanaRepository
import com.example.data.remote.GoogleAuthHelper
import com.example.domain.UserProfile
import com.example.domain.UserRole
import com.example.domain.WhatsAppShareHelper
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.RoleOnboardingScreen
import com.example.ui.components.WhatsAppComposerSheet
import com.example.ui.counsellor.CounsellorDashboardScreen
import com.example.ui.counsellor.CounsellorDevoteeDetailScreen
import com.example.ui.counsellor.CounsellorProfileScreen
import com.example.ui.devotee.DevoteeHistoryScreen
import com.example.ui.devotee.DevoteeProfileScreen
import com.example.ui.devotee.DevoteeTodayScreen
import com.example.ui.devotee.DevoteeWeeklyTabScreen
import com.example.ui.devotee.FocusJapaScreen
import com.example.ui.theme.SadhanaSevaTheme
import com.example.viewmodel.CounsellorViewModel
import com.example.viewmodel.DevoteeViewModel
import com.example.viewmodel.SessionUiState
import com.example.viewmodel.SessionViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SadhanaSevaTheme {
                SadhanaAppRoot()
            }
        }
    }
}

@Composable
fun SadhanaAppRoot() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedSessionHost(
                uid = uid,
                displayNameHint = user.displayName.orEmpty(),
                emailHint = user.email.orEmpty(),
                onSignedOut = { currentUser = null }
            )
        }
    }
}

@Composable
private fun AuthenticatedSessionHost(
    uid: String,
    displayNameHint: String,
    emailHint: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val repository = remember(context) { SadhanaRepository(context.applicationContext) }

    val sessionViewModel: SessionViewModel = viewModel(
        key = "session_$uid",
        factory = SessionViewModel.Factory(uid, displayNameHint, emailHint, repository)
    )
    val sessionState by sessionViewModel.state.collectAsStateWithLifecycle()
    val sessionMessage by sessionViewModel.actionMessage.collectAsStateWithLifecycle()
    val sessionBusy by sessionViewModel.busy.collectAsStateWithLifecycle()

    val performSignOut: () -> Unit = {
        GoogleAuthHelper.signOut(
            credentialManager = credentialManager,
            scope = scope,
            onBeforeSignOut = { repository.clearLocalSessionCache() },
            onSignOutComplete = onSignedOut
        )
    }

    when (val s = sessionState) {
        is SessionUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Preparing Sadhana Seva…", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        is SessionUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = s.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { sessionViewModel.loadProfile() }) {
                        Text("Retry")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = performSignOut) {
                        Text("Sign Out")
                    }
                }
            }
        }

        is SessionUiState.NeedsOnboarding -> {
            RoleOnboardingScreen(
                defaultName = s.defaultName,
                email = s.email,
                busy = sessionBusy,
                errorMessage = sessionMessage,
                onComplete = { name, role, wa, code ->
                    sessionViewModel.completeOnboarding(name, role, wa, code)
                },
                onSignOut = performSignOut
            )
        }

        is SessionUiState.Ready -> {
            if (s.profile.userRole == UserRole.COUNSELLOR) {
                CounsellorShell(
                    profile = s.profile,
                    repository = repository,
                    sessionBusy = sessionBusy,
                    sessionMessage = sessionMessage,
                    onClearSessionMessage = sessionViewModel::clearMessage,
                    onUpdateContact = sessionViewModel::updateContactInfo,
                    onSignOut = performSignOut
                )
            } else {
                DevoteeShell(
                    profile = s.profile,
                    repository = repository,
                    sessionBusy = sessionBusy,
                    sessionMessage = sessionMessage,
                    onClearSessionMessage = sessionViewModel::clearMessage,
                    onProfileUpdated = sessionViewModel::updateProfileLocally,
                    onLinkCounsellor = sessionViewModel::linkCounsellor,
                    onSignOut = performSignOut
                )
            }
        }
    }
}

@Composable
private fun DevoteeShell(
    profile: UserProfile,
    repository: SadhanaRepository,
    sessionBusy: Boolean,
    sessionMessage: String?,
    onClearSessionMessage: () -> Unit,
    onProfileUpdated: (UserProfile) -> Unit,
    onLinkCounsellor: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val devoteeVm: DevoteeViewModel = viewModel(
        key = "devotee_${profile.uid}",
        factory = DevoteeViewModel.Factory(profile, context.applicationContext, repository, onProfileUpdated)
    )
    LaunchedEffect(profile) {
        devoteeVm.updateProfile(profile)
    }

    val state by devoteeVm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showFocusJapa by remember { mutableStateOf(false) }
    var whatsAppDraftMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.bannerMessage) {
        val msg = state.bannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            devoteeVm.clearBanner()
        }
    }
    LaunchedEffect(sessionMessage) {
        if (!sessionMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(sessionMessage)
            onClearSessionMessage()
        }
    }

    if (showFocusJapa) {
        FocusJapaScreen(
            roundsCompleted = state.draft.japaRounds,
            targetRounds = state.profile.targets.japaRoundsTarget,
            currentBead = state.draft.japaBeads,
            chantingSlot = state.draft.chantingEnum,
            onTapBead = devoteeVm::tapJapaBead,
            onAdjustRounds = devoteeVm::adjustJapaRounds,
            onResetBead = devoteeVm::resetJapaBeadsOnly,
            onMarkRoundsCompleteNow = devoteeVm::markRoundsDoneNow,
            onClose = { showFocusJapa = false }
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(if (selectedTab == 0) Icons.Filled.EditCalendar else Icons.Outlined.EditCalendar, contentDescription = "Today") },
                    label = { Text("Today") },
                    modifier = Modifier.testTag("nav_today")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(if (selectedTab == 1) Icons.Filled.CalendarViewWeek else Icons.Outlined.CalendarViewWeek, contentDescription = "Weekly Card") },
                    label = { Text("Card") },
                    modifier = Modifier.testTag("nav_weekly")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(if (selectedTab == 2) Icons.Filled.History else Icons.Outlined.History, contentDescription = "History") },
                    label = { Text("History") },
                    modifier = Modifier.testTag("nav_history")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(if (selectedTab == 3) Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    modifier = Modifier.testTag("nav_profile")
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                0 -> DevoteeTodayScreen(
                    state = state,
                    onSelectDate = devoteeVm::selectDate,
                    onUpdateDraft = devoteeVm::updateDraft,
                    onOpenFocusJapa = { showFocusJapa = true },
                    onAdjustJapaRounds = devoteeVm::adjustJapaRounds,
                    onToggleReadingTimer = devoteeVm::toggleReadingTimer,
                    onResetReadingTimer = devoteeVm::resetReadingTimer,
                    onCommitReadingTimer = devoteeVm::commitReadingTimerToDraft,
                    onSaveCheckIn = devoteeVm::saveDailyCheckIn,
                    onSyncNow = devoteeVm::syncPendingNow,
                    onOpenReminders = { selectedTab = 3 },
                    onOpenWhatsAppDaily = {
                        whatsAppDraftMessage = WhatsAppShareHelper.buildDailyMessage(
                            devoteeName = state.profile.displayName,
                            counsellorName = state.profile.counsellorName,
                            log = state.draft,
                            targets = state.profile.targets
                        )
                    }
                )

                1 -> DevoteeWeeklyTabScreen(
                    state = state,
                    onPreviousWeek = { devoteeVm.shiftSelectedWeek(-1) },
                    onNextWeek = { devoteeVm.shiftSelectedWeek(+1) },
                    onRefreshCloud = { devoteeVm.refreshFromCloud(silent = false) },
                    onOpenWhatsAppWeekly = {
                        whatsAppDraftMessage = WhatsAppShareHelper.buildWeeklyMessage(
                            devoteeName = state.profile.displayName,
                            counsellorName = state.profile.counsellorName,
                            summary = state.selectedWeekSummary
                        )
                    }
                )

                2 -> DevoteeHistoryScreen(
                    state = state,
                    onSelectWeekAndOpenCard = { weekKey ->
                        devoteeVm.selectWeek(weekKey)
                        selectedTab = 1
                    },
                    onRefreshCloud = { devoteeVm.refreshFromCloud(silent = false) }
                )

                3 -> DevoteeProfileScreen(
                    state = state,
                    busy = sessionBusy,
                    onLinkCounsellor = onLinkCounsellor,
                    onSaveReminders = devoteeVm::updateReminderSettings,
                    onSyncNow = devoteeVm::syncPendingNow,
                    onRefreshCloud = { devoteeVm.refreshFromCloud(silent = false) },
                    onSignOut = onSignOut
                )
            }
        }
    }

    val msgToCompose = whatsAppDraftMessage
    if (msgToCompose != null) {
        WhatsAppComposerSheet(
            counsellorName = state.profile.counsellorName,
            counsellorWhatsapp = state.profile.counsellorWhatsapp,
            initialMessage = msgToCompose,
            onDismiss = { whatsAppDraftMessage = null },
            onSend = { phone, editedMsg ->
                whatsAppDraftMessage = null
                WhatsAppShareHelper.launchWhatsAppOrShare(context, phone, editedMsg)
            }
        )
    }
}

@Composable
private fun CounsellorShell(
    profile: UserProfile,
    repository: SadhanaRepository,
    sessionBusy: Boolean,
    sessionMessage: String?,
    onClearSessionMessage: () -> Unit,
    onUpdateContact: (String, String) -> Unit,
    onSignOut: () -> Unit
) {
    val counsellorVm: CounsellorViewModel = viewModel(
        key = "counsellor_${profile.uid}",
        factory = CounsellorViewModel.Factory(profile, repository)
    )
    LaunchedEffect(profile) {
        counsellorVm.updateCounsellorProfile(profile)
    }

    val state by counsellorVm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var copiedNotice by remember { mutableStateOf(false) }

    LaunchedEffect(state.bannerMessage) {
        val msg = state.bannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            counsellorVm.clearBanner()
        }
    }
    LaunchedEffect(sessionMessage) {
        if (!sessionMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(sessionMessage)
            onClearSessionMessage()
        }
    }
    LaunchedEffect(copiedNotice) {
        if (copiedNotice) {
            snackbarHostState.showSnackbar("Invite code ${profile.inviteCode} copied!")
            copiedNotice = false
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state.selectedDevoteeUid == null) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(if (selectedTab == 0) Icons.Filled.Groups else Icons.Outlined.Groups, contentDescription = "Devotees") },
                        label = { Text("Devotees") },
                        modifier = Modifier.testTag("nav_counsellor_devotees")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(if (selectedTab == 1) Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        modifier = Modifier.testTag("nav_counsellor_profile")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (state.selectedDevoteeUid != null) {
                CounsellorDevoteeDetailScreen(
                    state = state,
                    onBack = { counsellorVm.selectDevotee(null) },
                    onRefresh = { counsellorVm.refreshRoster(silent = false) },
                    onPreviousWeek = { counsellorVm.shiftSelectedWeek(-1) },
                    onNextWeek = { counsellorVm.shiftSelectedWeek(+1) },
                    onSaveTargets = counsellorVm::saveDevoteeTargets,
                    onSaveFeedback = counsellorVm::saveWeeklyFeedback
                )
            } else {
                when (selectedTab) {
                    0 -> CounsellorDashboardScreen(
                        state = state,
                        onRefresh = { counsellorVm.refreshRoster(silent = false) },
                        onSelectDevotee = { uid -> counsellorVm.selectDevotee(uid) },
                        onCopiedInviteCode = { copiedNotice = true }
                    )

                    1 -> CounsellorProfileScreen(
                        profile = state.counsellorProfile,
                        busy = sessionBusy,
                        onSaveContact = onUpdateContact,
                        onCopiedInviteCode = { copiedNotice = true },
                        onSignOut = onSignOut
                    )
                }
            }
        }
    }
}
