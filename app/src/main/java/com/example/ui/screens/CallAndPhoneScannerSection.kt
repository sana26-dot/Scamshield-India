package com.example.ui.screens

import android.app.role.RoleManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ReportCategory
import com.example.security.CallScreeningStateHolder
import com.example.ui.components.ErrorBanner
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.RiskCriticalColor
import com.example.ui.theme.RiskHighColor
import com.example.ui.theme.RiskSafeColor
import com.example.viewmodel.PhoneLookupState

@Composable
fun CallProtectionConceptCard() {
    val context = LocalContext.current
    var hasRole by remember { mutableStateOf(CallScreeningStateHolder.isCallScreeningRoleHeld(context)) }
    val recentScreened by CallScreeningStateHolder.recentScreenedCall.collectAsStateWithLifecycle()
    var showWarningPreview by remember { mutableStateOf(false) }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasRole = CallScreeningStateHolder.isCallScreeningRoleHeld(context)
    }

    GlassmorphicCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (hasRole) Icons.Default.CheckCircle else Icons.Default.Call,
                    contentDescription = null,
                    tint = if (hasRole) RiskSafeColor else ElectricViolet
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Android Call Screening & Privacy Limits",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = if (hasRole) {
                    "Call Screening Role is ACTIVE. ScamShield can inspect unknown incoming carrier verification metadata (STIR/SHAKEN) without reading your contacts or call logs."
                } else {
                    "On Android 10+, you can optionally grant the Call Screening Role so ScamShield checks unknown incoming call verification metadata. Android restricts apps from secretly listening to or recording live phone audio, and WhatsApp/Telegram VoIP calls cannot be intercepted by third-party apps."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Zero silent recording • No contact uploads • Post-call manual analysis & reporting always works.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (recentScreened != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            text = "Last Screened Unknown Call: ${recentScreened!!.phoneNumber}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = recentScreened!!.verificationNote,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Button(
                        onClick = {
                            val roleManager = context.getSystemService(RoleManager::class.java)
                            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                                roleLauncher.launch(
                                    roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                )
                            }
                        },
                        enabled = !hasRole,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (hasRole) "Screening Active" else "Enable Call Screening")
                    }
                }
                OutlinedButton(
                    onClick = { showWarningPreview = !showWarningPreview },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (showWarningPreview) "Hide Alert Preview" else "Preview Alert UI")
                }
            }

            if (showWarningPreview) {
                Surface(
                    color = RiskCriticalColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, RiskCriticalColor.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = RiskCriticalColor
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Suspicious Call Warning (Preview)",
                                style = MaterialTheme.typography.titleSmall,
                                color = RiskCriticalColor
                            )
                            Text(
                                text = "Potential scam indicators: Caller claims to be CBI/TRAI threatening Digital Arrest. Hang up immediately — never transfer money or share OTP.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhoneNumberLookupCard(
    initialNumber: String = "",
    lookupState: PhoneLookupState,
    onLookup: (String) -> Unit,
    onReportNumber: (String) -> Unit
) {
    var phoneInput by remember(initialNumber) { mutableStateOf(initialNumber) }

    GlassmorphicCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Indian Phone Number & Community Check",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = phoneInput,
                onValueChange = { phoneInput = it },
                label = { Text("Enter Indian phone number (e.g. +91 9876543210 or 140...)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_lookup_input")
            )
            Button(
                onClick = { onLookup(phoneInput) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("phone_lookup_button")
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Check Format & Community Reports")
            }

            if (lookupState.isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Checking community reports...", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (lookupState.errorMessage != null) {
                ErrorBanner(lookupState.errorMessage)
            }

            val format = lookupState.formatAnalysis
            if (format != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Normalized: ${format.normalizedNumber} • ${format.formatClassification}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        format.observations.forEach { obs ->
                            Text(text = "• $obs", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                val reports = lookupState.matchingReports
                val count = reports.size
                val statusColor = when {
                    count >= 3 -> RiskCriticalColor
                    count >= 1 -> RiskHighColor
                    else -> RiskSafeColor
                }

                Surface(
                    color = statusColor.copy(alpha = 0.11f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (count > 0) Icons.Default.Warning else Icons.Default.Info,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (count > 0) {
                                    "Reported as suspicious ($count community report${if (count == 1) "" else "s"})"
                                } else {
                                    "No community reports found yet"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = statusColor
                            )
                        }

                        if (count > 0) {
                            val confidence = when {
                                count >= 5 -> "High community signal (multiple independent reports)"
                                count >= 2 -> "Moderate community signal"
                                else -> "Single unverified user report — treat with caution"
                            }
                            Text(
                                text = "Confidence: $confidence",
                                style = MaterialTheme.typography.labelMedium
                            )
                            reports.take(4).forEach { rep ->
                                Text(
                                    text = "• [${ReportCategory.fromRaw(rep.category).displayLabel}] (${rep.incidentDate}): ${rep.description}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            Text(
                                text = "Absence of reports does not guarantee a number is safe. Never share OTPs or transfer money to unknown callers.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { onReportNumber(format.normalizedNumber) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskHighColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Report This Number to Community")
                }
            }
        }
    }
}
