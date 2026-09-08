package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneForwarded
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.TouchApp
import com.example.ui.components.ContactPickerDialog
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
    whatsAppCallMode: String = "ask_learn",
    onSetWhatsAppCallMode: (String) -> Unit = {},
    onResetWhatsAppChoices: () -> Unit = {},
    learnedChoicesCount: Int = 0,
    spamNumbers: List<SpamNumber> = emptyList(),
    onRemoveSpam: (String) -> Unit = {},
    confirmFavoritesCall: Boolean = false,
    onSetConfirmFavoritesCall: (Boolean) -> Unit = {},
    defaultStartTab: Int = 0,
    onSetDefaultStartTab: (Int) -> Unit = {},
    onToggleRule: (CallerRule) -> Unit,
    onSaveRule: (CallerRule) -> Unit,
    onDeleteRule: (CallerRule) -> Unit,
    onClearLogs: () -> Unit,
    initiallyShowAddRuleWithNumber: String? = null,
    onConsumeAddRuleNumber: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showHistoryDialog by rememberSaveable { mutableStateOf(false) }
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

    Box(modifier = modifier.fillMaxSize().testTag("rules_screen")) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Caller Rules (${rules.size})") },
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Settings") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                // Rules list
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
                                text = "No Automation Rules",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Add rules to auto-answer, dial DTMF extensions, or send SMS replies.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                                Triple("all_international", "All International Numbers", "Directs international (+91, etc.) numbers to WhatsApp"),
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
                                onClick = onResetWhatsAppChoices,
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

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Default Startup Screen", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "Choose which tab opens first when launching the dialer",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val tabs = listOf(
                                    0 to "Favorites",
                                    1 to "Call Log",
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
                        text = "Spam & Blocked Numbers (${spamNumbers.size})",
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
                                text = "Review numbers flagged as spam. You can unmark or remove numbers from the spam filter at any time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (spamNumbers.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "No spam numbers registered. Your spam filter list is clear.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                spamNumbers.forEach { item ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Block,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = item.phoneNumber,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        text = item.label.ifBlank { "Marked Spam" },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                            TextButton(
                                                onClick = { onRemoveSpam(item.phoneNumber) },
                                                modifier = Modifier.testTag("unmark_spam_${item.phoneNumber}")
                                            ) {
                                                Text(
                                                    text = "Unmark (Not Spam)",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF16A34A),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
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

        // Bottom Action Row for Add Rule and Execution History in Tab 0
        if (selectedTab == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
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
                    FloatingActionButton(
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
                        modifier = Modifier.testTag("add_rule_fab"),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Rule")
                    }
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
                        label = { Text("Caller Number Pattern (or * for any)") },
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
