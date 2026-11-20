package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.AiScanUiState
import com.example.model.AppLanguage
import com.example.model.ScanType
import com.example.security.UrlAndPhoneHeuristics
import com.example.ui.components.AiScanningAnimationCard
import com.example.ui.components.ErrorBanner
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.ScamResultCard
import com.example.ui.components.ScreenshotPickerCard
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RiskHighColor
import com.example.viewmodel.PhoneLookupState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScannerDetailScreen(
    scanType: ScanType,
    language: AppLanguage,
    scanState: AiScanUiState,
    phoneLookupState: PhoneLookupState,
    onBack: () -> Unit,
    onRunTextScan: (ScanType, String, String, List<String>) -> Unit,
    onRunImageScan: (ScanType, Bitmap, String, String) -> Unit,
    onLookupPhone: (String) -> Unit,
    onNavigateToReportNumber: (String) -> Unit
) {
    val strings = AppStrings.forLanguage(language)

    var primaryInput by remember(scanType) { mutableStateOf("") }
    var secondaryInput by remember(scanType) { mutableStateOf("") }
    var tertiaryInput by remember(scanType) { mutableStateOf("") }
    var selectedImageUri by remember(scanType) { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember(scanType) { mutableStateOf<Bitmap?>(null) }

    val platformOptions = when (scanType) {
        ScanType.MESSAGE -> listOf("SMS", "WhatsApp", "Telegram", "Instagram", "Forwarded", "Other")
        ScanType.SCREENSHOT -> listOf("WhatsApp", "SMS", "Email", "Payment Request", "Advertisement", "Job Offer")
        ScanType.CALL -> listOf("Unknown Mobile", "Fake Police / CBI", "TRAI / Telecom", "Bank / KYC", "Courier / Customs")
        ScanType.UPI -> listOf("Collect Request", "QR Code", "Refund Claim", "Fake Payment Screenshot", "Merchant")
        ScanType.JOB -> listOf("WhatsApp Recruiter", "Telegram Task", "Email Offer", "Internship", "LinkedIn / Portal")
        ScanType.IMPERSONATION -> listOf("WhatsApp", "Instagram", "Facebook", "Telegram", "X / Support")
        else -> emptyList()
    }
    var selectedOption by remember(scanType) { mutableStateOf(platformOptions.firstOrNull().orEmpty()) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("scanner_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(
                        text = "${scanType.label} Analyzer",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Supports English, हिंदी, मराठी, Hinglish & transliterated text",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Specialized Call Screening / Phone Lookup cards
            if (scanType == ScanType.CALL) {
                CallProtectionConceptCard()
                PhoneNumberLookupCard(
                    initialNumber = secondaryInput,
                    lookupState = phoneLookupState,
                    onLookup = onLookupPhone,
                    onReportNumber = onNavigateToReportNumber
                )
            } else if (scanType == ScanType.PHONE) {
                PhoneNumberLookupCard(
                    initialNumber = primaryInput,
                    lookupState = phoneLookupState,
                    onLookup = onLookupPhone,
                    onReportNumber = onNavigateToReportNumber
                )
            }

            // UPI Golden Rule Card
            if (scanType == ScanType.UPI) {
                Surface(
                    color = RiskHighColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = RiskHighColor
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "How UPI Works in India",
                                style = MaterialTheme.typography.titleSmall,
                                color = RiskHighColor
                            )
                            Text(
                                text = "A UPI PIN is ONLY used to authorize sending money OUT of your account — never to receive money. Screenshots can be edited; always check your real bank/UPI transaction history.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Phishing Link safety card
            if (scanType == ScanType.LINK) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "ScamShield inspects links structurally and via AI without opening them in your browser. Remember: HTTPS (padlock) alone does NOT guarantee a link is safe.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Main AI Analyzer Input Card
            GlassmorphicCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (platformOptions.isNotEmpty()) {
                        Text(
                            text = "Select Source / Category",
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            platformOptions.forEach { opt ->
                                FilterChip(
                                    selected = selectedOption == opt,
                                    onClick = { selectedOption = opt },
                                    label = { Text(opt) }
                                )
                            }
                        }
                    }

                    // Sample Indian Scam test presets for quick verification
                    if (scanType == ScanType.MESSAGE) {
                        Text(
                            text = "Try an Indian Scam Example:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val samples = listOf(
                                "Electricity Cutoff" to "Dear Consumer, your electricity power will be disconnected tonight at 9:30 PM because previous month bill was not updated. Immediately call our Electricity Officer +91 9830011223 or pay via bit.ly/mseb-pay-now",
                                "Digital Arrest" to "TRAI Notice: Your Aadhaar linked mobile number is involved in money laundering parcel seized by Mumbai Customs. Stay on Skype video call for Digital Arrest verification or transfer Rs 45,000 security deposit.",
                                "Fake Bank KYC" to "SBI Alert: Your YONO account will be suspended today! Update PAN KYC within 2 hours at http://sbi-kyc-update-in.xyz/login or share OTP."
                            )
                            samples.forEach { (label, sample) ->
                                FilterChip(
                                    selected = false,
                                    onClick = { primaryInput = sample },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    when (scanType) {
                        ScanType.EMAIL -> {
                            OutlinedTextField(
                                value = secondaryInput,
                                onValueChange = { secondaryInput = it },
                                label = { Text("Sender email address (e.g. support@hdfc-alerts.xyz)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = tertiaryInput,
                                onValueChange = { tertiaryInput = it },
                                label = { Text("Email headers (optional)") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        ScanType.CALL -> {
                            OutlinedTextField(
                                value = secondaryInput,
                                onValueChange = { secondaryInput = it },
                                label = { Text("Caller phone number (optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        ScanType.JOB -> {
                            OutlinedTextField(
                                value = secondaryInput,
                                onValueChange = { secondaryInput = it },
                                label = { Text("Recruiter email / company name (optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        else -> Unit
                    }

                    val primaryLabel = when (scanType) {
                        ScanType.MESSAGE -> "Paste suspicious SMS, WhatsApp, Telegram, or forwarded message"
                        ScanType.SCREENSHOT -> "Optional notes or question about this screenshot"
                        ScanType.LINK -> "Paste suspicious URL (e.g. https://bit.ly/... or .apk link)"
                        ScanType.CALL -> "Paste call transcript or describe what the caller said"
                        ScanType.PHONE -> "Optional: Describe what this caller claimed or texted"
                        ScanType.EMAIL -> "Paste email content / body"
                        ScanType.UPI -> "Paste UPI payment request message, VPA (@handle), or describe situation"
                        ScanType.JOB -> "Paste job description, internship offer, or recruiter message"
                        ScanType.IMPERSONATION -> "Paste DM/message or describe the suspicious social media profile"
                    }

                    OutlinedTextField(
                        value = primaryInput,
                        onValueChange = { primaryInput = it },
                        label = { Text(primaryLabel) },
                        minLines = if (scanType == ScanType.LINK) 1 else 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scanner_primary_input")
                    )

                    if (scanType in listOf(
                            ScanType.SCREENSHOT,
                            ScanType.CALL,
                            ScanType.EMAIL,
                            ScanType.UPI,
                            ScanType.JOB,
                            ScanType.IMPERSONATION
                        )
                    ) {
                        ScreenshotPickerCard(
                            selectedImageUri = selectedImageUri,
                            onImageSelected = { uri, bmp ->
                                selectedImageUri = uri
                                selectedBitmap = bmp
                            },
                            buttonLabel = if (scanType == ScanType.CALL) {
                                "Upload Call History Screenshot (Optional)"
                            } else {
                                "Upload Screenshot for AI OCR & Vision Scan"
                            }
                        )
                    }

                    Button(
                        onClick = {
                            val bmp = selectedBitmap
                            if (bmp != null) {
                                val combinedNotes = listOfNotNull(
                                    secondaryInput.takeIf { it.isNotBlank() },
                                    primaryInput.takeIf { it.isNotBlank() }
                                ).joinToString("\n")
                                onRunImageScan(scanType, bmp, combinedNotes, selectedOption)
                            } else {
                                val hints = if (scanType == ScanType.LINK && primaryInput.isNotBlank()) {
                                    UrlAndPhoneHeuristics.inspectUrl(primaryInput).warnings
                                } else {
                                    emptyList()
                                }
                                val combinedText = buildString {
                                    if (secondaryInput.isNotBlank()) append("Header/Sender/Caller: $secondaryInput\n")
                                    if (tertiaryInput.isNotBlank()) append("Extra Headers: $tertiaryInput\n")
                                    append(primaryInput)
                                }
                                onRunTextScan(scanType, combinedText, selectedOption, hints)
                            }
                        },
                        enabled = scanState !is AiScanUiState.Scanning &&
                            (primaryInput.isNotBlank() || selectedBitmap != null),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricViolet,
                            contentColor = PureWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("scanner_analyze_button")
                    ) {
                        Icon(Icons.Default.Radar, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (selectedBitmap != null) "Analyze Screenshot with Gemini AI" else "Analyze with Gemini AI",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Scan Output Section
            when (scanState) {
                is AiScanUiState.Idle -> Unit
                is AiScanUiState.Scanning -> AiScanningAnimationCard()
                is AiScanUiState.Error -> ErrorBanner(scanState.message)
                is AiScanUiState.Result -> {
                    ScamResultCard(
                        analysis = scanState.analysis,
                        disclaimerText = strings.aiDisclaimer,
                        localHints = scanState.localHints
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
