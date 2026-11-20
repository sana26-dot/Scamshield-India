package com.example.security

import android.net.Uri
import com.example.model.IndiaScamAdvisory
import com.example.model.RiskLevel

data class PhoneFormatAnalysis(
    val normalizedNumber: String,
    val isValidIndianMobileFormat: Boolean,
    val formatClassification: String,
    val observations: List<String>
)

data class UrlStructuralReport(
    val normalizedUrl: String,
    val host: String,
    val warnings: List<String>,
    val usesHttps: Boolean,
    val isApkLink: Boolean,
    val isShortener: Boolean,
    val hasIpHost: Boolean
)

object UrlAndPhoneHeuristics {

    private val shortenerDomains = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "cutt.ly", "rb.gy",
        "is.gd", "ow.ly", "shorturl.at", "tiny.cc", "rebrand.ly"
    )

    private val officialIndianBrands = listOf(
        "sbi" to "onlinesbi.sbi",
        "hdfc" to "hdfcbank.com",
        "icici" to "icicibank.com",
        "axis" to "axisbank.com",
        "kotak" to "kotak.com",
        "uidai" to "uidai.gov.in",
        "aadhaar" to "uidai.gov.in",
        "incometax" to "incometax.gov.in",
        "indiapost" to "indiapost.gov.in",
        "parivahan" to "parivahan.gov.in",
        "echallan" to "echallan.parivahan.gov.in",
        "npci" to "npci.org.in",
        "cybercrime" to "cybercrime.gov.in"
    )

    fun normalizeIndianPhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.length == 12 && digits.startsWith("91") -> "+$digits"
            digits.length == 11 && digits.startsWith("0") -> "+91${digits.drop(1)}"
            digits.length == 10 && digits.firstOrNull() in listOf('6', '7', '8', '9') -> "+91$digits"
            else -> raw.trim()
        }
    }

    fun isValidIndianMobileInput(raw: String): Boolean {
        val digits = raw.filter { it.isDigit() }
        val ten = when {
            digits.length == 12 && digits.startsWith("91") -> digits.drop(2)
            digits.length == 11 && digits.startsWith("0") -> digits.drop(1)
            else -> digits
        }
        return ten.length == 10 && ten.firstOrNull() in listOf('6', '7', '8', '9')
    }

    fun analyzeIndianPhoneFormat(raw: String): PhoneFormatAnalysis {
        val cleaned = raw.trim()
        val digits = cleaned.filter { it.isDigit() }
        val normalized = normalizeIndianPhone(cleaned)
        val observations = mutableListOf<String>()

        val tenDigits = when {
            digits.length == 12 && digits.startsWith("91") -> digits.drop(2)
            digits.length == 11 && digits.startsWith("0") -> digits.drop(1)
            else -> digits
        }

        val isStandardMobile = tenDigits.length == 10 && tenDigits.firstOrNull() in listOf('6', '7', '8', '9')
        val classification = when {
            digits.startsWith("140") && digits.length == 10 -> {
                observations.add("Starts with 140 — TRAI-assigned telemarketing / promotional series in India.")
                "TRAI 140 Telemarketing Series"
            }
            digits.startsWith("160") && digits.length >= 10 -> {
                observations.add("Starts with 160 — TRAI service / transactional calling series (verify caller via official bank app).")
                "TRAI 160 Service Series"
            }
            digits.startsWith("1800") || digits.startsWith("1860") -> {
                observations.add("Matches Indian toll-free / helpline prefix (1800/1860). Still verify on the official company website—never trust numbers from SMS forwards.")
                "Toll-Free / Helpline Format"
            }
            isStandardMobile -> {
                observations.add("Standard 10-digit Indian mobile number format (+91-${tenDigits.take(2)}XXXXXXXX).")
                observations.add("Indian banks, police, CBI, TRAI, or courts do NOT use personal 10-digit mobile numbers to demand money or threaten arrest.")
                "Indian Mobile (+91)"
            }
            cleaned.startsWith("+") && !digits.startsWith("91") -> {
                observations.add("International country code detected. Be cautious of unexpected +92, +1, +44, or WhatsApp VoIP calls claiming to be Indian police or customs.")
                "International Number Format"
            }
            else -> {
                observations.add("Does not match a standard 10-digit Indian mobile format.")
                "Non-standard / Short Code"
            }
        }

        observations.add("Note: Phone number format alone never proves whether a caller is genuine or fraudulent.")

        return PhoneFormatAnalysis(
            normalizedNumber = normalized,
            isValidIndianMobileFormat = isStandardMobile,
            formatClassification = classification,
            observations = observations
        )
    }

    fun inspectUrl(rawUrl: String): UrlStructuralReport {
        val trimmed = rawUrl.trim()
        val withScheme = if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
        val parsed = runCatching { Uri.parse(withScheme) }.getOrNull()
        val host = parsed?.host?.lowercase().orEmpty()
        val usesHttps = trimmed.startsWith("https://", ignoreCase = true)
        val isApkLink = trimmed.lowercase().contains(".apk")
        val isShortener = shortenerDomains.any { host == it || host.endsWith(".$it") }
        val hasIpHost = host.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}$"))

        val warnings = mutableListOf<String>()

        if (!usesHttps && trimmed.startsWith("http://", ignoreCase = true)) {
            warnings.add("Uses unencrypted HTTP instead of HTTPS — credentials or data could be intercepted.")
        } else {
            warnings.add("Reminder: Having HTTPS (padlock icon) does NOT mean a website is safe; phishing sites also use HTTPS.")
        }

        if (isApkLink) {
            warnings.add("Links to an .apk Android installer file — high malware/banking trojan risk outside Google Play.")
        }
        if (isShortener) {
            warnings.add("Uses a URL shortener ($host) which hides the real destination website.")
        }
        if (hasIpHost) {
            warnings.add("Uses a raw IP address ($host) instead of a registered domain name.")
        }
        if (host.startsWith("xn--") || host.contains(".xn--")) {
            warnings.add("Contains Punycode (xn--) characters that can imitate trusted domain letters.")
        }
        if (trimmed.contains("@")) {
            warnings.add("Contains '@' symbol inside URL which can obscure the actual destination host.")
        }
        if (host.count { it == '.' } >= 4) {
            warnings.add("Excessive subdomains ($host) — common phishing trick to mimic trusted brands.")
        }

        officialIndianBrands.forEach { (keyword, official) ->
            if (host.contains(keyword) && host != official && !host.endsWith(".$official")) {
                warnings.add("Mentions '$keyword' in '$host', which is NOT the official domain ($official).")
            }
        }

        return UrlStructuralReport(
            normalizedUrl = withScheme,
            host = host.ifBlank { trimmed },
            warnings = warnings,
            usesHttps = usesHttps,
            isApkLink = isApkLink,
            isShortener = isShortener,
            hasIpHost = hasIpHost
        )
    }

    val commonIndiaAdvisories: List<IndiaScamAdvisory> = listOf(
        IndiaScamAdvisory(
            id = "digital_arrest",
            title = "Digital Arrest Video Call Scam",
            severity = RiskLevel.CRITICAL_RISK,
            category = "Fake Police / CBI / TRAI",
            summary = "Scammers pose as CBI, ED, Customs, or TRAI officers on WhatsApp/Skype video calls claiming your Aadhaar or parcel is linked to crime and demand money for 'verification'.",
            redFlags = listOf(
                "Indian law enforcement NEVER conducts 'digital arrest' on video calls.",
                "Never transfer money to 'RBI/Supreme Court escrow accounts' to prove innocence.",
                "Hang up immediately and verify via 1930."
            ),
            safetyTip = "No government agency arrests citizens over WhatsApp or demands bail via UPI/RTGS."
        ),
        IndiaScamAdvisory(
            id = "upi_collect_qr",
            title = "UPI Collect Request & QR Receive Trap",
            severity = RiskLevel.HIGH_RISK,
            category = "UPI Payment Fraud",
            summary = "Fraudsters posing as buyers (OLX/marketplace) or army/merchant staff send a QR code or 'Pay' collect request claiming you must enter your UPI PIN to receive money.",
            redFlags = listOf(
                "Entering your UPI PIN ALWAYS sends money out — NEVER receives money.",
                "Receiving money via UPI requires zero PIN entry.",
                "Never install screen-sharing apps (AnyDesk, TeamViewer) for refunds."
            ),
            safetyTip = "UPI PIN = Money Out. Cancel unexpected collect requests right away."
        ),
        IndiaScamAdvisory(
            id = "electricity_kyc",
            title = "Electricity Disconnection & Bank KYC SMS",
            severity = RiskLevel.HIGH_RISK,
            category = "SMS / WhatsApp Phishing",
            summary = "Messages warning power will be cut at 9:30 PM tonight or bank KYC will freeze today, urging you to call a personal 10-digit mobile number or click a short link.",
            redFlags = listOf(
                "Personal 10-digit mobile number given as 'Electricity Officer'.",
                "Shortened links (bit.ly / cutt.ly) or .apk downloads.",
                "Extreme urgency threatening cutoff tonight."
            ),
            safetyTip = "Check your official DISCOM or bank app directly—never call numbers from panic SMS."
        ),
        IndiaScamAdvisory(
            id = "apk_wedding_challan",
            title = "Fake e-Challan & Wedding Invite .APK Malware",
            severity = RiskLevel.CRITICAL_RISK,
            category = "Malware / APK Theft",
            summary = "WhatsApp messages sending 'Traffic_eChallan.apk' or 'Wedding_Invitation.apk'. Installing grants scammers access to SMS OTPs and banking apps.",
            redFlags = listOf(
                "File ends in .apk instead of PDF or official parivahan.gov.in link.",
                "Requests 'Install unknown apps' or Accessibility permissions."
            ),
            safetyTip = "Never install .apk files received over WhatsApp, SMS, or Telegram."
        ),
        IndiaScamAdvisory(
            id = "task_job_scam",
            title = "Telegram YouTube/Review Task & Fake Job Scam",
            severity = RiskLevel.HIGH_RISK,
            category = "Fake Job / Investment",
            summary = "Work-from-home offers paying ₹500–₹3,000 for liking videos or rating hotels, then forcing 'prepaid VIP recharge tasks' where deposits get locked.",
            redFlags = listOf(
                "Asking for registration fee, security deposit, or 'prepaid task recharge'.",
                "Recruiter communicates only on Telegram/WhatsApp with Gmail address."
            ),
            safetyTip = "Genuine employers never ask candidates to pay money to get salary or unlock tasks."
        )
    )

    val dailySafetyTips: List<String> = listOf(
        "You NEVER need to enter your UPI PIN or scan a QR code to receive money in India.",
        "Police, CBI, Customs, and TRAI never place citizens under 'Digital Arrest' on video calls.",
        "Never install .apk files sent on WhatsApp as e-Challan, bank KYC, or wedding cards.",
        "Report suspicious scam calls & SMS on Sanchar Saathi Chakshu (sancharsaathi.gov.in).",
        "If financial cyber fraud happens, call 1930 immediately—the first hour ('Golden Hour') helps freeze funds.",
        "Do not trust a link just because it has HTTPS or a padlock; verify the exact domain spelling."
    )
}
