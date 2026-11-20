package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCardOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RiskCriticalColor
import com.example.ui.theme.RiskSafeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyHelpScreen(
    language: AppLanguage
) {
    val context = LocalContext.current
    val strings = AppStrings.forLanguage(language)
    var showLostMoneySheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "India Cybercrime Help Center",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Official Indian portals, helpline 1930, and step-by-step response guides.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Prominent "I've Lost Money" Emergency Button
            item {
                Surface(
                    color = RiskCriticalColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.5.dp, RiskCriticalColor.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = RiskCriticalColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Golden Hour Financial Fraud Action",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = RiskCriticalColor
                                )
                                Text(
                                    text = "Act quickly within the first hour to help freeze fraudulent transfers.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Button(
                            onClick = { showLostMoneySheet = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RiskCriticalColor,
                                contentColor = PureWhite
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("emergency_lost_money_button")
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = strings.emergencyLostMoneyBtn,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            // Official Portals & Helpline 1930
            item {
                GlassmorphicCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Official Government Helplines & Portals",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Button(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930")))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("call_1930_button")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Call 1930 — National Financial Cyber Fraud Helpline")
                        }

                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://cybercrime.gov.in/"))
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_cybercrime_portal_button")
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("National Cyber Crime Portal (cybercrime.gov.in)")
                        }

                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://sancharsaathi.gov.in/"))
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_sanchar_saathi_button")
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Sanchar Saathi — Report Scam Calls / Chakshu")
                        }
                    }
                }
            }

            item {
                GuideSectionCard(
                    title = "1. Contact Bank & Block Cards",
                    icon = Icons.Default.AccountBalance,
                    bullets = listOf(
                        "Call your bank's official toll-free helpline (found on the back of your card or official app).",
                        "Report unauthorized UPI/NEFT/IMPS/card debit and ask for an immediate transaction hold & complaint reference number.",
                        "Temporarily freeze or hotlist compromised debit/credit cards inside mobile banking."
                    )
                )
            }

            item {
                GuideSectionCard(
                    title = "2. Secure Compromised Accounts",
                    icon = Icons.Default.LockReset,
                    bullets = listOf(
                        "Change passwords for email, banking, and UPI apps from a clean device.",
                        "Enable 2-Factor Authentication (2FA) on Google, WhatsApp (Two-Step PIN), and social accounts.",
                        "If you installed a suspicious .apk or remote-access app (AnyDesk/TeamViewer), disconnect internet, uninstall it immediately, and check Accessibility permissions."
                    )
                )
            }

            item {
                GuideSectionCard(
                    title = "3. Preserve Evidence for Investigation",
                    icon = Icons.Default.Description,
                    bullets = listOf(
                        "Save clear screenshots of WhatsApp/SMS chats, payment receipts, UTR / UPI Reference IDs, and caller numbers.",
                        "Note exact date, time, bank account/UPI ID where money went, and URLs clicked.",
                        "Do NOT delete messages until you export screenshots for cybercrime.gov.in."
                    )
                )
            }

            item {
                GuideSectionCard(
                    title = "4. Report Impersonation & Suspicious Numbers",
                    icon = Icons.Default.CreditCardOff,
                    bullets = listOf(
                        "Report fake profiles directly inside WhatsApp, Instagram, Facebook, or X.",
                        "Warn friends/family in your group chat so nobody sends money to the impersonator.",
                        "Report scam numbers & phishing messages on Sanchar Saathi Chakshu and ScamShield Community."
                    )
                )
            }
        }
    }

    if (showLostMoneySheet) {
        ModalBottomSheet(onDismissRequest = { showLostMoneySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Immediate Steps If You Lost Money",
                    style = MaterialTheme.typography.headlineSmall,
                    color = RiskCriticalColor
                )

                val immediateSteps = listOf(
                    "1. Contact your bank or payment provider immediately to flag and freeze the disputed transaction.",
                    "2. Call 1930 promptly if financial cyber fraud has occurred (keep UTR/transaction ID ready).",
                    "3. Report the incident through the official portal: https://cybercrime.gov.in/.",
                    "4. Save screenshots, transaction IDs, bank SMS alerts, and scammer messages as evidence.",
                    "5. Change compromised passwords (email, netbanking, social media) immediately.",
                    "6. Block compromised debit/credit cards or temporarily freeze accounts where appropriate.",
                    "7. WARNING: Do NOT pay additional money to anyone promising to recover lost money — recovery agents asking for fees are secondary scammers."
                )

                immediateSteps.forEach { step ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = RiskSafeColor,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = step, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930")))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RiskCriticalColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Call 1930")
                    }
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://cybercrime.gov.in/"))
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Open Portal")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun GuideSectionCard(
    title: String,
    icon: ImageVector,
    bullets: List<String>
) {
    GlassmorphicCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            bullets.forEach { bullet ->
                Text(
                    text = "• $bullet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
