package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.ScanType
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.ElectricViolet

private data class ScannerToolEntry(
    val type: ScanType,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun ScannersHubScreen(
    onOpenScanner: (ScanType) -> Unit
) {
    val tools = listOf(
        ScannerToolEntry(
            ScanType.MESSAGE,
            "AI Scam Message Detector",
            "Analyze SMS, WhatsApp, Telegram, Instagram & forwarded messages in English, Hindi, Marathi, or Hinglish.",
            Icons.AutoMirrored.Filled.Message,
            ElectricViolet
        ),
        ScannerToolEntry(
            ScanType.SCREENSHOT,
            "Screenshot OCR & Vision Scanner",
            "Upload screenshots of chats, payment requests, job offers, or ads via Photo Picker.",
            Icons.Default.ImageSearch,
            Color(0xFF0EA5E9)
        ),
        ScannerToolEntry(
            ScanType.LINK,
            "Phishing Link & Website Scanner",
            "Check shortened URLs, .apk downloads, misspelled bank/gov links without opening them.",
            Icons.Default.Link,
            Color(0xFF8B5CF6)
        ),
        ScannerToolEntry(
            ScanType.CALL,
            "Scam Call & Real-Time Protection",
            "Android CallScreeningService setup, post-call transcript check, digital arrest & fake police call detector.",
            Icons.Default.Call,
            Color(0xFFEC4899)
        ),
        ScannerToolEntry(
            ScanType.PHONE,
            "Indian Phone Number Scanner",
            "Check +91, 140/160 TRAI series formats & community scam reports.",
            Icons.Default.PhoneAndroid,
            Color(0xFF6366F1)
        ),
        ScannerToolEntry(
            ScanType.UPI,
            "UPI & Payment Fraud Detector",
            "Spot fake collect requests, QR receive traps, fake refund screenshots & screen-sharing tricks.",
            Icons.Default.AccountBalanceWallet,
            Color(0xFF10B981)
        ),
        ScannerToolEntry(
            ScanType.EMAIL,
            "Email Phishing Scanner",
            "Inspect sender domains, headers, fake bank/tax/delivery emails, or email screenshots.",
            Icons.Default.Email,
            Color(0xFFF59E0B)
        ),
        ScannerToolEntry(
            ScanType.JOB,
            "Fake Job & Internship Detector",
            "Detect registration fees, Telegram task scams, fake placement letters & get a verification checklist.",
            Icons.Default.WorkOutline,
            Color(0xFF14B8A6)
        ),
        ScannerToolEntry(
            ScanType.IMPERSONATION,
            "Social Media & Identity Scanner",
            "Check fake friend/relative money requests, sextortion, romance scams & impersonation accounts.",
            Icons.Default.PersonSearch,
            Color(0xFFF97316)
        )
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "ScamShield Security Suite",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Choose any analyzer below. Every scan prioritizes your privacy and never requests OTPs or PINs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(tools, key = { it.type.name }) { tool ->
                GlassmorphicCard(
                    onClick = { onOpenScanner(tool.type) },
                    modifier = Modifier.testTag("hub_tool_${tool.type.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(tool.accent.copy(alpha = 0.14f))
                        ) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = null,
                                tint = tool.accent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(text = tool.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = tool.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
