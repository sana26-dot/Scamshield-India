package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesRepository
import com.example.data.ScamAiRepository
import com.example.data.ScamRepository
import com.example.model.AiScanUiState
import com.example.model.AppLanguage
import com.example.model.ChatMessage
import com.example.model.CommunityReport
import com.example.model.ReportCategory
import com.example.model.ScanRecord
import com.example.model.ScanType
import com.example.model.ThemePreference
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.security.PhoneFormatAnalysis
import com.example.security.UrlAndPhoneHeuristics
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PhoneLookupState(
    val isLoading: Boolean = false,
    val formatAnalysis: PhoneFormatAnalysis? = null,
    val matchingReports: List<CommunityReport> = emptyList(),
    val errorMessage: String? = null
)

class MainViewModel(
    private val scamRepository: ScamRepository?,
    private val aiRepository: ScamAiRepository,
    private val preferencesRepository: PreferencesRepository,
    private val currentUserId: String,
    initialDisplayName: String,
    initialPhoneNumber: String? = null
) : ViewModel() {

    val isGuestMode: Boolean = scamRepository == null

    val language: StateFlow<AppLanguage> = preferencesRepository.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppLanguage.EN)

    val themePreference: StateFlow<ThemePreference> = preferencesRepository.themeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ThemePreference.SYSTEM)

    // In-memory local states used only in Guest / Demo Mode (no unauthenticated Firestore calls)
    private val _guestProfile = MutableStateFlow(
        UserProfile(
            userId = currentUserId,
            displayName = initialDisplayName,
            indianPhoneNumber = initialPhoneNumber,
            preferredLanguage = AppLanguage.EN.code,
            callScreeningEnabled = false,
            privacyMinimizeStorage = true
        )
    )
    private val _guestHistory = MutableStateFlow<List<ScanRecord>>(emptyList())
    private val _guestReports = MutableStateFlow<List<CommunityReport>>(emptyList())

    val userProfileState: StateFlow<UiState<UserProfile?>> = if (scamRepository != null) {
        scamRepository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing profile", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load profile"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    } else {
        _guestProfile.map<UserProfile, UiState<UserProfile?>> { UiState.Success(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(_guestProfile.value))
    }

    val scanHistoryState: StateFlow<UiState<List<ScanRecord>>> = if (scamRepository != null) {
        scamRepository.observeScanHistory(currentUserId)
            .map<List<ScanRecord>, UiState<List<ScanRecord>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing scan history", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load scan history"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    } else {
        _guestHistory.map<List<ScanRecord>, UiState<List<ScanRecord>>> { UiState.Success(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))
    }

    val communityReportsState: StateFlow<UiState<List<CommunityReport>>> = if (scamRepository != null) {
        scamRepository.observePublishedCommunityReports()
            .map<List<CommunityReport>, UiState<List<CommunityReport>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing community reports", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load community reports"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    } else {
        _guestReports.map<List<CommunityReport>, UiState<List<CommunityReport>>> { UiState.Success(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))
    }

    private val _scanState = MutableStateFlow<AiScanUiState>(AiScanUiState.Idle)
    val scanState: StateFlow<AiScanUiState> = _scanState.asStateFlow()

    private val _phoneLookupState = MutableStateFlow(PhoneLookupState())
    val phoneLookupState: StateFlow<PhoneLookupState> = _phoneLookupState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatStreaming = MutableStateFlow(false)
    val isChatStreaming: StateFlow<Boolean> = _isChatStreaming.asStateFlow()

    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

    init {
        if (scamRepository != null) {
            viewModelScope.launch {
                scamRepository.ensureUserProfile(
                    displayName = initialDisplayName,
                    preferredLanguage = language.value,
                    phoneNumber = initialPhoneNumber
                )
            }
        }
    }

    fun clearActionFeedback() {
        _actionFeedback.value = null
    }

    fun resetScanState() {
        _scanState.value = AiScanUiState.Idle
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(lang)
            if (scamRepository == null) {
                _guestProfile.value = _guestProfile.value.copy(preferredLanguage = lang.code)
                return@launch
            }
            val profile = (userProfileState.value as? UiState.Success)?.data
            if (profile != null) {
                scamRepository.updateUserProfile(
                    displayName = profile.displayName,
                    indianPhoneNumber = profile.indianPhoneNumber,
                    preferredLanguage = lang,
                    callScreeningEnabled = profile.callScreeningEnabled,
                    privacyMinimizeStorage = profile.privacyMinimizeStorage
                )
            }
        }
    }

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch {
            preferencesRepository.setTheme(theme)
        }
    }

    fun updateProfileSettings(
        displayName: String,
        indianPhone: String,
        callScreeningEnabled: Boolean,
        privacyMinimizeStorage: Boolean
    ) {
        viewModelScope.launch {
            val cleanedPhone = indianPhone.trim()
            if (cleanedPhone.isNotEmpty() && !UrlAndPhoneHeuristics.isValidIndianMobileInput(cleanedPhone)) {
                _actionFeedback.value = "Please enter a valid 10-digit Indian mobile number (+91)."
                return@launch
            }
            val normalizedPhone = if (cleanedPhone.isEmpty()) null else UrlAndPhoneHeuristics.normalizeIndianPhone(cleanedPhone)

            if (scamRepository == null) {
                _guestProfile.value = _guestProfile.value.copy(
                    displayName = displayName.trim().ifBlank { "Guest User" },
                    indianPhoneNumber = normalizedPhone,
                    callScreeningEnabled = callScreeningEnabled,
                    privacyMinimizeStorage = privacyMinimizeStorage
                )
                _actionFeedback.value = "Preferences saved for this Guest session."
                return@launch
            }

            val result = scamRepository.updateUserProfile(
                displayName = displayName,
                indianPhoneNumber = normalizedPhone,
                preferredLanguage = language.value,
                callScreeningEnabled = callScreeningEnabled,
                privacyMinimizeStorage = privacyMinimizeStorage
            )
            _actionFeedback.value = if (result.isSuccess) {
                "Account & privacy preferences saved."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Failed to update settings."
            }
        }
    }

    fun runTextScan(
        scanType: ScanType,
        inputText: String,
        platformOrCategory: String = "",
        localHints: List<String> = emptyList()
    ) {
        if (inputText.isBlank()) {
            _scanState.value = AiScanUiState.Error("Please enter content to analyze.")
            return
        }
        viewModelScope.launch {
            _scanState.value = AiScanUiState.Scanning
            val activeLang = language.value
            val result = aiRepository.analyzeTextOrContext(
                scanType = scanType,
                contextDetails = inputText,
                platformOrCategory = platformOrCategory,
                language = activeLang
            )
            result.onSuccess { analysis ->
                _scanState.value = AiScanUiState.Result(scanType, analysis, localHints)
                val minimize = (userProfileState.value as? UiState.Success)?.data?.privacyMinimizeStorage ?: true
                if (scamRepository != null) {
                    scamRepository.saveScanRecord(
                        scanType = scanType,
                        analysis = analysis,
                        rawInputPreview = inputText,
                        language = activeLang,
                        minimizeStorage = minimize
                    )
                } else {
                    saveGuestScan(scanType, analysis, inputText, activeLang, minimize)
                }
            }.onFailure { err ->
                _scanState.value = AiScanUiState.Error(
                    err.localizedMessage ?: "AI scan failed. Please check internet connection and try again."
                )
            }
        }
    }

    fun runImageScan(
        scanType: ScanType,
        bitmap: Bitmap,
        notes: String = "",
        sourceCategory: String = ""
    ) {
        viewModelScope.launch {
            _scanState.value = AiScanUiState.Scanning
            val activeLang = language.value
            val result = aiRepository.analyzeImageWithContext(
                scanType = scanType,
                bitmap = bitmap,
                additionalText = notes,
                sourceCategory = sourceCategory,
                language = activeLang
            )
            result.onSuccess { analysis ->
                _scanState.value = AiScanUiState.Result(scanType, analysis)
                val minimize = (userProfileState.value as? UiState.Success)?.data?.privacyMinimizeStorage ?: true
                val preview = analysis.extractedText.ifBlank { notes }
                if (scamRepository != null) {
                    scamRepository.saveScanRecord(
                        scanType = scanType,
                        analysis = analysis,
                        rawInputPreview = preview,
                        language = activeLang,
                        minimizeStorage = minimize
                    )
                } else {
                    saveGuestScan(scanType, analysis, preview, activeLang, minimize)
                }
            }.onFailure { err ->
                _scanState.value = AiScanUiState.Error(
                    err.localizedMessage ?: "Screenshot analysis failed. Please try again."
                )
            }
        }
    }

    private fun saveGuestScan(
        scanType: ScanType,
        analysis: com.example.model.ScamAnalysisResult,
        rawPreview: String,
        language: AppLanguage,
        minimize: Boolean
    ) {
        val now = Timestamp.now()
        val record = ScanRecord(
            id = "guest_${System.currentTimeMillis()}",
            userId = currentUserId,
            scanType = scanType.name,
            riskLevel = analysis.riskLevel.apiValue,
            riskScore = analysis.riskScore.toLong(),
            scamCategory = analysis.scamType,
            summary = analysis.summary.ifBlank { analysis.simpleExplanation },
            inputPreview = if (minimize) null else rawPreview.take(240),
            explanation = analysis.simpleExplanation.ifBlank { analysis.summary },
            indicators = analysis.suspiciousPhrases,
            recommendedActions = analysis.nextSteps,
            detectedUrls = analysis.suspiciousUrls,
            language = language.code,
            createdAt = now,
            updatedAt = now
        )
        _guestHistory.value = listOf(record) + _guestHistory.value
    }

    fun lookupPhoneNumber(rawNumber: String) {
        val trimmed = rawNumber.trim()
        if (trimmed.length < 4) {
            _phoneLookupState.value = PhoneLookupState(errorMessage = "Enter a valid Indian phone number or short code.")
            return
        }
        val formatInfo = UrlAndPhoneHeuristics.analyzeIndianPhoneFormat(trimmed)
        if (scamRepository == null) {
            val matches = _guestReports.value.filter { it.reportedTarget == formatInfo.normalizedNumber }
            _phoneLookupState.value = PhoneLookupState(
                isLoading = false,
                formatAnalysis = formatInfo,
                matchingReports = matches
            )
            return
        }

        viewModelScope.launch {
            _phoneLookupState.value = PhoneLookupState(
                isLoading = true,
                formatAnalysis = formatInfo
            )
            val result = scamRepository.lookupReportsForTarget(formatInfo.normalizedNumber)
            result.onSuccess { reports ->
                _phoneLookupState.value = PhoneLookupState(
                    isLoading = false,
                    formatAnalysis = formatInfo,
                    matchingReports = reports
                )
            }.onFailure { err ->
                _phoneLookupState.value = PhoneLookupState(
                    isLoading = false,
                    formatAnalysis = formatInfo,
                    errorMessage = err.localizedMessage ?: "Could not check community database."
                )
            }
        }
    }

    fun submitCommunityReport(
        category: ReportCategory,
        description: String,
        rawTarget: String,
        incidentDate: String
    ) {
        viewModelScope.launch {
            val cleanTarget = if (category == ReportCategory.SCAM_CALL || category == ReportCategory.SMS_SCAM) {
                if (rawTarget.any { it.isDigit() } && !rawTarget.contains("http", true)) {
                    UrlAndPhoneHeuristics.normalizeIndianPhone(rawTarget)
                } else {
                    rawTarget.trim()
                }
            } else {
                rawTarget.trim()
            }

            if (scamRepository == null) {
                val now = Timestamp.now()
                val localRep = CommunityReport(
                    id = "guest_rep_${System.currentTimeMillis()}",
                    userId = currentUserId,
                    category = category.name,
                    description = description.trim().take(600),
                    reportedTarget = cleanTarget.ifBlank { null },
                    incidentDate = incidentDate.trim().ifBlank { "2026-11-20" },
                    moderationStatus = "published",
                    createdAt = now,
                    updatedAt = now
                )
                _guestReports.value = listOf(localRep) + _guestReports.value
                _actionFeedback.value = "Saved in local Guest session (sign in to publish to community)."
                return@launch
            }

            val result = scamRepository.submitCommunityReport(
                category = category,
                description = description,
                reportedTarget = cleanTarget.ifBlank { null },
                incidentDate = incidentDate
            )
            _actionFeedback.value = if (result.isSuccess) {
                "Report submitted. Thank you for helping keep India safer."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Failed to submit report."
            }
        }
    }

    fun deleteCommunityReport(reportId: String) {
        if (scamRepository == null) {
            _guestReports.value = _guestReports.value.filterNot { it.id == reportId }
            _actionFeedback.value = "Report removed."
            return
        }
        viewModelScope.launch {
            val res = scamRepository.deleteCommunityReport(reportId)
            _actionFeedback.value = if (res.isSuccess) "Report removed." else "Could not remove report."
        }
    }

    fun deleteScanRecord(scanId: String) {
        if (scamRepository == null) {
            _guestHistory.value = _guestHistory.value.filterNot { it.id == scanId }
            _actionFeedback.value = "Scan deleted."
            return
        }
        viewModelScope.launch {
            val res = scamRepository.deleteScanRecord(scanId)
            _actionFeedback.value = if (res.isSuccess) "Scan deleted." else "Could not delete scan."
        }
    }

    fun clearScanHistory() {
        if (scamRepository == null) {
            _guestHistory.value = emptyList()
            _actionFeedback.value = "All scan history cleared."
            return
        }
        viewModelScope.launch {
            val res = scamRepository.clearScanHistory()
            _actionFeedback.value = if (res.isSuccess) "All scan history cleared." else "Could not clear history."
        }
    }

    fun sendShieldAiMessage(question: String) {
        val trimmed = question.trim()
        if (trimmed.isBlank() || _isChatStreaming.value) return

        val historySnapshot = _chatMessages.value.map { it.isUser to it.text }
        val userMsg = ChatMessage(
            id = "u_${System.currentTimeMillis()}",
            isUser = true,
            text = trimmed
        )
        val assistantMsgId = "a_${System.currentTimeMillis() + 1}"
        _chatMessages.value = _chatMessages.value + userMsg + ChatMessage(
            id = assistantMsgId,
            isUser = false,
            text = "",
            isStreaming = true
        )
        _isChatStreaming.value = true

        viewModelScope.launch {
            try {
                aiRepository.streamShieldAiReply(
                    userMessage = trimmed,
                    historyPairs = historySnapshot,
                    language = language.value
                ).collect { partial ->
                    _chatMessages.value = _chatMessages.value.map { msg ->
                        if (msg.id == assistantMsgId) msg.copy(text = partial, isStreaming = true) else msg
                    }
                }
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == assistantMsgId) msg.copy(isStreaming = false) else msg
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Shield AI stream failed", e)
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == assistantMsgId) {
                        msg.copy(
                            text = "Unable to reach Shield AI right now: ${e.localizedMessage ?: "Network error"}. If this is a financial cyber emergency, call 1930 or visit cybercrime.gov.in immediately.",
                            isStreaming = false
                        )
                    } else msg
                }
            } finally {
                _isChatStreaming.value = false
            }
        }
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
