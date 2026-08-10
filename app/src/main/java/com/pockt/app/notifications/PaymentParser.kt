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
    private val fallbackAmountPattern = Regex("(?:paid|sent|debited|payment(?:\\s+of)?|transferred|spent|refund(?:ed)?)\\D{0,24}([0-9][0-9,]*(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    private val outgoingWords = listOf("paid", "sent", "debited", "payment successful", "successful", "transferred", "spent")
    private val incomingWords = listOf("received", "credited", "got money", "money received", "collect request received")
    private val failureWords = listOf("failed", "declined", "pending", "processing", "couldn't", "cancelled", "reversed")
    private val merchantPatterns = listOf(
        Regex("(?:paid|sent|payment(?:\\s+of)?|debited|transferred|spent)(?:\\s+(?:₹|rs\\.?|inr)?\\s*[0-9,.]+)?\\s+(?:to|at)\\s+([^.!\\n|]+)", RegexOption.IGNORE_CASE),
        Regex("(?:payment successful).*?(?:to|at)\\s+([^.!\\n|]+)", RegexOption.IGNORE_CASE),
        Regex("(?:to|at)\\s+([A-Z0-9][^.!\\n|]{2,60})", RegexOption.IGNORE_CASE),
    )

    fun parse(packageName: String, title: String?, body: String?, postedAt: Long): ParsedPayment? = inspect(packageName, title, body, postedAt).payment

    fun inspect(packageName: String, title: String?, body: String?, postedAt: Long): ParseResult {
        if (packageName !in supportedPackages) return ParseResult(null, "unsupported app")
        val text = listOfNotNull(title, body).joinToString(" ").replace(Regex("\\s+"), " ").trim()
        val lower = text.lowercase(Locale.ROOT)
        if (text.isBlank()) return ParseResult(null, "blank notification")
        if (failureWords.any(lower::contains)) return ParseResult(null, "failure or pending")
        if (incomingWords.any(lower::contains) && outgoingWords.none(lower::contains)) return ParseResult(null, "incoming ignored")
        if (outgoingWords.none(lower::contains) && "refund" !in lower) return ParseResult(null, "no outgoing words")
        val match = amountPattern.find(text) ?: fallbackAmountPattern.find(text) ?: return ParseResult(null, "no amount")
        val rupees = match.groupValues[1].replace(",", "").toBigDecimalOrNull() ?: return ParseResult(null, "bad amount")
        val paise = rupees.movePointRight(2).longValueExactOrNull() ?: return ParseResult(null, "bad paise")
        if (paise <= 0) return ParseResult(null, "zero amount")
        val direction = if ("refund" in lower) TransactionDirection.REFUND else TransactionDirection.EXPENSE
        val merchant = merchantPatterns.firstNotNullOfOrNull { it.find(text)?.groupValues?.getOrNull(1)?.trim() }
            ?.replace(Regex("\\s+(on|via|using|at|from)\\s+.*$", RegexOption.IGNORE_CASE), "")
            ?.take(60)?.ifBlank { null } ?: if (direction == TransactionDirection.REFUND) "Refund" else "UPI payment"
        val timeBucket = postedAt / 60_000L
        val rawFingerprint = "$packageName|$paise|${merchant.lowercase()}|$direction|$timeBucket"
        return ParseResult(ParsedPayment(paise, merchant, Categories.guess(merchant), direction, sha256(rawFingerprint)), "parsed outgoing")
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun java.math.BigDecimal.longValueExactOrNull(): Long? = try { longValueExact() } catch (_: ArithmeticException) { null }
}
