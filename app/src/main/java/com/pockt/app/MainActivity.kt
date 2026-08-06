package com.pockt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pockt.app.data.PocktRepository
import com.pockt.app.ui.OnboardingScreen
import com.pockt.app.ui.PocktApp
import com.pockt.app.ui.PocktViewModel
import com.pockt.app.ui.theme.PocktTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: PocktViewModel = viewModel()
            val state by vm.state.collectAsState()
            PocktTheme {
                if (state.preferences[PocktRepository.ONBOARDED] != "true") {
                    OnboardingScreen(
                        onComplete = { vm.setBudget(it, true) },
                    )
                } else PocktApp(state, vm)
            }
        }
    }
}
