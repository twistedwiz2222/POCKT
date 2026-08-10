package com.pockt.app.notifications

import com.pockt.app.data.TransactionDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentParserTest {
    private val gpay = "com.google.android.apps.nbu.paisa.user"
    private val phonePe = "com.phonepe.app"

    @Test fun parsesExpenseWithRupeeSymbol() {
        val value = PaymentParser.parse(gpay, "Payment successful", "₹280 paid to ABC Cafe", 1_000_000)!!
        assertEquals(28_000, value.amountPaise)
        assertEquals("ABC Cafe", value.merchant)
        assertEquals("Food", value.category)
        assertEquals(TransactionDirection.EXPENSE, value.direction)
    }

    @Test fun parsesPhonePeOutgoingSuccessText() {
        val value = PaymentParser.parse(phonePe, "Payment Successful", "Paid ₹1 to ABHRAJIT MISRA", 1_000_000)!!
        assertEquals(100, value.amountPaise)
        assertEquals("ABHRAJIT MISRA", value.merchant)
        assertEquals(TransactionDirection.EXPENSE, value.direction)
    }

    @Test fun parsesRsFormat() {
        val value = PaymentParser.parse(gpay, "Paid", "Rs. 1,500.50 paid to Store", 1_000_000)!!
        assertEquals(150_050, value.amountPaise)
        assertEquals("Store", value.merchant)
    }

    @Test fun ignoresReceivedMoney() {
        val result = PaymentParser.inspect(phonePe, "Money received", "Received ₹500 from Rohan", 1_000_000)
        assertNull(result.payment)
        assertEquals("incoming ignored", result.reason)
    }

    @Test fun explainsMiss() {
        val result = PaymentParser.inspect(phonePe, "Offer", "Get cashback now", 1_000_000)
        assertEquals(false, result.parsed)
        assertEquals("no outgoing words", result.reason)
    }

    @Test fun ignoresFailure() = assertNull(PaymentParser.parse(gpay, "Payment failed", "₹500 payment failed", 1_000_000))
    @Test fun ignoresUnknownApps() = assertNull(PaymentParser.parse("random.app", "Paid", "₹500 paid to Store", 1_000_000))
}
