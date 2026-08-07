package com.pockt.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pockt.app.PocktApplication
import com.pockt.app.data.AppState
import com.pockt.app.data.BudgetSnapshot
import com.pockt.app.data.PocktRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PocktViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as PocktApplication).repository
    val state = repository.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppState(emptyList(), emptyBudget(), emptyMap(), emptyList()))

    fun add(amountRupees: String, merchant: String, category: String) = viewModelScope.launch {
        amountRupees.toBigDecimalOrNull()?.movePointRight(2)?.longValueExact()?.takeIf { it > 0 }?.let { repository.add(it, merchant, category) }
    }
    fun setBudget(rupees: String, completeOnboarding: Boolean = false) = viewModelScope.launch {
        rupees.toBigDecimalOrNull()?.movePointRight(2)?.longValueExact()?.takeIf { it > 0 }?.let { repository.setMonthlyBudget(it) }
        if (completeOnboarding) repository.setOnboarded()
    }
    fun setCycleStartDay(day: String) = viewModelScope.launch {
        day.toIntOrNull()?.takeIf { it in 1..28 }?.let { repository.setCycleStartDay(it) }
    }
    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }
    fun clear() = viewModelScope.launch { repository.clear() }

    companion object {
        private fun emptyBudget() = BudgetSnapshot(
            monthlyBudgetPaise = 400_000,
            spentPaise = 0,
            remainingPaise = 400_000,
            safeDailyPaise = 0,
            daysRemaining = 1,
            progress = 0f,
            cycleStartDay = 1,
            cycleStartMillis = 0,
            cycleEndMillis = 0,
            daysElapsed = 1,
            todaySpendPaise = 0,
            todayLimitPaise = 0,
            todayOverPaise = 0,
            recoveryDailyPaise = 0,
            recoveryDays = 1,
            projectedOverspendPaise = 0,
        )
    }
}
