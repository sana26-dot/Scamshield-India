package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.AppLanguage
import com.example.model.CommunityReport
import com.example.model.ReportCategory
import com.example.model.ScamAnalysisResult
import com.example.model.ScanRecord
import com.example.model.ScanType
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ScamRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        displayName: String,
        preferredLanguage: AppLanguage,
        phoneNumber: String? = null
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = displayName.trim().ifBlank { "ScamShield User" }.take(80)
                val cleanPhone = phoneNumber?.trim()?.take(16)?.ifBlank { null }
                val payload = buildMap<String, Any> {
                    put("userId", uid)
                    put("displayName", safeName)
                    if (cleanPhone != null) put("indianPhoneNumber", cleanPhone)
                    put("preferredLanguage", preferredLanguage.code)
                    put("callScreeningEnabled", false)
                    put("privacyMinimizeStorage", true)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        displayName: String,
        indianPhoneNumber: String?,
        preferredLanguage: AppLanguage,
        callScreeningEnabled: Boolean,
        privacyMinimizeStorage: Boolean
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val safeName = displayName.trim().ifBlank { "ScamShield User" }.take(80)
            val cleanedPhone = indianPhoneNumber?.trim()?.take(16)
            val updates = mutableMapOf<String, Any>(
                "displayName" to safeName,
                "preferredLanguage" to preferredLanguage.code,
                "callScreeningEnabled" to callScreeningEnabled,
                "privacyMinimizeStorage" to privacyMinimizeStorage,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!cleanedPhone.isNullOrEmpty()) {
                updates["indianPhoneNumber"] = cleanedPhone
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeScanHistory(userId: String): Flow<List<ScanRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch" }
        val path = "users/$uid/scanHistory"
        emitAll(
            db.collection("users").document(uid).collection("scanHistory")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ScanRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveScanRecord(
        scanType: ScanType,
        analysis: ScamAnalysisResult,
        rawInputPreview: String,
        language: AppLanguage,
        minimizeStorage: Boolean
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/scanHistory"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("scanHistory").document()
            docPath = docRef.path

            val safePreview = if (minimizeStorage || rawInputPreview.isBlank()) {
                null
            } else {
                rawInputPreview.trim().take(240)
            }

            val payload = buildMap<String, Any> {
                put("userId", uid)
                put("scanType", scanType.name)
                put("riskLevel", analysis.riskLevel.apiValue)
                put("riskScore", analysis.riskScore.coerceIn(0, 100))
                put("scamCategory", analysis.scamType.ifBlank { "General Assessment" }.take(100))
                put("summary", analysis.summary.ifBlank { analysis.simpleExplanation }.take(400))
                if (safePreview != null) put("inputPreview", safePreview)
                put("explanation", analysis.simpleExplanation.ifBlank { analysis.summary }.take(1500))
                put("indicators", analysis.suspiciousPhrases.take(10).map { it.take(200) })
                put("recommendedActions", analysis.nextSteps.take(10).map { it.take(240) })
                put("detectedUrls", analysis.suspiciousUrls.take(10).map { it.take(300) })
                put("language", language.code)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteScanRecord(scanId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/scanHistory/$scanId"
        return try {
            val uid = requireUserId()
            require(scanId.isNotBlank()) { "Scan ID must not be blank" }
            val docRef = db.collection("users").document(uid).collection("scanHistory").document(scanId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun clearScanHistory(): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/scanHistory"
        return try {
            val uid = requireUserId()
            val colRef = db.collection("users").document(uid).collection("scanHistory")
            path = colRef.path
            val snapshot = colRef.get().await()
            val batch = db.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observePublishedCommunityReports(): Flow<List<CommunityReport>> = flow {
        val uid = requireUserId()
        val path = "communityReports"
        emitAll(
            db.collection(path)
                .whereEqualTo("moderationStatus", "published")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CommunityReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun lookupReportsForTarget(normalizedTarget: String): Result<List<CommunityReport>> {
        val path = "communityReports"
        return try {
            requireUserId()
            val cleanTarget = normalizedTarget.trim().take(160)
            if (cleanTarget.isBlank()) return Result.success(emptyList())
            val snapshot = db.collection(path)
                .whereEqualTo("moderationStatus", "published")
                .whereEqualTo("reportedTarget", cleanTarget)
                .get()
                .await()
            val results = snapshot.toObjects(
                CommunityReport::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            Result.success(results)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun submitCommunityReport(
        category: ReportCategory,
        description: String,
        reportedTarget: String?,
        incidentDate: String
    ): Result<Unit> {
        var docPath = "communityReports"
        return try {
            val uid = requireUserId()
            val cleanDesc = description.trim().take(600)
            require(cleanDesc.length >= 10) { "Description must be at least 10 characters." }
            val cleanDate = incidentDate.trim().ifBlank { "2026-11-20" }.take(20)
            val cleanTarget = reportedTarget?.trim()?.take(160)?.ifBlank { null }

            val docRef = db.collection("communityReports").document()
            docPath = docRef.path

            val payload = buildMap<String, Any> {
                put("userId", uid)
                put("category", category.name)
                put("description", cleanDesc)
                if (cleanTarget != null) put("reportedTarget", cleanTarget)
                put("incidentDate", cleanDate)
                put("moderationStatus", "published")
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }

            docRef.set(payload).await()
            Log.d("ScamRepository", "Submitted community report ${docRef.id}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCommunityReport(reportId: String): Result<Unit> {
        var docPath = "communityReports/$reportId"
        return try {
            requireUserId()
            require(reportId.isNotBlank()) { "Report ID must not be blank" }
            val docRef = db.collection("communityReports").document(reportId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
