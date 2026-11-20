package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.security.UrlAndPhoneHeuristics
import com.example.ui.theme.BrightCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.LavenderGlow
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RiskCriticalColor
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private enum class AuthMethodTab { PHONE_OTP, GOOGLE }

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val cred = credentialManager.getCredential(activityContext, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type received.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: NoCredentialException) {
            Log.w("Auth", "No Google account available on device", e)
            onAuthError("No Google account is signed in on this device/emulator. Add a Google account or use Guest / Demo Mode below.")
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed. You can use Guest / Demo Mode below.")
        }
    }
}

fun signOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    selectedLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onAuthSuccess: () -> Unit,
    onEnterGuestMode: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var selectedTab by remember { mutableStateOf(AuthMethodTab.PHONE_OTP) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showEmergencySheet by remember { mutableStateOf(false) }

    // Indian Phone OTP State
    var phoneDigitsInput by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var otpCodeInput by remember { mutableStateOf("") }

    val strings = AppStrings.forLanguage(selectedLanguage)

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    fun sendPhoneOtp() {
        val digitsOnly = phoneDigitsInput.filter { it.isDigit() }.let { d ->
            when {
                d.length == 12 && d.startsWith("91") -> d.drop(2)
                d.length == 11 && d.startsWith("0") -> d.drop(1)
                else -> d
            }
        }
        if (!UrlAndPhoneHeuristics.isValidIndianMobileInput(digitsOnly)) {
            errorMessage = "Enter a valid 10-digit Indian mobile number (starting with 6, 7, 8, or 9)."
            return
        }
        if (activity == null) {
            errorMessage = "Unable to launch verification from this context. Try Google Sign-In or Guest Mode."
            return
        }

        val e164Phone = "+91$digitsOnly"
        errorMessage = null
        infoMessage = null
        isLoading = true

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                scope.launch {
                    try {
                        Firebase.auth.signInWithCredential(credential).await()
                        isLoading = false
                        onAuthSuccess()
                    } catch (e: Exception) {
                        isLoading = false
                        errorMessage = e.localizedMessage ?: "Phone sign-in failed."
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                isLoading = false
                Log.w("Auth", "Phone OTP verification failed", e)
                val code = (e as? FirebaseAuthException)?.errorCode
                errorMessage = when (code) {
                    "ERROR_OPERATION_NOT_ALLOWED", "CONFIGURATION_NOT_FOUND" ->
                        "Phone OTP sign-in is not yet enabled in the Firebase Console. Use Google Sign-In or continue in Guest / Demo Mode below."
                    else ->
                        "Could not send SMS OTP on this device (${e.localizedMessage ?: "Play Integrity / SMS unavailable"}). You can use Google Sign-In or Guest / Demo Mode below."
                }
            }

            override fun onCodeSent(
                verId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                isLoading = false
                verificationId = verId
                infoMessage = "OTP sent to $e164Phone. Enter the 6-digit code below."
            }
        }

        val options = PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(e164Phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtpCode() {
        val verId = verificationId ?: return
        val code = otpCodeInput.filter { it.isDigit() }
        if (code.length < 6) {
            errorMessage = "Please enter the 6-digit OTP sent to your mobile."
            return
        }
        errorMessage = null
        isLoading = true
        scope.launch {
            try {
                val credential = PhoneAuthProvider.getCredential(verId, code)
                Firebase.auth.signInWithCredential(credential).await()
                isLoading = false
                onAuthSuccess()
            } catch (e: Exception) {
                isLoading = false
                Log.w("Auth", "OTP verification failed", e)
                errorMessage = e.localizedMessage ?: "Invalid or expired OTP. Please try again."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(max = 560.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Language selector row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.appTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = selectedLanguage == lang,
                            onClick = { onSelectLanguage(lang) },
                            label = {
                                Text(
                                    text = lang.nativeLabel,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("auth_lang_${lang.code}")
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Hero Shield Art
            Box(
                modifier = Modifier
                    .size(164.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(listOf(ElectricViolet, BrightCyan, LavenderGlow)),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_welcome_shield),
                    contentDescription = "ScamShield India AI protection illustration",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = strings.signInTitle,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = strings.signInSubtitle,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeaturePill("AI Scam Scan")
                FeaturePill("UPI & QR Guard")
                FeaturePill("Call Shield")
                FeaturePill("1930 Cyber Help")
            }

            Spacer(Modifier.height(18.dp))

            // Login Card (Phone OTP + Google Sign-In + Guest Demo Mode)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedTab == AuthMethodTab.PHONE_OTP,
                            onClick = {
                                selectedTab = AuthMethodTab.PHONE_OTP
                                errorMessage = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {
                                Icon(
                                    Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("auth_tab_phone")
                        ) {
                            Text(strings.phoneLoginTab, maxLines = 1)
                        }
                        SegmentedButton(
                            selected = selectedTab == AuthMethodTab.GOOGLE,
                            onClick = {
                                selectedTab = AuthMethodTab.GOOGLE
                                errorMessage = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("auth_tab_google")
                        ) {
                            Text(strings.googleLoginTab, maxLines = 1)
                        }
                    }

                    if (selectedTab == AuthMethodTab.PHONE_OTP) {
                        if (verificationId == null) {
                            OutlinedTextField(
                                value = phoneDigitsInput,
                                onValueChange = { raw ->
                                    phoneDigitsInput = raw.filter { it.isDigit() }.take(10)
                                },
                                label = { Text(strings.enterPhoneLabel) },
                                prefix = { Text("+91  ", fontWeight = FontWeight.Bold) },
                                placeholder = { Text("9876543210") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("phone_number_input")
                            )

                            Button(
                                onClick = { sendPhoneOtp() },
                                enabled = !isLoading && phoneDigitsInput.length >= 10,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricViolet,
                                    contentColor = PureWhite
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("send_otp_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = PureWhite,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Sms, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.sendOtpButton, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = otpCodeInput,
                                onValueChange = { raw ->
                                    otpCodeInput = raw.filter { it.isDigit() }.take(6)
                                },
                                label = { Text(strings.enterOtpLabel) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("otp_code_input")
                            )

                            Button(
                                onClick = { verifyOtpCode() },
                                enabled = !isLoading && otpCodeInput.length == 6,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricViolet,
                                    contentColor = PureWhite
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("verify_otp_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = PureWhite,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Text(strings.verifyOtpButton, style = MaterialTheme.typography.titleMedium)
                                }
                            }

                            TextButton(
                                onClick = {
                                    verificationId = null
                                    otpCodeInput = ""
                                    infoMessage = null
                                    errorMessage = null
                                }
                            ) {
                                Text(strings.resendOtpButton)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                errorMessage = null
                                infoMessage = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = { isLoading = false }
                                )
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricViolet,
                                contentColor = PureWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = PureWhite,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = strings.signInWithGoogle,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }

                    if (infoMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = infoMessage!!,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    // Guest / Demo Mode option (local session without unauthenticated Firestore calls)
                    OutlinedButton(
                        onClick = onEnterGuestMode,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("guest_demo_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = strings.continueGuestDemo,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Text(
                        text = strings.guestModeNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.privacyNoticeShort,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            OutlinedButton(
                onClick = { showEmergencySheet = true },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskCriticalColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("auth_emergency_help_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = strings.emergencyWithoutLogin,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    if (showEmergencySheet) {
        ModalBottomSheet(onDismissRequest = { showEmergencySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Immediate India Cyber Fraud Steps",
                    style = MaterialTheme.typography.headlineSmall,
                    color = RiskCriticalColor
                )
                val steps = listOf(
                    "1. Contact your bank or UPI app support immediately to freeze the transaction.",
                    "2. Call 1930 promptly (National Cyber Crime Helpline) within the Golden Hour.",
                    "3. File a report at https://cybercrime.gov.in/.",
                    "4. Save screenshots, UTR / transaction IDs, and caller numbers.",
                    "5. Change compromised passwords and block affected cards.",
                    "6. Never pay extra money to anyone promising to 'recover' lost funds."
                )
                steps.forEach { step ->
                    Text(text = step, style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://cybercrime.gov.in/")))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Portal")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun FeaturePill(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
