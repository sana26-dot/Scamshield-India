package com.example.security

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScreenedCallEvent(
    val phoneNumber: String,
    val verificationNote: String,
    val timestampMillis: Long,
    val isVerificationFailed: Boolean
)

object CallScreeningStateHolder {
    private val _recentScreenedCall = MutableStateFlow<ScreenedCallEvent?>(null)
    val recentScreenedCall: StateFlow<ScreenedCallEvent?> = _recentScreenedCall.asStateFlow()

    fun recordEvent(event: ScreenedCallEvent) {
        _recentScreenedCall.value = event
    }

    fun isCallScreeningRoleHeld(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
        return roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
            roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }
}

/**
 * Privacy-respecting Android 10+ CallScreeningService.
 * Only evaluates unknown incoming call metadata exposed by Android Telecom without
 * recording calls or reading user contacts.
 */
class ScamCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        ) {
            val number = callDetails.handle?.schemeSpecificPart ?: "Unknown"
            val verificationFailed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                callDetails.callerNumberVerificationStatus == Connection.VERIFICATION_STATUS_FAILED
            } else {
                false
            }
            val note = when {
                verificationFailed -> "Carrier caller-ID verification failed (potential spoofing indicator)"
                else -> "Unknown caller not in device contacts — review carefully"
            }

            CallScreeningStateHolder.recordEvent(
                ScreenedCallEvent(
                    phoneNumber = number,
                    verificationNote = note,
                    timestampMillis = System.currentTimeMillis(),
                    isVerificationFailed = verificationFailed
                )
            )
        }

        // Allow call through transparently without silent rejection or call log tampering.
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSilenceCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, response)
    }
}
