package com.pockt.app.notifications

import com.pockt.app.data.Categories
import com.pockt.app.data.TransactionDirection
import java.security.MessageDigest
import java.util.Locale

data class ParsedPayment(
    val amountPaise: Long,
    val merchant: String,
    val category: String,
    val direction: TransactionDirection,
    val fingerprint: String,
)

object PaymentParser {
    val supportedPackages = setOf(
        "com.google.android.apps.nbu.paisa.user",
        "net.one97.paytm",
        "com.phonepe.app",
        "in.org.npci.upiapp",
    )

    private val amountPattern = Regex("(?:₹|rs\\.?|inr)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    private val merchantPatterns = listOf(
        Regex("(?:paid|sent|payment of)(?:\\s+₹?[0-9,.]+)?\\s+(?:to|at)\\s+([^.!\\n]+)", RegexOption.IGNORE_CASE),
        Regex("(?:received)(?:\\s+₹?[0-9,.]+)?\\s+from\\s+([^.!\\n]+)", RegexOption.IGNORE_CASE),
    )
    private val successWords = listOf("paid", "payment successful", "sent", "debited", "received", "refund")
    private val failureWords = listOf("failed", "declined", "pending", "processing", "couldn't", "cancelled")

    fun parse(packageName: String, title: String?, body: String?, postedAt: Long): ParsedPayment? {
        if (packageName !in supportedPackages) return null
        val text = listOfNotNull(title, body).joinToString(" ").replace(Regex("\\s+"), " ").trim()
        val lower = text.lowercase(Locale.ROOT)
        if (text.isBlank() || failureWords.any(lower::contains) || successWords.none(lower::contains)) return null
        val match = amountPattern.find(text) ?: return null
        val rupees = match.groupValues[1].replace(",", "").toBigDecimalOrNull() ?: return null
        val paise = rupees.movePointRight(2).longValueExactOrNull() ?: return null
        if (paise <= 0) return null
        val direction = when {
            "refund" in lower -> TransactionDirection.REFUND
            "received" in lower || "credited" in lower -> TransactionDirection.INCOME
            else -> TransactionDirection.EXPENSE
        }
        val merchant = merchantPatterns.firstNotNullOfOrNull { it.find(text)?.groupValues?.getOrNull(1)?.trim() }
            ?.take(60)?.ifBlank { null } ?: when (direction) {
                TransactionDirection.REFUND -> "Refund"
                TransactionDirection.INCOME -> "Money received"
                else -> "UPI payment"
            }
        val timeBucket = postedAt / 60_000L
        val rawFingerprint = "$packageName|$paise|${merchant.lowercase()}|$direction|$timeBucket"
        return ParsedPayment(paise, merchant, Categories.guess(merchant), direction, sha256(rawFingerprint))
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun java.math.BigDecimal.longValueExactOrNull(): Long? = try { longValueExact() } catch (_: ArithmeticException) { null }
}
