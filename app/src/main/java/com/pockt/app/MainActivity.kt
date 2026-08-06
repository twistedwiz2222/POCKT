package com.pockt.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pockt.app.data.PocktRepository
import com.pockt.app.ui.OnboardingScreen
import com.pockt.app.ui.PocktApp
import com.pockt.app.ui.PocktViewModel
import com.pockt.app.ui.theme.PocktTheme

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.PAYMENT_DETECTOR_ENABLED && Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val vm: PocktViewModel = viewModel()
            val state by vm.state.collectAsState()
            PocktTheme {
                if (state.preferences[PocktRepository.ONBOARDED] != "true") {
                    OnboardingScreen(
                        detectorEnabled = BuildConfig.PAYMENT_DETECTOR_ENABLED,
                        onEnableAccess = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                        onComplete = { vm.setBudget(it, true) },
                    )
                } else {
                    PocktApp(
                        state = state,
                        vm = vm,
                        detectorEnabled = BuildConfig.PAYMENT_DETECTOR_ENABLED,
                        onOpenNotificationAccess = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    )
                }
            }
        }
    }
}
