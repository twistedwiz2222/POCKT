package com.pockt.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class PocktRepository(private val dao: PocktDao) {
    val transactions: Flow<List<TransactionEntity>> = dao.observeTransactions()
    val preferences: Flow<Map<String, String>> = dao.observePreferences()
        .map { values -> values.associate { it.key to it.value } }
    val notificationDebug: Flow<List<NotificationDebugEntity>> = dao.observeNotificationDebug()

    val state = combine(transactions, preferences, notificationDebug) { items, prefs, debug ->
        val budget = prefs[MONTHLY_BUDGET]?.toLongOrNull() ?: 400_000L
        val cycleStartDay = prefs[CYCLE_START_DAY]?.toIntOrNull()?.coerceIn(1, 28) ?: 1
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now(zone)
        val cycleStartDate = cycleStartFor(now, cycleStartDay)
        val cycleEndDate = cycleStartDate.plusMonths(1)
        val start = cycleStartDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = cycleEndDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayStart = now.atStartOfDay(zone).toInstant().toEpochMilli()
        val tomorrowStart = now.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val cycleItems = items.filter { it.occurredAt in start until end }
        val spent = cycleItems.filter { it.direction == TransactionDirection.EXPENSE.name }.sumOf { it.amountPaise }
        val refunds = cycleItems.filter { it.direction == TransactionDirection.REFUND.name }.sumOf { it.amountPaise }
        val netSpent = (spent - refunds).coerceAtLeast(0)
        val remainingRaw = budget - netSpent
        val remaining = remainingRaw.coerceAtLeast(0)
        val daysRemaining = (ChronoUnit.DAYS.between(now, cycleEndDate) + 1).toInt().coerceAtLeast(1)
        val cycleLengthDays = ChronoUnit.DAYS.between(cycleStartDate, cycleEndDate).toInt().coerceAtLeast(1)
        val daysElapsed = (ChronoUnit.DAYS.between(cycleStartDate, now) + 1).toInt().coerceIn(1, cycleLengthDays)
        val dailyLimit = budget / cycleLengthDays
        val todaySpend = cycleItems.filter { it.occurredAt in todayStart until tomorrowStart && it.direction == TransactionDirection.EXPENSE.name }.sumOf { it.amountPaise }
        val todaySaved = (dailyLimit - todaySpend).coerceAtLeast(0)
        val safeDaily = remaining / daysRemaining
        AppState(
            transactions = items,
            budget = BudgetSnapshot(
                monthlyBudgetPaise = budget,
                spentPaise = netSpent,
                remainingPaise = remainingRaw,
                safeDailyPaise = safeDaily,
                daysRemaining = daysRemaining,
                progress = if (budget == 0L) 0f else (netSpent.toFloat() / budget).coerceIn(0f, 1f),
                cycleStartDay = cycleStartDay,
                cycleStartMillis = start,
                cycleEndMillis = end,
                daysElapsed = daysElapsed,
                todaySpendPaise = todaySpend,
                todayLimitPaise = dailyLimit,
                todaySavedPaise = todaySaved,
            ),
            preferences = prefs,
            notificationDebug = debug,
        )
    }

    suspend fun add(amountPaise: Long, merchant: String, category: String, source: String = "Manual", occurredAt: Long = Instant.now().toEpochMilli(), fingerprint: String? = null, direction: TransactionDirection = TransactionDirection.EXPENSE) =
        dao.insert(TransactionEntity(amountPaise = amountPaise, merchant = merchant.ifBlank { "Unknown merchant" }, category = category, occurredAt = occurredAt, sourceApp = source, fingerprint = fingerprint, direction = direction.name))

    suspend fun update(item: TransactionEntity) = dao.update(item)
    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun isDuplicate(fingerprint: String) = dao.fingerprintCount(fingerprint) > 0
    suspend fun setMonthlyBudget(paise: Long) = dao.savePreference(PreferenceEntity(MONTHLY_BUDGET, paise.toString()))
    suspend fun setCycleStartDay(day: Int) = dao.savePreference(PreferenceEntity(CYCLE_START_DAY, day.coerceIn(1, 28).toString()))
    suspend fun setOnboarded() = dao.savePreference(PreferenceEntity(ONBOARDED, "true"))
    suspend fun addNotificationDebug(packageName: String, appName: String, title: String?, body: String?, postedAt: Long, parsed: Boolean, reason: String) {
        dao.insertNotificationDebug(NotificationDebugEntity(packageName = packageName, appName = appName, title = title.orEmpty().take(160), body = body.orEmpty().take(320), postedAt = postedAt, parsed = parsed, reason = reason.take(80)))
        dao.trimNotificationDebug()
    }
    suspend fun clear() { dao.deleteAllTransactions(); dao.deleteAllPreferences(); dao.deleteAllNotificationDebug() }

    private fun cycleStartFor(date: LocalDate, startDay: Int): LocalDate {
        val thisMonthStart = date.withDayOfMonth(minOf(startDay, date.lengthOfMonth()))
        return if (date.isBefore(thisMonthStart)) {
            val previous = date.minusMonths(1)
            previous.withDayOfMonth(minOf(startDay, previous.lengthOfMonth()))
        } else thisMonthStart
    }

    companion object {
        const val MONTHLY_BUDGET = "monthly_budget_paise"
        const val CYCLE_START_DAY = "cycle_start_day"
        const val ONBOARDED = "onboarded"
    }
}

data class AppState(val transactions: List<TransactionEntity>, val budget: BudgetSnapshot, val preferences: Map<String, String>, val notificationDebug: List<NotificationDebugEntity>)
