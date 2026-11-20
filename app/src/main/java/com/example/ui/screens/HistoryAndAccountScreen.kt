package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.RiskLevel
import com.example.model.ScamAnalysisResult
import com.example.model.ScanRecord
import com.example.model.ScanType
import com.example.model.ThemePreference
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.ErrorBanner
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.RiskBadge
import com.example.ui.components.ScamResultCard
import com.example.ui.theme.RiskCriticalColor
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanHistoryScreen(
    language: AppLanguage,
    historyState: UiState<List<ScanRecord>>,
    onBack: () -> Unit,
    onDeleteScan: (String) -> Unit,
    onClearAll: () -> Unit
) {
    val strings = AppStrings.forLanguage(language)
    var selectedRecord by remember { mutableStateOf<ScanRecord?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("history_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Scan History", style = MaterialTheme.typography.headlineSmall)
                    }
                    if (historyState is UiState.Success && historyState.data.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = RiskCriticalColor),
                            modifier = Modifier.testTag("clear_all_history_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear All")
                        }
                    }
                }
            }

            when (historyState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item { ErrorBanner(historyState.message) }
                }
                is UiState.Success -> {
                    val items = historyState.data
                    if (items.isEmpty()) {
                        item {
                            GlassmorphicCard {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text("No Scan History", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Your completed scam analyses appear here. You can delete individual items or clear all history anytime.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(items, key = { it.id }) { record ->
                            val ts = (record.updatedAt ?: record.createdAt ?: Timestamp.now()).toDate()
                            val risk = RiskLevel.fromRaw(record.riskLevel)
                            GlassmorphicCard(onClick = { selectedRecord = record }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = ScanType.fromRaw(record.scanType).label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            RiskBadge(riskLevel = risk, score = record.riskScore.toInt())
                                        }
                                        Text(
                                            text = record.scamCategory,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = record.summary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = dateFormat.format(ts),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteScan(record.id) },
                                        modifier = Modifier.testTag("delete_scan_${record.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete scan",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedRecord != null) {
        val rec = selectedRecord!!
        ModalBottomSheet(onDismissRequest = { selectedRecord = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                ScamResultCard(
                    analysis = ScamAnalysisResult(
                        riskLevel = RiskLevel.fromRaw(rec.riskLevel),
                        riskScore = rec.riskScore.toInt(),
                        scamType = rec.scamCategory,
                        summary = rec.summary,
                        simpleExplanation = rec.explanation,
                        suspiciousPhrases = rec.indicators,
                        suspiciousUrls = rec.detectedUrls,
                        scammerGoal = "",
                        nextSteps = rec.recommendedActions
                    ),
                    disclaimerText = strings.aiDisclaimer
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Scan History?") },
            text = { Text("This permanently deletes all saved scan summaries from your account.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        onClearAll()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RiskCriticalColor)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountSettingsScreen(
    isGuestMode: Boolean,
    userEmail: String?,
    userPhone: String?,
    profileState: UiState<UserProfile?>,
    language: AppLanguage,
    themePreference: ThemePreference,
    onBack: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectTheme: (ThemePreference) -> Unit,
    onSaveProfile: (String, String, Boolean, Boolean) -> Unit,
    onSignOut: () -> Unit
) {
    val profile = (profileState as? UiState.Success)?.data
    var displayName by remember(profile?.displayName) {
        mutableStateOf(profile?.displayName ?: if (isGuestMode) "Guest User" else "ScamShield User")
    }
    var indianPhone by remember(profile?.indianPhoneNumber) {
        mutableStateOf(profile?.indianPhoneNumber ?: userPhone.orEmpty())
    }
    var privacyMinimize by remember(profile?.privacyMinimizeStorage) {
        mutableStateOf(profile?.privacyMinimizeStorage ?: true)
    }
    var callScreeningPref by remember(profile?.callScreeningEnabled) {
        mutableStateOf(profile?.callScreeningEnabled ?: false)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("account_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(6.dp))
                Text("Account & Privacy Settings", style = MaterialTheme.typography.headlineSmall)
            }

            // Account Profile Card
            GlassmorphicCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isGuestMode) "Guest / Demo Session (Local Only)" else "Signed-In Account",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (isGuestMode) {
                        Text(
                            text = "You are using ScamShield India in Guest / Demo Mode. AI scanners work live, while history stays local for this session.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        if (!userEmail.isNullOrBlank()) {
                            Text(
                                text = "Google Account: $userEmail",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!userPhone.isNullOrBlank()) {
                            Text(
                                text = "Verified Mobile: $userPhone",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = indianPhone,
                        onValueChange = { indianPhone = it },
                        label = { Text("Indian Mobile (+91) for safety profile") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Language & Theme Card
            GlassmorphicCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = AppStrings.forLanguage(language).languageLabel,
                        style = MaterialTheme.typography.titleMedium
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = language == lang,
                                onClick = { onSelectLanguage(lang) },
                                label = { Text("${lang.nativeLabel} (${lang.englishLabel})") }
                            )
                        }
                    }

                    Text("App Theme", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemePreference.entries.forEach { pref ->
                            FilterChip(
                                selected = themePreference == pref,
                                onClick = { onSelectTheme(pref) },
                                label = {
                                    Text(
                                        when (pref) {
                                            ThemePreference.SYSTEM -> "System"
                                            ThemePreference.LIGHT -> "Light"
                                            ThemePreference.DARK -> "Dark"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Privacy Controls Card
            GlassmorphicCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Privacy-First Controls", style = MaterialTheme.typography.titleMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Minimize Stored Message Content", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "When ON, raw scanned text previews are omitted from cloud scan history.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = privacyMinimize,
                            onCheckedChange = { privacyMinimize = it }
                        )
                    }

                    Text(
                        text = "ScamShield India never reads your private SMS/WhatsApp inbox automatically, never uploads your contacts, never records phone calls, and never asks for OTPs, UPI PINs, or passwords.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            onSaveProfile(displayName, indianPhone, callScreeningPref, privacyMinimize)
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_profile_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Preferences")
                    }
                }
            }

            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskCriticalColor),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isGuestMode) "Exit Guest Mode / Sign In" else "Sign Out")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
