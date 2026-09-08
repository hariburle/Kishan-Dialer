package com.example

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.example.telecom.ActiveCallInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.example.telecom.CallManager
import com.example.telecom.RoleHelper
import com.example.ui.MainViewModel
import com.example.ui.components.ContactSaveDestination
import com.example.ui.components.WhatsAppIcon
import com.example.ui.screens.CallLogScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DialerScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.InCallScreen
import com.example.ui.screens.RulesScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.provideFactory(this)
    }

    private var isInPipMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        CallManager.init(applicationContext)
        com.example.telecom.FlipToShhhManager.initialize(this)

        val tabExtra = intent.getIntExtra("EXTRA_INITIAL_TAB", 0)
        handleDialIntent(intent)
        if (intent.getBooleanExtra("EXTRA_IN_CALL", false)) {
            viewModel.maximizeCall()
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = darkTheme) {
                if (isInPipMode) {
                    PipCallContent(viewModel = viewModel)
                } else {
                    MainAppContent(
                        viewModel = viewModel,
                        initialTab = tabExtra,
                        onOpenDialNumber = { number ->
                            viewModel.setDialerNumber(number)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDialIntent(intent)
        if (intent.getBooleanExtra("EXTRA_IN_CALL", false)) {
            viewModel.maximizeCall()
        }
    }

    override fun onResume() {
        super.onResume()
        CallManager.isCallUiForegrounded = true
        com.example.telecom.OngoingCallNotificationHelper.cancelCallNotification(this)
        viewModel.refreshDefaultDialerStatus()
        viewModel.refreshContacts()
    }

    override fun onPause() {
        super.onPause()
        CallManager.isCallUiForegrounded = false
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val active = CallManager.activeCall.value
        if (active != null && active.state == android.telecom.Call.STATE_ACTIVE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    enterPictureInPictureMode(
                        PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(16, 9))
                            .build()
                    )
                } catch (e: Exception) {
                    Log.w("MainActivity", "Failed to enter PiP mode", e)
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    private fun handleDialIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val data: Uri? = intent.data

        if (action == Intent.ACTION_DIAL || action == Intent.ACTION_VIEW || action == Intent.ACTION_CALL) {
            data?.schemeSpecificPart?.let { rawNumber ->
                viewModel.setDialerNumber(rawNumber)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    initialTab: Int = 0,
    onOpenDialNumber: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    var ruleNumberToCreate by remember { mutableStateOf<String?>(null) }

    // Collect States
    val dialerNumber by viewModel.dialerNumber.collectAsStateWithLifecycle()
    val isDefaultDialer by viewModel.isDefaultDialer.collectAsStateWithLifecycle()
    val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsStateWithLifecycle()
    val automationStep by viewModel.automationState.collectAsStateWithLifecycle()
    val lastDtmfKey by viewModel.lastDtmfKey.collectAsStateWithLifecycle()
    val showInCallKeypad by viewModel.showInCallKeypad.collectAsStateWithLifecycle()
    val currentAudioRoute by viewModel.currentAudioRoute.collectAsStateWithLifecycle()
    val supportedAudioRoutes by viewModel.supportedAudioRoutes.collectAsStateWithLifecycle()
    val bluetoothDeviceName by viewModel.bluetoothDeviceName.collectAsStateWithLifecycle()

    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val recentCalls by viewModel.recentCalls.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val ignoredContacts by viewModel.ignoredContacts.collectAsStateWithLifecycle()
    val spamNumbers by viewModel.spamNumbers.collectAsStateWithLifecycle()
    val automationLogs by viewModel.automationLogs.collectAsStateWithLifecycle()
    val selectedSimSlot by viewModel.selectedSimSlot.collectAsStateWithLifecycle()
    val activeSims by viewModel.activeSims.collectAsStateWithLifecycle()
    val deviceContacts by viewModel.deviceContacts.collectAsStateWithLifecycle()
    val pendingCloudConfirmation by viewModel.pendingCloudConfirmation.collectAsStateWithLifecycle()
    val pendingCallMethodChoice by viewModel.pendingCallMethodChoice.collectAsStateWithLifecycle()

    val isFlipToShhhEnabled by viewModel.isFlipToShhhEnabled.collectAsStateWithLifecycle()
    val isShhhActive by viewModel.isShhhActive.collectAsStateWithLifecycle()
    val isCallScreenMinimized by viewModel.isCallScreenMinimized.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val whatsAppCallMode by viewModel.whatsAppCallMode.collectAsStateWithLifecycle()
    val learnedCallModes by viewModel.learnedCallModes.collectAsStateWithLifecycle()
    val defaultStartTab by viewModel.defaultStartTab.collectAsStateWithLifecycle()
    val confirmFavoritesCall by viewModel.confirmFavoritesCall.collectAsStateWithLifecycle()

    var hasAppliedDefaultTab by remember { mutableStateOf(false) }
    LaunchedEffect(defaultStartTab) {
        if (!hasAppliedDefaultTab && initialTab == 0) {
            hasAppliedDefaultTab = true
            selectedTab = defaultStartTab.coerceIn(0, 4)
        }
    }

    LaunchedEffect(activeCall?.id) {
        // Whenever a call is initiated or incoming, always ensure call screen is maximized
        viewModel.maximizeCall()
    }

    BackHandler(enabled = activeCall != null && !isCallScreenMinimized) {
        viewModel.minimizeCall()
    }

    var showDefaultAppPrompt by remember { mutableStateOf(true) }

    val defaultDialerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshDefaultDialerStatus()
        viewModel.refreshSimCards()
    }

    // Request necessary runtime permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshDefaultDialerStatus()
        viewModel.refreshSimCards()
        viewModel.syncWithDeviceContacts()
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.SEND_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val ungranted = permissions.filter {
            context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Star, contentDescription = "Favorites") },
                        label = { Text("Favorites") },
                        modifier = Modifier.testTag("nav_favorites")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.History, contentDescription = "Recents") },
                        label = { Text("Recents") },
                        modifier = Modifier.testTag("nav_recents")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Dialpad, contentDescription = "Keypad") },
                        label = { Text("Keypad") },
                        modifier = Modifier.testTag("nav_keypad")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Contacts, contentDescription = "Contacts") },
                        label = { Text("Contacts") },
                        modifier = Modifier.testTag("nav_contacts")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = {
                            ruleNumberToCreate = null
                            selectedTab = 4
                        },
                        icon = { Icon(Icons.Default.SmartToy, contentDescription = "Rules") },
                        label = { Text("Rules") },
                        modifier = Modifier.testTag("nav_rules")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> FavoritesScreen(
                        favorites = favorites,
                        recentCalls = recentCalls,
                        ignoredContacts = ignoredContacts,
                        confirmFavoritesCall = confirmFavoritesCall,
                        getPreferredCallingMode = { num -> viewModel.getPreferredCallingMode(num) },
                        onSelectNumber = { num ->
                            viewModel.setDialerNumber(num)
                            selectedTab = 2
                        },
                        onCallNumber = { num ->
                            viewModel.initiateCall(context, num)
                        },
                        onCallWhatsApp = { num ->
                            viewModel.placeWhatsAppCall(context, num)
                        },
                        onCreateRule = { num ->
                            ruleNumberToCreate = num
                            selectedTab = 4
                        },
                        onDeleteFavorite = { fav -> viewModel.deleteFavorite(fav) },
                        onAddFavorite = { name, num, label, photoUri ->
                            viewModel.addFavorite(name, num, label, photoUri)
                        },
                        onAddNewContact = { name, number, label, destination, addToFavorites ->
                            val saveToDevice = (destination == ContactSaveDestination.PHONE_CONTACTS)
                            viewModel.createNewContact(name, number, label, saveToDevice, addToFavorites)
                        },
                        onAssignSpeedDial = { contact, slot ->
                            viewModel.assignSpeedDial(contact, slot)
                        },
                        onMoveFavorite = { fromIndex, toIndex ->
                            viewModel.moveFavorite(fromIndex, toIndex)
                        },
                        onEditFavorite = { contact, newNickname ->
                            viewModel.updateFavorite(contact, newNickname)
                        },
                        onUpdateFavoriteNumber = { contact, newNum, newLabel ->
                            viewModel.updateFavoritePhoneNumber(contact, newNum, newLabel)
                        },
                        onIgnoreContact = { num, name, cat, tag ->
                            viewModel.ignorePopularContact(num, name, cat, tag)
                        },
                        onUnignoreContact = { num ->
                            viewModel.unignorePopularContact(num)
                        },
                        onUpdateIgnoredContactTag = { num, tag, name ->
                            viewModel.updateIgnoredContactTag(num, tag, name)
                        },
                        isFlipToShhhEnabled = isFlipToShhhEnabled,
                        isShhhActive = isShhhActive,
                        onToggleFlipToShhh = { viewModel.toggleFlipToShhh() },
                        deviceContacts = deviceContacts
                    )
                    1 -> CallLogScreen(
                        recentCalls = recentCalls,
                        spamNumbers = spamNumbers,
                        favorites = favorites,
                        isSpamNumber = { num -> viewModel.isSpamNumber(num) },
                        onCallBack = { num ->
                            viewModel.initiateCall(context, num)
                        },
                        onCreateRuleForNumber = { num ->
                            ruleNumberToCreate = num
                            selectedTab = 4
                        },
                        onMarkSpam = { num -> viewModel.markAsSpam(num) },
                        onRemoveSpam = { num -> viewModel.removeSpam(num) },
                        onToggleFavorite = { name, num, label, photoUri ->
                            viewModel.toggleFavorite(name, num, label, photoUri)
                        },
                        onUpdateNoteAndReminder = { call, note, rem ->
                            viewModel.updateRecentCallNoteAndReminder(call, note, rem)
                        },
                        onUpdateContact = { oldNum, name, number, label, nickname ->
                            viewModel.updateContact(oldNum, name, number, label, nickname)
                        },
                        onDeleteCall = { call ->
                            viewModel.deleteRecentCall(call)
                        },
                        onDeleteCallsForNumber = { phoneNumber ->
                            viewModel.deleteRecentCallsForNumber(phoneNumber)
                        }
                    )
                    2 -> DialerScreen(
                        number = dialerNumber,
                        favorites = favorites,
                        recentCalls = recentCalls,
                        isDefaultDialer = isDefaultDialer,
                        context = context,
                        simSlot = selectedSimSlot,
                        activeSims = activeSims,
                        onToggleSim = { viewModel.toggleSimSlot() },
                        onRoleChanged = { viewModel.refreshDefaultDialerStatus() },
                        onDigitPress = { viewModel.appendDigit(it) },
                        onDeleteDigit = { viewModel.deleteLastDigit() },
                        onClearDigits = { viewModel.clearDigits() },
                        onSelectContactNumber = { num -> viewModel.setDialerNumber(num) },
                        onPlaceCall = { num, reason -> viewModel.placeCall(context, num, reason) },
                        onPlaceWhatsAppCall = { num -> viewModel.placeWhatsAppCall(context, num) },
                        onSimulateCall = { num, name -> viewModel.simulateIncomingCall(context, num, name) },
                        onCreateRuleForNumber = { num ->
                            ruleNumberToCreate = num
                            selectedTab = 4
                        },
                        onAddFavorite = { name, num, label, photoUri ->
                            viewModel.addFavorite(name, num, label, photoUri)
                        },
                        onAddNewContact = { name, number, label, destination, addToFavorites ->
                            val saveToDevice = (destination == ContactSaveDestination.PHONE_CONTACTS)
                            viewModel.createNewContact(name, number, label, saveToDevice, addToFavorites)
                        },
                        onDeleteFavorite = { fav ->
                            viewModel.deleteFavorite(fav)
                        },
                        onAssignSpeedDialSlot = { slot, name, num, photoUri ->
                            viewModel.assignSpeedDialSlot(slot, name, num, photoUri)
                        },
                        onClearSpeedDialSlot = { slot ->
                            viewModel.clearSpeedDialSlot(slot)
                        },
                        deviceContacts = deviceContacts
                    )
                    3 -> ContactsScreen(
                        favorites = favorites,
                        deviceContacts = deviceContacts,
                        onRefreshContacts = { viewModel.refreshContacts() },
                        onPlaceWhatsAppCall = { num -> viewModel.placeWhatsAppCall(context, num) },
                        onSelectNumber = { num ->
                            viewModel.setDialerNumber(num)
                            selectedTab = 2
                        },
                        onCallNumber = { num ->
                            viewModel.initiateCall(context, num)
                        },
                        onCreateRule = { num ->
                            ruleNumberToCreate = num
                            selectedTab = 4
                        },
                        onToggleFavorite = { name, num, label, photoUri ->
                            viewModel.toggleFavorite(name, num, label, photoUri)
                        },
                        onDeleteFavorite = { fav ->
                            viewModel.deleteFavorite(fav)
                        },
                        onAddFavorite = { name, num, label, photoUri ->
                            viewModel.addFavorite(name, num, label, photoUri)
                        },
                        onUpdateFavoriteNumber = { contact, newNum, newLabel ->
                            viewModel.updateFavoritePhoneNumber(contact, newNum, newLabel)
                        },
                        onAddNewContact = { name, num, label, destination, addToFavs ->
                            viewModel.createNewContact(
                                name = name,
                                phoneNumber = num,
                                label = label,
                                saveToDevice = destination == ContactSaveDestination.PHONE_CONTACTS,
                                addToFavorites = addToFavs
                            )
                        },
                        onSyncContactToPhone = { contact ->
                            viewModel.syncAppContactToPhone(contact)
                        },
                        onSyncAllAppContactsToDevice = {
                            viewModel.syncAllAppContactsToDevice()
                        }
                    )
                    4 -> RulesScreen(
                        rules = rules,
                        automationLogs = automationLogs,
                        favorites = favorites,
                        themeMode = themeMode,
                        onSetThemeMode = { viewModel.setThemeMode(it) },
                        whatsAppCallMode = whatsAppCallMode,
                        onSetWhatsAppCallMode = { viewModel.setWhatsAppCallMode(it) },
                        onResetWhatsAppChoices = { viewModel.resetWhatsAppChoices() },
                        learnedChoicesCount = learnedCallModes.size,
                        spamNumbers = spamNumbers,
                        onRemoveSpam = { viewModel.removeSpam(it) },
                        confirmFavoritesCall = confirmFavoritesCall,
                        onSetConfirmFavoritesCall = { viewModel.setConfirmFavoritesCall(it) },
                        defaultStartTab = defaultStartTab,
                        onSetDefaultStartTab = { viewModel.setDefaultStartTab(it) },
                        onToggleRule = { viewModel.toggleRuleEnabled(it) },
                        onSaveRule = { viewModel.saveRule(it) },
                        onDeleteRule = { viewModel.deleteRule(it) },
                        onClearLogs = { viewModel.clearLogs() },
                        initiallyShowAddRuleWithNumber = ruleNumberToCreate,
                        onConsumeAddRuleNumber = { ruleNumberToCreate = null },
                        deviceContacts = deviceContacts
                    )
                }
            }
        }

        // Active In-Call Overlay Screen (Appears seamlessly over UI whenever call is active/ringing)
        AnimatedVisibility(
            visible = activeCall != null && !isCallScreenMinimized,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            activeCall?.let { call ->
                InCallScreen(
                    callInfo = call,
                    isMuted = isMuted,
                    isSpeakerOn = isSpeakerOn,
                    automationStep = automationStep,
                    lastDtmfKey = lastDtmfKey,
                    showKeypad = showInCallKeypad,
                    onToggleKeypad = { viewModel.toggleInCallKeypad() },
                    onAnswer = { viewModel.answerCall() },
                    onDecline = { viewModel.declineCall() },
                    onDisconnect = { viewModel.disconnectCall() },
                    onToggleMute = { viewModel.toggleMute() },
                    onToggleSpeaker = { viewModel.toggleSpeaker() },
                    audioRoute = currentAudioRoute,
                    supportedAudioRoutes = supportedAudioRoutes,
                    bluetoothDeviceName = bluetoothDeviceName,
                    onSelectAudioRoute = { route -> viewModel.setAudioRoute(route) },
                    onPlayDtmf = { viewModel.playDtmf(it) },
                    onStopDtmf = { viewModel.stopDtmf() },
                    onDeclineWithSms = { msg -> viewModel.declineWithSms(msg) },
                    onSavePostCallNote = { note, reminderMinutes ->
                        val reminderTime = reminderMinutes?.let { System.currentTimeMillis() + it * 60 * 1000 }
                        viewModel.savePostCallNote(call.phoneNumber, note, reminderTime)
                    },
                    onMarkSpam = { num -> viewModel.markAsSpam(num) },
                    onDismiss = { viewModel.minimizeCall() },
                    onClosePostCall = { viewModel.dismissCall() }
                )
            }
        }

        // Floating Green In-Call Progress Pill (Appears when user minimizes in-call screen or navigates app during active call)
        AnimatedVisibility(
            visible = activeCall != null &&
                    activeCall?.state != Call.STATE_DISCONNECTED &&
                    activeCall?.state != Call.STATE_DISCONNECTING &&
                    isCallScreenMinimized,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            activeCall?.let { call ->
                FloatingCallPill(
                    callInfo = call,
                    onMaximize = { viewModel.maximizeCall() },
                    onDisconnect = { viewModel.disconnectCall() }
                )
            }
        }

        // Explicit Confirmation Dialog before making any changes to Google Account Contacts in the Cloud
        pendingCloudConfirmation?.let { conf ->
            AlertDialog(
                onDismissRequest = {
                    conf.onDismissOrCancel()
                    viewModel.clearCloudConfirmation()
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = conf.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Text(
                        text = conf.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            conf.onConfirmCloudAction()
                            viewModel.clearCloudConfirmation()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(conf.confirmButtonText)
                    }
                },
                dismissButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        if (conf.secondaryButtonText != null && conf.onSecondaryAction != null) {
                            FilledTonalButton(
                                onClick = {
                                    conf.onSecondaryAction.invoke()
                                    viewModel.clearCloudConfirmation()
                                }
                            ) {
                                Text(conf.secondaryButtonText)
                            }
                        }
                        TextButton(
                            onClick = {
                                conf.onDismissOrCancel()
                                viewModel.clearCloudConfirmation()
                            }
                        ) {
                            Text(conf.dismissButtonText)
                        }
                    }
                }
            )
        }

        // WhatsApp vs Cellular Call Choice Dialog (Ask Always & Ask and Learn)
        pendingCallMethodChoice?.let { prompt ->
            var rememberChoice by remember(prompt) { mutableStateOf(prompt.isLearnMode) }
            AlertDialog(
                onDismissRequest = { viewModel.dismissCallMethodChoice() },
                title = {
                    Text(
                        text = "Choose Calling Method",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column {
                            if (!prompt.contactName.isNullOrBlank()) {
                                Text(
                                    text = prompt.contactName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = prompt.number,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.chooseCallMethod(context, "cellular", rememberChoice)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                            ) {
                                Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Cellular")
                            }

                            Button(
                                onClick = {
                                    viewModel.chooseCallMethod(context, "whatsapp", rememberChoice)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                            ) {
                                WhatsAppIcon(modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("WhatsApp", color = Color.White)
                            }
                        }

                        if (prompt.isLearnMode) {
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                modifier = Modifier.clickable { rememberChoice = !rememberChoice }
                            ) {
                                Checkbox(
                                    checked = rememberChoice,
                                    onCheckedChange = { rememberChoice = it }
                                )
                                Text(
                                    text = "Remember choice for this contact",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissCallMethodChoice() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Check if Kishan Dialer is the default app on startup, and prompt user if not
        if (!isDefaultDialer && showDefaultAppPrompt) {
            AlertDialog(
                onDismissRequest = {
                    showDefaultAppPrompt = false
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Set as Default Phone App",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Text(
                        text = "Kishan Dialer is not your default phone app. To answer calls, screen spam, and use speed dials seamlessly, please set Kishan Dialer as your default app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val intent = RoleHelper.createDefaultDialerIntent(context)
                            if (intent != null) {
                                defaultDialerLauncher.launch(intent)
                            }
                            showDefaultAppPrompt = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Set as Default")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDefaultAppPrompt = false
                        }
                    ) {
                        Text("Later")
                    }
                }
            )
        }
    }
}

@Composable
private fun FloatingCallPill(
    callInfo: ActiveCallInfo,
    onMaximize: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var elapsedSeconds by remember { mutableStateOf(0L) }

    LaunchedEffect(callInfo.connectTimeMillis) {
        val connectTime = callInfo.connectTimeMillis
        if (connectTime > 0L) {
            while (true) {
                elapsedSeconds = ((System.currentTimeMillis() - connectTime) / 1000).coerceAtLeast(0L)
                delay(1000)
            }
        } else {
            elapsedSeconds = 0L
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timerText = if (callInfo.connectTimeMillis > 0L) String.format("%02d:%02d", minutes, seconds) else "In Call..."

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF16A34A),
        contentColor = Color.White,
        shadowElevation = 8.dp,
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 8.dp, start = 16.dp, end = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable { onMaximize() }
            .testTag("floating_call_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Active Call",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )

            Column {
                Text(
                    text = callInfo.displayName ?: callInfo.phoneNumber,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = timerText,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDisconnect,
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFDC2626), CircleShape)
                    .testTag("floating_pill_hangup_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Clean, compact Picture-in-Picture call banner showing active call duration,
 * caller name, and immediate hangup control when user moves to another app.
 */
@Composable
fun PipCallContent(viewModel: MainViewModel) {
    val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()
    var elapsedSeconds by remember { mutableStateOf(0L) }

    LaunchedEffect(activeCall?.connectTimeMillis) {
        val connectTime = activeCall?.connectTimeMillis ?: 0L
        if (connectTime > 0L) {
            while (true) {
                elapsedSeconds = (System.currentTimeMillis() - connectTime) / 1000
                delay(1000)
            }
        } else {
            elapsedSeconds = 0L
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timerText = String.format("%02d:%02d", minutes, seconds)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pip_call_container"),
        color = Color(0xFF16A34A)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = activeCall?.displayName ?: "Ongoing call",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                    Text(
                        text = timerText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(
                onClick = { viewModel.disconnectCall() },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("pip_hangup_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Hang up",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
