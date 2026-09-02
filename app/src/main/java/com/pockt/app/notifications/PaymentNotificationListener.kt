package com.pockt.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.pockt.app.MainActivity
import com.pockt.app.PocktApplication
import com.pockt.app.data.TransactionDirection
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PaymentNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() { super.onCreate(); createChannel() }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val body = listOfNotNull(text, bigText, subText).distinct().joinToString(" | ")
        val result = PaymentParser.inspect(sbn.packageName, title, body, sbn.postTime)
        scope.launch {
            val repo = (application as PocktApplication).repository
            val parsed = result.payment
            repo.addNotificationDebug(sbn.packageName, appName(sbn.packageName), title, body, sbn.postTime, result.parsed, result.reason)
            if (parsed == null) return@launch
            if (repo.isDuplicate(parsed.fingerprint)) return@launch
            repo.add(parsed.amountPaise, parsed.merchant, parsed.category, appName(sbn.packageName), sbn.postTime, parsed.fingerprint, parsed.direction)
            if (parsed.direction == TransactionDirection.EXPENSE) {
                val state = repo.state.first()
                showBudgetAlert(parsed.amountPaise, parsed.merchant, state.budget.remainingPaise, state.budget.safeDailyPaise, state.budget.todaySavedPaise)
            }
        }
    }

    private fun showBudgetAlert(amount: Long, merchant: String, remaining: Long, safeDaily: Long, todaySaved: Long) {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(this, 10, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val detail = if (todaySaved > 0) "${money(todaySaved)} saved today" else "today's allowance used"
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("${money(amount)} spent at $merchant")
            .setContentText("${money(remaining)} left - ${money(safeDaily)}/day safe")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${money(remaining)} left this cycle. ${money(safeDaily)} safe per day; $detail."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL, "Payment insights", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Immediate local budget updates after detected payments"
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun appName(pkg: String) = when (pkg) {
        "com.google.android.apps.nbu.paisa.user" -> "Google Pay"
        "net.one97.paytm" -> "Paytm"
        "com.phonepe.app" -> "PhonePe"
        "in.org.npci.upiapp" -> "BHIM"
        "com.csam.icici.bank.imobile" -> "iMobile"
        "com.sbi.upi" -> "BHIM SBI Pay"
        else -> pkg
    }

    private fun money(paise: Long): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(paise / 100.0).replace(".00", "")
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
    companion object { const val CHANNEL = "pockt_payment_insights" }
}