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

data class ParseResult(
    val payment: ParsedPayment?,
    val reason: String,
) {
    val parsed: Boolean get() = payment != null
}

object PaymentParser {
    val supportedPackages = setOf(
        "com.google.android.apps.nbu.paisa.user",
        "net.one97.paytm",
        "com.phonepe.app",
        "in.org.npci.upiapp",
    )

    private val amountPattern = Regex("(?:₹|rs\\.?|inr)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    private val fallbackAmountPattern = Regex("(?:paid|sent|debited|payment(?:\\s+of)?|received|credited|refund(?:ed)?)\\D{0,18}([0-9][0-9,]*(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    private val merchantPatterns = listOf(
        Regex("(?:paid|sent|payment(?:\\s+of)?|debited)(?:\\s+(?:₹|rs\\.?|inr)?\\s*[0-9,.]+)?\\s+(?:to|at)\\s+([^.!\\n|]+)", RegexOption.IGNORE_CASE),
        Regex("(?:payment successful).*?(?:to|at)\\s+([^.!\\n|]+)", RegexOption.IGNORE_CASE),
        Regex("(?:received|credited)(?:\\s+(?:₹|rs\\.?|inr)?\\s*[0-9,.]+)?\\s+from\\s+([^.!\\n|]+)", RegexOption.IGNORE_CASE),
    )
    private val successWords = listOf("paid", "payment successful", "successful", "sent", "debited", "received", "credited", "refund")
    private val failureWords = listOf("failed", "declined", "pending", "processing", "couldn't", "cancelled", "reversed")

    fun parse(packageName: String, title: String?, body: String?, postedAt: Long): ParsedPayment? = inspect(packageName, title, body, postedAt).payment

    fun inspect(packageName: String, title: String?, body: String?, postedAt: Long): ParseResult {
        if (packageName !in supportedPackages) return ParseResult(null, "unsupported app")
        val text = listOfNotNull(title, body).joinToString(" ").replace(Regex("\\s+"), " ").trim()
        val lower = text.lowercase(Locale.ROOT)
        if (text.isBlank()) return ParseResult(null, "blank notification")
        if (failureWords.any(lower::contains)) return ParseResult(null, "failure or pending")
        if (successWords.none(lower::contains)) return ParseResult(null, "no success words")
        val match = amountPattern.find(text) ?: fallbackAmountPattern.find(text) ?: return ParseResult(null, "no amount")
        val rupees = match.groupValues[1].replace(",", "").toBigDecimalOrNull() ?: return ParseResult(null, "bad amount")
        val paise = rupees.movePointRight(2).longValueExactOrNull() ?: return ParseResult(null, "bad paise")
        if (paise <= 0) return ParseResult(null, "zero amount")
        val direction = when {
            "refund" in lower -> TransactionDirection.REFUND
            "received" in lower || "credited" in lower -> TransactionDirection.INCOME
            else -> TransactionDirection.EXPENSE
        }
        val merchant = merchantPatterns.firstNotNullOfOrNull { it.find(text)?.groupValues?.getOrNull(1)?.trim() }
            ?.replace(Regex("\\s+(on|via|using|at)\\s+.*$", RegexOption.IGNORE_CASE), "")
            ?.take(60)?.ifBlank { null } ?: when (direction) {
                TransactionDirection.REFUND -> "Refund"
                TransactionDirection.INCOME -> "Money received"
                else -> "UPI payment"
            }
        val timeBucket = postedAt / 60_000L
        val rawFingerprint = "$packageName|$paise|${merchant.lowercase()}|$direction|$timeBucket"
        return ParseResult(ParsedPayment(paise, merchant, Categories.guess(merchant), direction, sha256(rawFingerprint)), "parsed")
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun java.math.BigDecimal.longValueExactOrNull(): Long? = try { longValueExact() } catch (_: ArithmeticException) { null }
}
