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
        "com.csam.icici.bank.imobile",
        "com.sbi.upi",
    )

    private val amountPattern = Regex("(?:\\u20B9|rs\\.?|inr)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    private val looseAmountPattern = Regex("\\b([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\b")
    private val outgoingWords = listOf("paid", "sent", "debited", "spent", "transferred", "payment", "transaction")
    private val successWords = listOf("successful", "success", "completed", "complete", "done", "processed")
    private val incomingWords = listOf("received", "credited", "got money", "money received", "collect request received", "from your contact")
    private val failureWords = listOf("failed", "declined", "pending", "processing", "couldn't", "cancelled", "canceled", "reversed", "request")
    private val merchantPatterns = listOf(
        Regex("(?:paid|sent|payment(?:\\s+of)?|debited|transferred|spent)(?:\\s+(?:\\u20B9|rs\\.?|inr)?\\s*[0-9,.]+)?\\s+(?:to|at)\\s+([^.!\\n|]+)", RegexOption.IGNORE_CASE),
        Regex("(?:payment successful|successful)\\s+([A-Z][A-Z0-9 ._-]{2,60})\\s+(?:\\u20B9|rs\\.?|inr)", RegexOption.IGNORE_CASE),
        Regex("(?:to|at)\\s+([A-Z0-9][^.!\\n|]{2,60})", RegexOption.IGNORE_CASE),
        Regex("^([A-Z][A-Z0-9 ._-]{2,60})\\s+(?:\\u20B9|rs\\.?|inr)", RegexOption.IGNORE_CASE),
    )

    fun parse(packageName: String, title: String?, body: String?, postedAt: Long): ParsedPayment? = inspect(packageName, title, body, postedAt).payment

    fun inspect(packageName: String, title: String?, body: String?, postedAt: Long): ParseResult {
        if (packageName !in supportedPackages) return ParseResult(null, "unsupported app")
        val text = listOfNotNull(title, body).joinToString(" ").replace(Regex("\\s+"), " ").trim()
        val lower = text.lowercase(Locale.ROOT)
        if (text.isBlank()) return ParseResult(null, "blank notification")
        if (incomingWords.any(lower::contains)) return ParseResult(null, "incoming ignored")
        if (failureWords.any(lower::contains)) return ParseResult(null, "failure or pending")
        val amount = amountPattern.find(text) ?: loosePaymentAmount(text) ?: return ParseResult(null, "no amount")
        val hasPaymentIntent = outgoingWords.any(lower::contains) || successWords.any(lower::contains)
        if (!hasPaymentIntent) return ParseResult(null, "no payment words")
        val rupees = amount.groupValues[1].replace(",", "").toBigDecimalOrNull() ?: return ParseResult(null, "bad amount")
        val paise = rupees.movePointRight(2).longValueExactOrNull() ?: return ParseResult(null, "bad paise")
        if (paise <= 0) return ParseResult(null, "zero amount")
        val direction = if ("refund" in lower) TransactionDirection.REFUND else TransactionDirection.EXPENSE
        val merchant = merchantPatterns.firstNotNullOfOrNull { it.find(text)?.groupValues?.getOrNull(1)?.trim() }
            ?.replace(Regex("\\s+(on|via|using|at|from|for)\\s+.*$", RegexOption.IGNORE_CASE), "")
            ?.take(60)?.ifBlank { null } ?: if (direction == TransactionDirection.REFUND) "Refund" else appMerchant(packageName)
        val timeBucket = postedAt / 60_000L
        val rawFingerprint = "$packageName|$paise|${merchant.lowercase()}|$direction|$timeBucket"
        return ParseResult(ParsedPayment(paise, merchant, Categories.guess(merchant), direction, sha256(rawFingerprint)), "parsed outgoing")
    }

    private fun loosePaymentAmount(text: String): MatchResult? {
        val lower = text.lowercase(Locale.ROOT)
        if ("\u20B9" in text || "rs" in lower || "inr" in lower) return null
        return if (outgoingWords.any(lower::contains) && successWords.any(lower::contains)) looseAmountPattern.find(text) else null
    }

    private fun appMerchant(packageName: String) = when (packageName) {
        "com.google.android.apps.nbu.paisa.user" -> "Google Pay payment"
        "net.one97.paytm" -> "Paytm payment"
        "com.phonepe.app" -> "PhonePe payment"
        "in.org.npci.upiapp" -> "BHIM payment"
        else -> "UPI payment"
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun java.math.BigDecimal.longValueExactOrNull(): Long? = try { longValueExact() } catch (_: ArithmeticException) { null }
}
