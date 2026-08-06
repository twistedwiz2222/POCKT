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
    val state = repository.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppState(emptyList(), BudgetSnapshot(400_000, 0, 400_000, 0, 1, 0f), emptyMap()))

    fun add(amountRupees: String, merchant: String, category: String) = viewModelScope.launch {
        amountRupees.toBigDecimalOrNull()?.movePointRight(2)?.longValueExact()?.takeIf { it > 0 }?.let { repository.add(it, merchant, category) }
    }
    fun setBudget(rupees: String, completeOnboarding: Boolean = false) = viewModelScope.launch {
        rupees.toBigDecimalOrNull()?.movePointRight(2)?.longValueExact()?.takeIf { it > 0 }?.let { repository.setMonthlyBudget(it) }
        if (completeOnboarding) repository.setOnboarded()
    }
    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }
    fun clear() = viewModelScope.launch { repository.clear() }
}
