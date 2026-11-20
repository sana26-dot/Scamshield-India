package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.model.AppLanguage
import com.example.model.RiskLevel
import com.example.model.ScamAnalysisResult
import com.example.model.ScanType
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "ScamAiRepository"
private const val AI_TIMEOUT_MS = 60_000L

class ScamAiRepository {

    private val scamAnalysisSchema = Schema.obj(
        properties = mapOf(
            "riskLevel" to Schema.enumeration(
                values = listOf("SAFE_LOOKING", "CAUTION", "HIGH_RISK", "CRITICAL_RISK"),
                description = "Risk assessment level. Never claim guaranteed safety."
            ),
            "riskScore" to Schema.integer(description = "Risk score from 0 to 100."),
            "scamType" to Schema.string(description = "Specific scam category detected, e.g. Digital Arrest, UPI Collect Fraud, Fake KYC, Job Task Scam, or No Obvious Scam Signals."),
            "summary" to Schema.string(description = "One-sentence summary of the assessment."),
            "simpleExplanation" to Schema.string(description = "Plain, non-technical explanation suitable for Indian teenagers, adults, and elderly users."),
            "suspiciousPhrases" to Schema.array(
                items = Schema.string(),
                description = "Exact suspicious words, phrases, or red flags identified in the input."
            ),
            "suspiciousUrls" to Schema.array(
                items = Schema.string(),
                description = "Any suspicious URLs, domains, UPI handles, or phone numbers found."
            ),
            "scammerGoal" to Schema.string(description = "What a potential scammer is trying to obtain or trick the user into doing."),
            "nextSteps" to Schema.array(
                items = Schema.string(),
                description = "Clear, practical next steps for safety in India (e.g., do not pay, verify official portal, call 1930 if money lost)."
            ),
            "extractedText" to Schema.string(description = "Text extracted via OCR if an image was provided, otherwise empty string."),
            "shouldAvoidClicking" to Schema.boolean(description = "True if the user should avoid clicking links or downloading files."),
            "shouldAvoidReplying" to Schema.boolean(description = "True if the user should avoid replying."),
            "shouldAvoidCalling" to Schema.boolean(description = "True if the user should avoid calling numbers inside the message."),
            "shouldAvoidSharingInfo" to Schema.boolean(description = "True if the user should avoid sharing OTPs, PINs, Aadhaar, or bank info."),
            "verificationChecklist" to Schema.array(
                items = Schema.string(),
                description = "Checklist of things the user can verify independently on official websites or apps."
            )
        ),
        optionalProperties = listOf("extractedText", "verificationChecklist")
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = scamAnalysisSchema
                temperature = 0.2f
            },
            systemInstruction = content {
                text(
                    "You are ScamShield India's AI cybersecurity analyzer protecting Indian citizens from digital fraud. " +
                        "Detect Indian scam patterns including fake Aadhaar/PAN updates, PM/government subsidy claims, " +
                        "fake electricity disconnection, fake traffic e-challans, fake Police/CBI/ED/Customs/TRAI 'Digital Arrest' threats, " +
                        "fake bank suspension/KYC, OTP theft, UPI collect/QR receive scams, fake courier parcel seizures, " +
                        "work-from-home/Telegram task scams, fake job/internship fees, fake loan/insurance, crypto/stock scams, " +
                        "WhatsApp impersonation, sextortion, romance scams, and .apk malware links. " +
                        "Understand English, Hindi, Marathi, Hinglish, and transliterated Marathi. " +
                        "CRITICAL RULES: Never guarantee a message, screenshot, or link is 100% safe — use 'SAFE_LOOKING' at most and note AI is an assessment, not proof. " +
                        "Never assume a link is safe just because it uses HTTPS. " +
                        "Remind users that UPI PIN is only to send money, never to receive money."
                )
            }
        )
    }

    private val assistantModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.4f
            },
            systemInstruction = content {
                text(
                    "You are Shield AI, a friendly, calm, practical cybersecurity safety assistant in ScamShield India. " +
                        "Rules: " +
                        "1. Respond in simple, empathetic, non-technical language easy for teenagers, adults, and elderly users. " +
                        "2. Support English, Hindi, and Marathi cleanly matching the user's preferred language or prompt language. " +
                        "3. Prioritize immediate safety steps first (numbered or short bullets). " +
                        "4. NEVER ask for OTPs, UPI PINs, banking passwords, CVV, or full Aadhaar/PAN numbers. " +
                        "5. NEVER claim to be a police officer, RBI official, or government authority. " +
                        "6. When financial fraud or money loss is mentioned, immediately highlight India's 1930 Cyber Helpline, https://cybercrime.gov.in/, and contacting the bank immediately."
                )
            }
        )
    }

    suspend fun analyzeTextOrContext(
        scanType: ScanType,
        contextDetails: String,
        platformOrCategory: String,
        language: AppLanguage
    ): Result<ScamAnalysisResult> {
        return try {
            val prompt = buildString {
                append("Analyze the following ${scanType.label} input for cyber fraud or scam indicators in India.\n")
                if (platformOrCategory.isNotBlank()) {
                    append("Source/Context: $platformOrCategory\n")
                }
                append("Language instruction: ${language.aiPromptInstruction}\n")
                append("Input content:\n\"\"\"\n$contextDetails\n\"\"\"")
            }

            val response = withTimeout(AI_TIMEOUT_MS) {
                structuredModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("AI returned an empty response.")
            Result.success(parseScamAnalysis(jsonText))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI analysis timed out", e)
            Result.failure(Exception("Analysis timed out after 60 seconds. Please check your connection and try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI analysis failed", e)
            Result.failure(e)
        }
    }

    suspend fun analyzeImageWithContext(
        scanType: ScanType,
        bitmap: Bitmap,
        additionalText: String,
        sourceCategory: String,
        language: AppLanguage
    ): Result<ScamAnalysisResult> {
        return try {
            val scaledBitmap = scaleBitmapForAi(bitmap, maxDimension = 1280)
            val prompt = buildString {
                append("Perform OCR text extraction and scam/phishing visual analysis on this ${scanType.label} screenshot.\n")
                if (sourceCategory.isNotBlank()) {
                    append("Screenshot Type: $sourceCategory\n")
                }
                if (additionalText.isNotBlank()) {
                    append("User notes/context: $additionalText\n")
                }
                if (scanType == ScanType.UPI) {
                    append("Important: Even if a payment screenshot looks authentic, state clearly that screenshots can be forged and advise verifying directly inside the official bank/UPI app balance. Explain that UPI PIN is only to send money.\n")
                }
                append("Put all readable extracted text in extractedText. ${language.aiPromptInstruction}")
            }

            val response = withTimeout(AI_TIMEOUT_MS) {
                structuredModel.generateContent(
                    content {
                        image(scaledBitmap)
                        text(prompt)
                    }
                )
            }
            val jsonText = response.text ?: throw IllegalStateException("AI returned an empty response.")
            Result.success(parseScamAnalysis(jsonText))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Multimodal AI analysis timed out", e)
            Result.failure(Exception("Screenshot analysis timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Multimodal AI analysis failed", e)
            Result.failure(e)
        }
    }

    fun streamShieldAiReply(
        userMessage: String,
        historyPairs: List<Pair<Boolean, String>>,
        language: AppLanguage
    ): Flow<String> = flow {
        val chatHistory = historyPairs.takeLast(8).map { (isUser, msg) ->
            content(role = if (isUser) "user" else "model") {
                text(msg)
            }
        }
        val chat = assistantModel.startChat(history = chatHistory)
        val promptWithLang = "${language.aiPromptInstruction}\n\nUser question: $userMessage"
        val accumulated = StringBuilder()
        withTimeout(AI_TIMEOUT_MS) {
            chat.sendMessageStream(promptWithLang).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    accumulated.append(chunkText)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Shield AI returned an empty response.")
        }
    }

    private fun parseScamAnalysis(jsonString: String): ScamAnalysisResult {
        val json = JSONObject(jsonString)
        return ScamAnalysisResult(
            riskLevel = RiskLevel.fromRaw(json.optString("riskLevel", "CAUTION")),
            riskScore = json.optInt("riskScore", 50).coerceIn(0, 100),
            scamType = json.optString("scamType", "Suspicious Content"),
            summary = json.optString("summary", "AI scan completed."),
            simpleExplanation = json.optString("simpleExplanation", ""),
            suspiciousPhrases = json.optJsonStringList("suspiciousPhrases"),
            suspiciousUrls = json.optJsonStringList("suspiciousUrls"),
            scammerGoal = json.optString("scammerGoal", ""),
            nextSteps = json.optJsonStringList("nextSteps"),
            extractedText = json.optString("extractedText", ""),
            shouldAvoidClicking = json.optBoolean("shouldAvoidClicking", true),
            shouldAvoidReplying = json.optBoolean("shouldAvoidReplying", true),
            shouldAvoidCalling = json.optBoolean("shouldAvoidCalling", true),
            shouldAvoidSharingInfo = json.optBoolean("shouldAvoidSharingInfo", true),
            verificationChecklist = json.optJsonStringList("verificationChecklist")
        )
    }

    private fun JSONObject.optJsonStringList(key: String): List<String> {
        val arr = optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { idx ->
            arr.optString(idx).takeIf { it.isNotBlank() }
        }
    }

    private fun scaleBitmapForAi(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val targetW: Int
        val targetH: Int
        if (width >= height) {
            targetW = maxDimension
            targetH = (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            targetH = maxDimension
            targetW = (maxDimension * ratio).toInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
