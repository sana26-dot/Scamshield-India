package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.IndiaScamAdvisory
import com.example.model.RiskLevel
import com.example.model.ScanRecord
import com.example.model.ScanType
import com.example.model.UiState
import com.example.security.CallScreeningStateHolder
import com.example.security.UrlAndPhoneHeuristics
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.RiskBadge
import com.example.ui.theme.BrightCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RiskSafeColor
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeDashboardScreen(
    displayName: String,
    language: AppLanguage,
    scanHistoryState: UiState<List<ScanRecord>>,
    onOpenScanner: (ScanType) -> Unit,
    onOpenAllScanners: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenEmergency: () -> Unit
) {
    val context = LocalContext.current
    val strings = AppStrings.forLanguage(language)
    val isScreeningEnabled = remember { CallScreeningStateHolder.isCallScreeningRoleHeld(context) }
    val tipIndex = remember {
        Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % UrlAndPhoneHeuristics.dailySafetyTips.size
    }
    val dailyTip = UrlAndPhoneHeuristics.dailySafetyTips[tipIndex]

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Greeting & Account Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${strings.welcomeBack}, ${displayName.substringBefore(" ")}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onOpenHistory,
                            modifier = Modifier.testTag("home_history_button")
                        ) {
                            Icon(Icons.Default.History, contentDescription = "Scan History")
                        }
                        IconButton(
                            onClick = onOpenAccount,
                            modifier = Modifier.testTag("home_account_button")
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = "Account & Settings")
                        }
                    }
                }
            }

            // Hero Card with artwork & Prominent "Scan for Scam" button
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_shield_hero),
                            contentDescription = "ScamShield AI hero illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(215.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(215.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MidnightNavy.copy(alpha = 0.45f),
                                            MidnightNavy.copy(alpha = 0.90f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = BrightCyan.copy(alpha = 0.18f),
                                shape = CircleShape
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = BrightCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = strings.aiStatusProtected,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = PureWhite
                                    )
                                }
                            }
                            Text(
                                text = strings.aiStatusSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PureWhite.copy(alpha = 0.9f)
                            )
                            Text(
                                text = if (isScreeningEnabled) {
                                    "Call Screening: Active"
                                } else {
                                    "Call Screening: Post-call & manual check ready"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = BrightCyan
                            )
                            Button(
                                onClick = { onOpenScanner(ScanType.MESSAGE) },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricViolet,
                                    contentColor = PureWhite
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("home_scan_for_scam_button")
                            ) {
                                Icon(Icons.Default.Radar, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = strings.scanForScamCta,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }

            // Quick Access Scanners Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.quickToolsTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = onOpenAllScanners) {
                        Text("See all 9")
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                val quickItems = listOf(
                    QuickScannerItem(ScanType.MESSAGE, "Message", Icons.AutoMirrored.Filled.Message, ElectricViolet),
                    QuickScannerItem(ScanType.SCREENSHOT, "Screenshot", Icons.Default.ImageSearch, Color(0xFF0EA5E9)),
                    QuickScannerItem(ScanType.LINK, "Link / URL", Icons.Default.Link, Color(0xFF8B5CF6)),
                    QuickScannerItem(ScanType.CALL, "Call Scam", Icons.Default.Call, Color(0xFFEC4899)),
                    QuickScannerItem(ScanType.UPI, "UPI Fraud", Icons.Default.AccountBalanceWallet, Color(0xFF10B981)),
                    QuickScannerItem(ScanType.EMAIL, "Email", Icons.Default.Email, Color(0xFFF59E0B)),
                    QuickScannerItem(ScanType.PHONE, "Phone No.", Icons.Default.PhoneAndroid, Color(0xFF6366F1)),
                    QuickScannerItem(ScanType.JOB, "Fake Job", Icons.Default.WorkOutline, Color(0xFF14B8A6)),
                    QuickScannerItem(ScanType.IMPERSONATION, "Identity", Icons.Default.PersonSearch, Color(0xFFF97316))
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 3,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    quickItems.forEach { item ->
                        QuickToolTile(
                            item = item,
                            onClick = { onOpenScanner(item.type) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Stay Safe Today card
            item {
                GlassmorphicCard {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = strings.staySafeToday,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = dailyTip,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Recently Scanned Messages
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.recentScansTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = onOpenHistory) {
                        Text("History")
                    }
                }

                when (scanHistoryState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }
                    is UiState.Error -> {
                        Text(
                            text = scanHistoryState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is UiState.Success -> {
                        val recent = scanHistoryState.data.take(3)
                        if (recent.isEmpty()) {
                            GlassmorphicCard(onClick = { onOpenScanner(ScanType.MESSAGE) }) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "No scans yet. Tap 'Scan for Scam' to check any suspicious WhatsApp, SMS, UPI, or call.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                recent.forEach { record ->
                                    RecentScanCard(record = record, onClick = onOpenHistory)
                                }
                            }
                        }
                    }
                }
            }

            // Common Scam Alerts in India
            item {
                Text(
                    text = strings.commonIndiaAlertsTitle,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(UrlAndPhoneHeuristics.commonIndiaAdvisories, key = { it.id }) { advisory ->
                        AdvisoryAlertCard(
                            advisory = advisory,
                            onClick = { onOpenScanner(ScanType.MESSAGE) }
                        )
                    }
                }
            }

            // Quick 1930 Emergency Callout
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onOpenEmergency)
                        .testTag("home_emergency_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.emergencyLostMoneyBtn,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "National Helpline 1930 & cybercrime.gov.in",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private data class QuickScannerItem(
    val type: ScanType,
    val shortTitle: String,
    val icon: ImageVector,
    val tint: Color
)

@Composable
private fun QuickToolTile(
    item: QuickScannerItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        onClick = onClick,
        modifier = modifier.testTag("quick_tool_${item.type.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.tint.copy(alpha = 0.14f))
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.shortTitle,
                    tint = item.tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = item.shortTitle,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentScanCard(record: ScanRecord, onClick: () -> Unit) {
    val risk = RiskLevel.fromRaw(record.riskLevel)
    GlassmorphicCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = record.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AdvisoryAlertCard(
    advisory: IndiaScamAdvisory,
    onClick: () -> Unit
) {
    GlassmorphicCard(
        onClick = onClick,
        modifier = Modifier.width(275.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RiskBadge(riskLevel = advisory.severity)
            Text(
                text = advisory.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = advisory.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Tip: ${advisory.safetyTip}",
                style = MaterialTheme.typography.labelSmall,
                color = RiskSafeColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
