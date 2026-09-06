package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.example.telecom.RoleHelper
import com.example.ui.MainViewModel
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        com.example.telecom.FlipToShhhManager.initialize(this)

        val tabExtra = intent.getIntExtra("EXTRA_INITIAL_TAB", 0)
        handleDialIntent(intent)

        setContent {
            MyApplicationTheme {
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDialIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDefaultDialerStatus()
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
    val spamNumbers by viewModel.spamNumbers.collectAsStateWithLifecycle()
    val automationLogs by viewModel.automationLogs.collectAsStateWithLifecycle()
    val selectedSimSlot by viewModel.selectedSimSlot.collectAsStateWithLifecycle()
    val activeSims by viewModel.activeSims.collectAsStateWithLifecycle()
    val pendingCloudConfirmation by viewModel.pendingCloudConfirmation.collectAsStateWithLifecycle()

    val isFlipToShhhEnabled by viewModel.isFlipToShhhEnabled.collectAsStateWithLifecycle()
    val isShhhActive by viewModel.isShhhActive.collectAsStateWithLifecycle()
    val isCallScreenMinimized by viewModel.isCallScreenMinimized.collectAsStateWithLifecycle()

    LaunchedEffect(activeCall) {
        if (activeCall == null) {
            viewModel.maximizeCall()
        }
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
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = "Kishan Dialer",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Minimized Ongoing Call Timer Banner (Simulating native dialer ongoing call state inside the app)
                    AnimatedVisibility(
                        visible = activeCall != null && isCallScreenMinimized && activeCall?.state != android.telecom.Call.STATE_DISCONNECTED,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
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

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.maximizeCall() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("minimized_ongoing_call_banner"),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF16A34A) // Standard Dialer green color
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Ongoing call: ${activeCall?.displayName ?: "Unknown"}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Text(
                                    text = timerText,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            },
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
                        onSelectNumber = { num ->
                            viewModel.setDialerNumber(num)
                            selectedTab = 2
                        },
                        onCallNumber = { num ->
                            viewModel.setDialerNumber(num)
                            viewModel.placeCall(context, num)
                        },
                        onCreateRule = { num ->
                            ruleNumberToCreate = num
                            selectedTab = 4
                        },
                        onDeleteFavorite = { fav -> viewModel.deleteFavorite(fav) },
                        onAddFavorite = { name, num, label, photoUri ->
                            viewModel.addFavorite(name, num, label, photoUri)
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
                        isFlipToShhhEnabled = isFlipToShhhEnabled,
                        isShhhActive = isShhhActive,
                        onToggleFlipToShhh = { viewModel.toggleFlipToShhh() }
                    )
                    1 -> CallLogScreen(
                        recentCalls = recentCalls,
                        spamNumbers = spamNumbers,
                        favorites = favorites,
                        onCallBack = { num ->
                            viewModel.setDialerNumber(num)
                            selectedTab = 2
                            viewModel.placeCall(context, num)
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
                        onSimulateCall = { num, name -> viewModel.simulateIncomingCall(context, num, name) },
                        onCreateRuleForNumber = { num ->
                            ruleNumberToCreate = num
                            selectedTab = 4
                        },
                        onAddFavorite = { name, num, label, photoUri ->
                            viewModel.addFavorite(name, num, label, photoUri)
                        },
                        onDeleteFavorite = { fav ->
                            viewModel.deleteFavorite(fav)
                        }
                    )
                    3 -> ContactsScreen(
                        favorites = favorites,
                        onSelectNumber = { num ->
                            viewModel.setDialerNumber(num)
                            selectedTab = 2
                        },
                        onCallNumber = { num ->
                            viewModel.setDialerNumber(num)
                            viewModel.placeCall(context, num)
                        },
                        onCreateRule = { num ->
                            ruleNumberToCreate = num
                            selectedTab = 4
                        },
                        onToggleFavorite = { name, num, label, photoUri ->
                            viewModel.toggleFavorite(name, num, label, photoUri)
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
                                saveToDevice = destination == com.example.ui.components.ContactSaveDestination.GOOGLE_DEVICE,
                                addToFavorites = addToFavs
                            )
                        },
                        onSyncContactToGoogle = { contact ->
                            viewModel.syncAppContactToGoogle(contact)
                        }
                    )
                    4 -> RulesScreen(
                        rules = rules,
                        automationLogs = automationLogs,
                        favorites = favorites,
                        onToggleRule = { viewModel.toggleRuleEnabled(it) },
                        onSaveRule = { viewModel.saveRule(it) },
                        onDeleteRule = { viewModel.deleteRule(it) },
                        onClearLogs = { viewModel.clearLogs() },
                        initiallyShowAddRuleWithNumber = ruleNumberToCreate,
                        onConsumeAddRuleNumber = { ruleNumberToCreate = null }
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
                    onDismiss = { viewModel.minimizeCall() }
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
