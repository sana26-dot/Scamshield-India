package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppLanguage(
    val code: String,
    val nativeLabel: String,
    val englishLabel: String,
    val aiPromptInstruction: String
) {
    EN(
        code = "en",
        nativeLabel = "English",
        englishLabel = "English",
        aiPromptInstruction = "Respond in clear, simple, non-technical English suitable for Indian users."
    ),
    HI(
        code = "hi",
        nativeLabel = "हिन्दी",
        englishLabel = "Hindi",
        aiPromptInstruction = "Respond in clear, simple, everyday Hindi (Devanagari script) easy for Indian families and elders to understand."
    ),
    MR(
        code = "mr",
        nativeLabel = "मराठी",
        englishLabel = "Marathi",
        aiPromptInstruction = "Respond in clear, simple, everyday Marathi (Devanagari script) easy for Indian families and elders to understand."
    );

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: EN
    }
}

enum class ThemePreference(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): ThemePreference =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class ScanType(val label: String) {
    MESSAGE("Message"),
    SCREENSHOT("Screenshot"),
    LINK("Link / URL"),
    CALL("Scam Call"),
    PHONE("Phone Number"),
    EMAIL("Email"),
    UPI("UPI / Payment"),
    JOB("Job / Internship"),
    IMPERSONATION("Identity / Social");

    companion object {
        fun fromRaw(raw: String?): ScanType =
            entries.firstOrNull { it.name == raw } ?: MESSAGE
    }
}

enum class RiskLevel(val apiValue: String, val displayLabel: String) {
    SAFE_LOOKING("SAFE_LOOKING", "Safe-Looking"),
    CAUTION("CAUTION", "Caution"),
    HIGH_RISK("HIGH_RISK", "High Risk"),
    CRITICAL_RISK("CRITICAL_RISK", "Critical Risk");

    companion object {
        fun fromRaw(raw: String?): RiskLevel =
            entries.firstOrNull { it.name == raw || it.apiValue == raw } ?: CAUTION
    }
}

enum class ReportCategory(val displayLabel: String) {
    SMS_SCAM("Scam SMS"),
    WHATSAPP_SCAM("WhatsApp Scam"),
    SCAM_CALL("Scam Call"),
    PHISHING_LINK("Fake Website / Link"),
    UPI_FRAUD("UPI Fraud"),
    FAKE_JOB("Fake Job Offer"),
    IMPERSONATION("Social Impersonation"),
    FAKE_EMAIL("Fake Email");

    companion object {
        fun fromRaw(raw: String?): ReportCategory =
            entries.firstOrNull { it.name == raw } ?: SMS_SCAM
    }
}

data class ScamAnalysisResult(
    val riskLevel: RiskLevel,
    val riskScore: Int,
    val scamType: String,
    val summary: String,
    val simpleExplanation: String,
    val suspiciousPhrases: List<String>,
    val suspiciousUrls: List<String>,
    val scammerGoal: String,
    val nextSteps: List<String>,
    val extractedText: String = "",
    val shouldAvoidClicking: Boolean = true,
    val shouldAvoidReplying: Boolean = true,
    val shouldAvoidCalling: Boolean = true,
    val shouldAvoidSharingInfo: Boolean = true,
    val verificationChecklist: List<String> = emptyList()
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val indianPhoneNumber: String? = null,
    val preferredLanguage: String = "en",
    val callScreeningEnabled: Boolean = false,
    val privacyMinimizeStorage: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ScanRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val scanType: String = ScanType.MESSAGE.name,
    val riskLevel: String = RiskLevel.CAUTION.name,
    val riskScore: Long = 0L,
    val scamCategory: String = "",
    val summary: String = "",
    val inputPreview: String? = null,
    val explanation: String = "",
    val indicators: List<String> = emptyList(),
    val recommendedActions: List<String> = emptyList(),
    val detectedUrls: List<String> = emptyList(),
    val language: String = "en",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommunityReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = ReportCategory.SMS_SCAM.name,
    val description: String = "",
    val reportedTarget: String? = null,
    val incidentDate: String = "",
    val moderationStatus: String = "published",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class IndiaScamAdvisory(
    val id: String,
    val title: String,
    val severity: RiskLevel,
    val category: String,
    val summary: String,
    val redFlags: List<String>,
    val safetyTip: String
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiScanUiState {
    data object Idle : AiScanUiState
    data object Scanning : AiScanUiState
    data class Result(
        val scanType: ScanType,
        val analysis: ScamAnalysisResult,
        val localHints: List<String> = emptyList()
    ) : AiScanUiState
    data class Error(val message: String) : AiScanUiState
}
