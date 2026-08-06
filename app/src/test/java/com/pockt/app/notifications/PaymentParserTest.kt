package com.pockt.app.notifications

import com.pockt.app.data.TransactionDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentParserTest {
    private val gpay = "com.google.android.apps.nbu.paisa.user"

    @Test fun parsesExpense() {
        val value = PaymentParser.parse(gpay, "Payment successful", "₹280 paid to ABC Cafe", 1_000_000)!!
        assertEquals(28_000, value.amountPaise)
        assertEquals("ABC Cafe", value.merchant)
        assertEquals("Food", value.category)
        assertEquals(TransactionDirection.EXPENSE, value.direction)
    }

    @Test fun ignoresFailure() = assertNull(PaymentParser.parse(gpay, "Payment failed", "₹500 payment failed", 1_000_000))
    @Test fun ignoresUnknownApps() = assertNull(PaymentParser.parse("random.app", "Paid", "₹500 paid to Store", 1_000_000))
}
