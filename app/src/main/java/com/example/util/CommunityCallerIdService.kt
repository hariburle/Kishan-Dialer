package com.example.util

data class CommunityCallerInfo(
    val phoneNumber: String,
    val name: String,
    val category: String,
    val isVerified: Boolean = true,
    val verificationType: String = "Verified Business", // "Verified Business", "Community Identified", "High Spam Risk"
    val spamScore: Int = 0, // 0 to 100
    val location: String = "United States",
    val defaultCallReason: String? = null
)

object CommunityCallerIdService {

    // Curated community crowd-sourced database for businesses, services & telemarketers
    private val directory = listOf(
        CommunityCallerInfo(
            phoneNumber = "18005550100",
            name = "Corporate Office IVR",
            category = "Enterprise Services",
            isVerified = true,
            verificationType = "Verified Business",
            spamScore = 0,
            location = "San Francisco, CA",
            defaultCallReason = "Automated Office Switchboard"
        ),
        CommunityCallerInfo(
            phoneNumber = "5550199",
            name = "Apartment Intercom",
            category = "Access Control & Security",
            isVerified = true,
            verificationType = "Verified System",
            spamScore = 0,
            location = "Local Gateway",
            defaultCallReason = "Visitor / Gate Buzzer"
        ),
        CommunityCallerInfo(
            phoneNumber = "18005550199",
            name = "National Robo-Promotions",
            category = "Robocall / Telemarketing",
            isVerified = false,
            verificationType = "High Spam Risk",
            spamScore = 92,
            location = "Toll-Free Network",
            defaultCallReason = "Automated Sales Pitch"
        ),
        CommunityCallerInfo(
            phoneNumber = "18885550144",
            name = "Suspected Tax Impersonation",
            category = "Financial Scam / Fraud",
            isVerified = false,
            verificationType = "High Spam Risk",
            spamScore = 98,
            location = "Toll-Free Network",
            defaultCallReason = "Urgent Payment Threat"
        ),
        CommunityCallerInfo(
            phoneNumber = "19005550123",
            name = "Premium Rate Tele-Survey",
            category = "Marketing & Polling",
            isVerified = false,
            verificationType = "High Spam Risk",
            spamScore = 84,
            location = "Premium Line",
            defaultCallReason = "Unsolicited Survey"
        ),
        CommunityCallerInfo(
            phoneNumber = "18002752273",
            name = "Apple Support",
            category = "Customer Care",
            isVerified = true,
            verificationType = "Verified Business",
            spamScore = 1,
            location = "Cupertino, CA",
            defaultCallReason = "Technical Support Inquiry"
        ),
        CommunityCallerInfo(
            phoneNumber = "18004337300",
            name = "American Airlines",
            category = "Travel & Airlines",
            isVerified = true,
            verificationType = "Verified Business",
            spamScore = 2,
            location = "Fort Worth, TX",
            defaultCallReason = "Flight Status Update"
        ),
        CommunityCallerInfo(
            phoneNumber = "18009220204",
            name = "Verizon Customer Service",
            category = "Telecom & Wireless",
            isVerified = true,
            verificationType = "Verified Business",
            spamScore = 5,
            location = "New York, NY",
            defaultCallReason = "Account & Plan Service"
        ),
        CommunityCallerInfo(
            phoneNumber = "18003662255",
            name = "Amazon Logistics / Delivery",
            category = "Delivery & Shipping",
            isVerified = true,
            verificationType = "Verified Delivery",
            spamScore = 2,
            location = "Seattle, WA",
            defaultCallReason = "Package Arrival Notification"
        ),
        CommunityCallerInfo(
            phoneNumber = "18009359935",
            name = "Chase Bank Fraud Alerts",
            category = "Banking & Finance",
            isVerified = true,
            verificationType = "Verified Financial",
            spamScore = 1,
            location = "Columbus, OH",
            defaultCallReason = "Card Security Verification"
        )
    )

    private fun normalize(number: String): String {
        return number.replace(Regex("[^0-9+]"), "").trimStart('+', '1')
    }

    /**
     * Looks up any unknown incoming or dialed number against the community database.
     * Returns matching verified business, delivery service, or community spam score.
     */
    fun lookup(rawNumber: String): CommunityCallerInfo? {
        val clean = normalize(rawNumber)
        if (clean.isBlank()) return null

        // 1. Direct match
        directory.firstOrNull { normalize(it.phoneNumber) == clean }?.let { return it }

        // 2. Prefix matching for known toll-free or carrier ranges
        if (clean.startsWith("800") || clean.startsWith("888") || clean.startsWith("877") || clean.startsWith("866")) {
            return CommunityCallerInfo(
                phoneNumber = rawNumber,
                name = "Toll-Free Enterprise Line",
                category = "Customer Service / Infoline",
                isVerified = true,
                verificationType = "Community Identified",
                spamScore = 15,
                location = "Toll-Free North America",
                defaultCallReason = "Inbound Service Line"
            )
        }

        // 3. Pattern match for delivery drivers (common 555-01xx dispatch range)
        if (clean.contains("55501")) {
            return CommunityCallerInfo(
                phoneNumber = rawNumber,
                name = "Courier / Delivery Dispatch",
                category = "Logistics & Delivery",
                isVerified = true,
                verificationType = "Community Identified",
                spamScore = 4,
                location = "Local Delivery Network",
                defaultCallReason = "Direct Package Delivery"
            )
        }

        return null
    }
}
