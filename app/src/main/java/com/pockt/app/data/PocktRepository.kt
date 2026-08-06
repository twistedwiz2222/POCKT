package com.pockt.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class PocktRepository(private val dao: PocktDao) {
    val transactions: Flow<List<TransactionEntity>> = dao.observeTransactions()
    val preferences: Flow<Map<String, String>> = dao.observePreferences()
        .map { values -> values.associate { it.key to it.value } }

    val state = combine(transactions, preferences) { items, prefs ->
        val budget = prefs[MONTHLY_BUDGET]?.toLongOrNull() ?: 400_000L
        val now = LocalDate.now()
        val start = now.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = now.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val spent = items.filter { it.occurredAt in start until end && it.direction == TransactionDirection.EXPENSE.name }.sumOf { it.amountPaise }
        val refunds = items.filter { it.occurredAt in start until end && it.direction == TransactionDirection.REFUND.name }.sumOf { it.amountPaise }
        val netSpent = (spent - refunds).coerceAtLeast(0)
        val remaining = (budget - netSpent).coerceAtLeast(0)
        val days = (now.lengthOfMonth() - now.dayOfMonth + 1).coerceAtLeast(1)
        AppState(items, BudgetSnapshot(budget, netSpent, budget - netSpent, remaining / days, days, if (budget == 0L) 0f else (netSpent.toFloat() / budget).coerceIn(0f, 1f)), prefs)
    }

    suspend fun add(amountPaise: Long, merchant: String, category: String, source: String = "Manual", occurredAt: Long = Instant.now().toEpochMilli(), fingerprint: String? = null, direction: TransactionDirection = TransactionDirection.EXPENSE) =
        dao.insert(TransactionEntity(amountPaise = amountPaise, merchant = merchant.ifBlank { "Unknown merchant" }, category = category, occurredAt = occurredAt, sourceApp = source, fingerprint = fingerprint, direction = direction.name))

    suspend fun update(item: TransactionEntity) = dao.update(item)
    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun isDuplicate(fingerprint: String) = dao.fingerprintCount(fingerprint) > 0
    suspend fun setMonthlyBudget(paise: Long) = dao.savePreference(PreferenceEntity(MONTHLY_BUDGET, paise.toString()))
    suspend fun setOnboarded() = dao.savePreference(PreferenceEntity(ONBOARDED, "true"))
    suspend fun clear() { dao.deleteAllTransactions(); dao.deleteAllPreferences() }

    companion object { const val MONTHLY_BUDGET = "monthly_budget_paise"; const val ONBOARDED = "onboarded" }
}

data class AppState(val transactions: List<TransactionEntity>, val budget: BudgetSnapshot, val preferences: Map<String, String>)
