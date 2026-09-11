package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneForwarded
import androidx.compose.material.icons.filled.SmartToy
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Settings
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
import com.example.data.AutomationLog
import com.example.data.CallerRule
import com.example.data.FavoriteContact
import com.example.data.SpamNumber
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.ContactPickerDialog
import com.example.util.DeviceContact
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RulesScreen(
    rules: List<CallerRule>,
    automationLogs: List<AutomationLog>,
    favorites: List<FavoriteContact> = emptyList(),
    themeMode: String = "system",
    onSetThemeMode: (String) -> Unit = {},
    favoriteCardStyle: String = "bento",
    onSetFavoriteCardStyle: (String) -> Unit = {},
    whatsAppCallMode: String = "ask_learn",
    onSetWhatsAppCallMode: (String) -> Unit = {},
    onResetWhatsAppChoices: () -> Unit = {},
    learnedChoicesCount: Int = 0,
    spamNumbers: List<SpamNumber> = emptyList(),
    onAddSpam: (String, String) -> Unit = { _, _ -> },
    onRemoveSpam: (String) -> Unit = {},
    confirmFavoritesCall: Boolean = false,
    onSetConfirmFavoritesCall: (Boolean) -> Unit = {},
    defaultStartTab: Int = 0,
    onSetDefaultStartTab: (Int) -> Unit = {},
    swipeToSwitchPanels: Boolean = true,
    onSetSwipeToSwitchPanels: (Boolean) -> Unit = {},
    callAnswerStyle: String = "swipe_up",
    onSetCallAnswerStyle: (String) -> Unit = {},
    onToggleRule: (CallerRule) -> Unit,
    onSaveRule: (CallerRule) -> Unit,
    onDeleteRule: (CallerRule) -> Unit,
    onClearLogs: () -> Unit,
    initiallyShowAddRuleWithNumber: String? = null,
    onConsumeAddRuleNumber: () -> Unit = {},
    deviceContacts: List<DeviceContact> = emptyList(),
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showHistoryDialog by rememberSaveable { mutableStateOf(false) }
    var showSpamDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<CallerRule?>(null) }

    LaunchedEffect(initiallyShowAddRuleWithNumber) {
        if (!initiallyShowAddRuleWithNumber.isNullOrBlank()) {
            editingRule = CallerRule(
                name = "Custom Rule",
                phoneNumberPattern = initiallyShowAddRuleWithNumber,
                isEnabled = true,
                autoAnswer = true,
                answerDelaySec = 1,
                dtmfSequence = "9#",
                dtmfDelayMs = 800,
                sendSms = false,
                smsMessage = "",
                autoHangup = true,
                hangupDelaySec = 2
            )
            showDialog = true
            onConsumeAddRuleNumber()
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val subPagerState = rememberPagerState(initialPage = selectedTab.coerceIn(0, 1)) { 2 }

    LaunchedEffect(subPagerState.currentPage) {
        if (selectedTab != subPagerState.currentPage) {
            selectedTab = subPagerState.currentPage
        }
    }

    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().testTag("rules_screen")) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        coroutineScope.launch { subPagerState.animateScrollToPage(0) }
                    },
                    text = { Text("Caller Rules (${rules.size})") },
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        coroutineScope.launch { subPagerState.animateScrollToPage(1) }
                    },
                    text = { Text("Settings") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                )
            }

            HorizontalPager(
                state = subPagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page == 0) {
                    // Rules Tab Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        if (rules.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "No Automation Rules Created Yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Create rules to auto-answer intercoms, dial DTMF extension codes, or auto-reply with SMS.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = {
                                            editingRule = CallerRule(
                                                name = "New Automation Rule",
                                                phoneNumberPattern = "",
                                                isEnabled = true,
                                                autoAnswer = true,
                                                answerDelaySec = 1,
                                                dtmfSequence = "9#",
                                                dtmfDelayMs = 800,
                                                sendSms = false,
                                                smsMessage = "Automated reply sent.",
                                                autoHangup = true,
                                                hangupDelaySec = 2
                                            )
                                            showDialog = true
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Create First Rule", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(rules, key = { it.id }) { rule ->
                            RuleCard(
                                rule = rule,
                                automationLogs = automationLogs,
                                onToggle = { onToggleRule(rule) },
                                onEdit = {
                                    editingRule = rule
                                    showDialog = true
                                },
                                onDelete = { onDeleteRule(rule) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
            } else {
                // Settings Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(text = "Theme Mode", fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (mode, label) ->
                                    val selected = themeMode == mode
                                    AssistChip(
                                        onClick = { onSetThemeMode(mode) },
                                        label = { Text(label) },
                                        leadingIcon = if (selected) {
                                            { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Favorite Contact Card Style",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Customize the visual appearance and layout of contact cards in Favorites",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val styles = listOf(
                                Triple(
                                    "bento",
                                    "Bento",
                                    "Modern rounded container with full-width action pill, squircle avatar, and clean outlines."
                                ),
                                Triple(
                                    "quick_action",
                                    "Grid",
                                    "Compact card with prominent circular call button and traditional rounded avatar."
                                ),
                                Triple(
                                    "material_you",
                                    "Material",
                                    "Dynamic rounded corners with tonal outline and chip action button."
                                )
                            )

                            styles.forEach { (styleKey, title, description) ->
                                val isSelected = (favoriteCardStyle == styleKey)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
                                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSetFavoriteCardStyle(styleKey) }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { onSetFavoriteCardStyle(styleKey) },
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        // Realistic visual preview of the favorite contact card
                                        Text(
                                            text = "Sample Preview:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        when (styleKey) {
                                            "bento" -> {
                                                Card(
                                                    shape = RoundedCornerShape(18.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Surface(
                                                                shape = RoundedCornerShape(12.dp),
                                                                color = Color(0xFF3B82F6),
                                                                modifier = Modifier.size(40.dp)
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.width(10.dp))
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text("Alex Morgan", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                                Text("+1 (555) 234-5678", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                                                Text("Mobile • Work", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.5.sp)
                                                            }
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = MaterialTheme.colorScheme.primaryContainer
                                                            ) {
                                                                Text("#1", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                                            }
                                                        }
                                                        Surface(
                                                            shape = RoundedCornerShape(10.dp),
                                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                                            modifier = Modifier.fillMaxWidth().height(26.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxSize(),
                                                                horizontalArrangement = Arrangement.Center,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(Icons.Default.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text("Direct Call", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            "quick_action" -> {
                                                Card(
                                                    shape = RoundedCornerShape(14.dp),
                                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Surface(
                                                                shape = CircleShape,
                                                                color = Color(0xFF10B981),
                                                                modifier = Modifier.size(38.dp)
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.width(10.dp))
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text("Alex Morgan", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                                Text("+1 (555) 234-5678", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                                                Text("Mobile • Work", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.5.sp)
                                                            }
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = MaterialTheme.colorScheme.primaryContainer
                                                            ) {
                                                                Text("#1", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                                            }
                                                        }
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.End,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Surface(
                                                                shape = CircleShape,
                                                                color = Color(0xFF16A34A),
                                                                modifier = Modifier.size(28.dp)
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            "material_you" -> {
                                                Card(
                                                    shape = RoundedCornerShape(22.dp),
                                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Surface(
                                                                shape = RoundedCornerShape(14.dp),
                                                                color = Color(0xFF8B5CF6),
                                                                modifier = Modifier.size(38.dp)
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.width(10.dp))
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text("Alex Morgan", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                                Text("+1 (555) 234-5678", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                                                Text("Mobile • Work", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.5.sp)
                                                            }
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = MaterialTheme.colorScheme.primaryContainer
                                                            ) {
                                                                Text("#1", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                                            }
                                                        }
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.End,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Surface(
                                                                shape = RoundedCornerShape(14.dp),
                                                                color = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.height(26.dp)
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 10.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                ) {
                                                                    Icon(Icons.Default.Call, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
                                                                    Text("Call", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "WhatsApp Audio Calls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(text = "WhatsApp Call Integration Mode", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            val options = listOf(
                                Triple("ask_learn", "Ask & Learn", "Prompts once per contact and memorizes choice"),
                                Triple("ask_always", "Ask Always", "Always shows Cellular vs WhatsApp choice on call"),
                                Triple("all_international", "International Numbers", "Directs numbers outside your country (+1 for US, +91 for India, etc.) to WhatsApp automatically."),
                                Triple("never", "Never", "Default standard cellular calls only")
                            )
                            options.forEach { (mode, label, desc) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSetWhatsAppCallMode(mode) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = whatsAppCallMode == mode,
                                        onClick = { onSetWhatsAppCallMode(mode) },
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Column {
                                        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            if (whatsAppCallMode == "ask_learn") {
                                Text(
                                    text = "$learnedChoicesCount contact choice(s) remembered",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            OutlinedButton(
                                onClick = { showResetConfirmDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Reset Choices and Learn Memory", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Call Protection & Start Screen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Confirm Before Calling Favorites", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "Displays a confirmation dialog to prevent accidental calls when tapping favorites",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = confirmFavoritesCall,
                                    onCheckedChange = onSetConfirmFavoritesCall,
                                    modifier = Modifier.testTag("confirm_favorites_call_switch")
                                )
                            }

                            HorizontalDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Swipe to switch panels",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Swipe horizontally across main screens (Favorites <-> Recents <-> Keypad <-> Contacts <-> Settings)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = swipeToSwitchPanels,
                                    onCheckedChange = onSetSwipeToSwitchPanels,
                                    modifier = Modifier.testTag("swipe_to_switch_panels_switch")
                                )
                            }

                            HorizontalDivider()

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Default Startup Screen", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "Choose which tab opens first when launching the dialer",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val tabs = listOf(
                                    0 to "Favorites (Default)",
                                    1 to "Recents",
                                    2 to "Keypad Dialer",
                                    3 to "Contacts"
                                )
                                tabs.forEach { (tabIndex, tabTitle) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSetDefaultStartTab(tabIndex) }
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        RadioButton(
                                            selected = defaultStartTab == tabIndex,
                                            onClick = { onSetDefaultStartTab(tabIndex) },
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(text = tabTitle, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Incoming Call Answering Style",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Choose the gesture or interaction style for incoming phone calls",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val answerStyles = listOf(
                                Triple(
                                    "swipe_up",
                                    "Swipe Up to Answer (Google Phone style)",
                                    "Swipe up to answer, swipe down to decline. Recommended standard to prevent accidental answering in pockets."
                                ),
                                Triple(
                                    "button_tap",
                                    "Press to Answer (Single Tap)",
                                    "Direct one-tap buttons for Answer and Decline. Fastest and easiest for one-handed use."
                                ),
                                Triple(
                                    "swipe_slider",
                                    "Horizontal Slide to Answer (Classic Slider)",
                                    "Slide handle to the right to answer, or slide left to decline. Classic and tactile slider interface."
                                )
                            )
                            answerStyles.forEach { (style, label, desc) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSetCallAnswerStyle(style) }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = callAnswerStyle == style,
                                        onClick = { onSetCallAnswerStyle(style) },
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Protection & Spam",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSpamDialog = true }
                            .testTag("entry_spam_management")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Spam & Blocked Calls",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (spamNumbers.isNotEmpty()) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "${spamNumbers.size} blocked",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (spamNumbers.isNotEmpty()) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Manage blocked numbers, community spam rules & auto-rejection",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open Spam Manager",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Footer credit
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Developed by Hari Burle with Google AI Studio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }

        // Bottom Action Row for Add Rule and Execution History in Rules Page (Page 0)
        if (subPagerState.currentPage == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp, end = 16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { showHistoryDialog = true },
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        text = { Text("History (${automationLogs.size})") },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    ExtendedFloatingActionButton(
                        onClick = {
                            editingRule = CallerRule(
                                name = "New Automation Rule",
                                phoneNumberPattern = "",
                                isEnabled = true,
                                autoAnswer = true,
                                answerDelaySec = 1,
                                dtmfSequence = "9#",
                                dtmfDelayMs = 800,
                                sendSms = false,
                                smsMessage = "Automated reply sent.",
                                autoHangup = true,
                                hangupDelaySec = 2
                            )
                            showDialog = true
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Create Rule", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("add_rule_fab"),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Execution History Dialog
        if (showHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showHistoryDialog = false },
                title = { Text("Execution History (${automationLogs.size})") },
                text = {
                    if (automationLogs.isEmpty()) {
                        Text("No automation history yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Recent logs", style = MaterialTheme.typography.titleSmall)
                                TextButton(onClick = onClearLogs) {
                                    Text("Clear")
                                }
                            }
                            automationLogs.forEach { log ->
                                AutomationLogItem(log = log)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHistoryDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }

    // Add / Edit Rule Dialog
    if (showDialog && editingRule != null) {
        RuleEditDialog(
            initialRule = editingRule!!,
            favorites = favorites,
            deviceContacts = deviceContacts,
            onDismiss = {
                showDialog = false
                editingRule = null
                onConsumeAddRuleNumber()
            },
            onSave = { updatedRule ->
                onSaveRule(updatedRule)
                showDialog = false
                editingRule = null
                onConsumeAddRuleNumber()
            }
        )
    }

    if (showSpamDialog) {
        com.example.ui.components.SpamManagementDialog(
            spamNumbers = spamNumbers,
            onAddSpam = onAddSpam,
            onRemoveSpam = onRemoveSpam,
            onDismiss = { showSpamDialog = false }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(
                    text = "Reset All Learned Channels?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will clear all learned Phone vs WhatsApp calling choices for all contacts ($learnedChoicesCount contacts remembered). You can set preferences per contact again at any time.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetWhatsAppChoices()
                        showResetConfirmDialog = false
                        Toast.makeText(context, "★ Reset all learned choices", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
}

@Composable
private fun RuleCard(
    rule: CallerRule,
    automationLogs: List<AutomationLog> = emptyList(),
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val runCount = automationLogs.count { it.ruleName.equals(rule.name, ignoreCase = true) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("rule_card_${rule.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.isEnabled)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = rule.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (runCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Triggered $runCount times",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Matches: ${rule.phoneNumberPattern.ifBlank { "Any Caller (*)" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = rule.isEnabled,
                        onCheckedChange = { onToggle() },
                        modifier = Modifier.testTag("rule_switch_${rule.id}")
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Rule",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            // Action feature pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (rule.autoAnswer) {
                    AssistChip(
                        onClick = onEdit,
                        label = { Text("Auto-Answer (${rule.answerDelaySec}s)", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                }
                if (rule.dtmfSequence.isNotBlank()) {
                    AssistChip(
                        onClick = onEdit,
                        label = { Text("DTMF: ${rule.dtmfSequence}", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Dialpad, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                }
                if (rule.sendSms) {
                    AssistChip(
                        onClick = onEdit,
                        label = { Text("SMS Reply", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AutomationLogItem(log: AutomationLog) {
    val timeFormat = SimpleDateFormat("MMM d, h:mm:ss a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(log.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = log.ruleName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Caller: ${log.phoneNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = log.actionsSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RuleEditDialog(
    initialRule: CallerRule,
    favorites: List<FavoriteContact> = emptyList(),
    deviceContacts: List<DeviceContact> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (CallerRule) -> Unit
) {
    var name by remember { mutableStateOf(initialRule.name) }
    var pattern by remember { mutableStateOf(initialRule.phoneNumberPattern) }
    var autoAnswer by remember { mutableStateOf(initialRule.autoAnswer) }
    var answerDelaySec by remember { mutableStateOf(initialRule.answerDelaySec.toString()) }
    var dtmfSequence by remember { mutableStateOf(initialRule.dtmfSequence) }
    var dtmfDelayMs by remember { mutableStateOf(initialRule.dtmfDelayMs.toString()) }
    var sendSms by remember { mutableStateOf(initialRule.sendSms) }
    var smsMessage by remember { mutableStateOf(initialRule.smsMessage) }
    var autoHangup by remember { mutableStateOf(initialRule.autoHangup) }
    var hangupDelaySec by remember { mutableStateOf(initialRule.hangupDelaySec.toString()) }
    var showContactPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialRule.id == 0L) "New Caller Automation Rule" else "Edit Automation Rule")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = {
                            name = "Gate Buzzer"
                            autoAnswer = true
                            answerDelaySec = "1"
                            dtmfSequence = "9#"
                            autoHangup = true
                            hangupDelaySec = "2"
                        },
                        label = { Text("Gate DTMF (9#)") }
                    )
                    AssistChip(
                        onClick = {
                            name = "IVR Office Extension"
                            autoAnswer = true
                            answerDelaySec = "2"
                            dtmfSequence = "104#"
                            autoHangup = false
                        },
                        label = { Text("Extension DTMF") }
                    )
                    AssistChip(
                        onClick = {
                            name = "Auto SMS Responder"
                            autoAnswer = false
                            sendSms = true
                            smsMessage = "I am currently busy. I will call you back shortly."
                            autoHangup = true
                            hangupDelaySec = "1"
                        },
                        label = { Text("SMS Auto-Reply") }
                    )
                    AssistChip(
                        onClick = {
                            name = "Delivery Access Auto-Grant"
                            autoAnswer = true
                            answerDelaySec = "1"
                            dtmfSequence = "4#"
                            sendSms = true
                            smsMessage = "Lobby gate opened automatically."
                            autoHangup = true
                            hangupDelaySec = "2"
                        },
                        label = { Text("Delivery Gate (4#)") }
                    )
                    AssistChip(
                        onClick = {
                            name = "Voicemail Auto PIN"
                            autoAnswer = false
                            answerDelaySec = "0"
                            dtmfSequence = "1234#"
                            sendSms = false
                            autoHangup = false
                        },
                        label = { Text("Voicemail PIN") }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rule_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = pattern,
                        onValueChange = { pattern = it },
                        label = { Text("Number Pattern (or * for any)") },
                        placeholder = { Text("e.g. 5550199 or 18005550100") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("rule_pattern_input")
                    )
                    IconButton(
                        onClick = { showContactPicker = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("rule_pick_contact_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = "Pick Contact",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto-Answer via Call.answer(0)")
                    Switch(
                        checked = autoAnswer,
                        onCheckedChange = { autoAnswer = it }
                    )
                }

                if (autoAnswer) {
                    OutlinedTextField(
                        value = answerDelaySec,
                        onValueChange = { answerDelaySec = it },
                        label = { Text("Answer Delay (seconds)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = dtmfSequence,
                    onValueChange = { dtmfSequence = it },
                    label = { Text("In-Band DTMF Key Sequence") },
                    placeholder = { Text("e.g. 9# or 104# or 1*23") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rule_dtmf_input")
                )

                if (dtmfSequence.isNotEmpty()) {
                    OutlinedTextField(
                        value = dtmfDelayMs,
                        onValueChange = { dtmfDelayMs = it },
                        label = { Text("DTMF Start Delay (milliseconds)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Send Auto-Reply SMS")
                    Switch(
                        checked = sendSms,
                        onCheckedChange = { sendSms = it }
                    )
                }

                if (sendSms) {
                    OutlinedTextField(
                        value = smsMessage,
                        onValueChange = { smsMessage = it },
                        label = { Text("SMS Message Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto-Hangup via Call.disconnect()")
                    Switch(
                        checked = autoHangup,
                        onCheckedChange = { autoHangup = it }
                    )
                }

                if (autoHangup) {
                    OutlinedTextField(
                        value = hangupDelaySec,
                        onValueChange = { hangupDelaySec = it },
                        label = { Text("Hangup Delay (seconds after actions)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalRule = initialRule.copy(
                        name = name.ifBlank { "Caller Rule" },
                        phoneNumberPattern = pattern.trim(),
                        autoAnswer = autoAnswer,
                        answerDelaySec = answerDelaySec.toIntOrNull() ?: 1,
                        dtmfSequence = dtmfSequence.trim(),
                        dtmfDelayMs = dtmfDelayMs.toLongOrNull() ?: 800L,
                        sendSms = sendSms,
                        smsMessage = smsMessage.trim(),
                        autoHangup = autoHangup,
                        hangupDelaySec = hangupDelaySec.toIntOrNull() ?: 2
                    )
                    onSave(finalRule)
                },
                modifier = Modifier.testTag("rule_save_button")
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showContactPicker) {
        ContactPickerDialog(
            favorites = favorites,
            deviceContacts = deviceContacts,
            onContactSelected = { contactName, contactNumber, _ ->
                pattern = contactNumber
                if (name.isBlank() || name == "New Automation Rule") {
                    name = "$contactName Rule"
                }
            },
            onDismiss = { showContactPicker = false },
            title = "Select Contact for Rule"
        )
    }
}
