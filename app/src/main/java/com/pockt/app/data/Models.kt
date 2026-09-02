package com.pockt.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionDirection { EXPENSE, INCOME, REFUND }

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountPaise: Long,
    val merchant: String,
    val category: String,
    val occurredAt: Long,
    val sourceApp: String,
    val direction: String = TransactionDirection.EXPENSE.name,
    val fingerprint: String? = null,
    val note: String = "",
)

@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Entity(tableName = "notification_debug")
data class NotificationDebugEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val body: String,
    val postedAt: Long,
    val parsed: Boolean,
    val reason: String,
)

data class BudgetSnapshot(
    val monthlyBudgetPaise: Long,
    val spentPaise: Long,
    val remainingPaise: Long,
    val safeDailyPaise: Long,
    val daysRemaining: Int,
    val progress: Float,
    val cycleStartDay: Int,
    val cycleStartMillis: Long,
    val cycleEndMillis: Long,
    val daysElapsed: Int,
    val todaySpendPaise: Long,
    val todayLimitPaise: Long,
    val todaySavedPaise: Long,
)

object Categories {
    val all = listOf("Food", "Transport", "Shopping", "Entertainment", "Education", "Bills", "Health", "Cash", "Other")

    fun guess(merchant: String): String {
        val value = merchant.lowercase()
        return when {
            listOf("swiggy", "zomato", "cafe", "restaurant", "foods", "tea", "coffee").any(value::contains) -> "Food"
            listOf("uber", "ola", "metro", "rapido", "irctc", "fuel").any(value::contains) -> "Transport"
            listOf("amazon", "flipkart", "myntra", "store", "mart").any(value::contains) -> "Shopping"
            listOf("netflix", "spotify", "cinema", "bookmyshow").any(value::contains) -> "Entertainment"
            listOf("school", "college", "course", "books").any(value::contains) -> "Education"
            listOf("electricity", "broadband", "mobile", "recharge", "bill").any(value::contains) -> "Bills"
            listOf("pharmacy", "hospital", "clinic", "medical").any(value::contains) -> "Health"
            else -> "Other"
        }
    }
}
