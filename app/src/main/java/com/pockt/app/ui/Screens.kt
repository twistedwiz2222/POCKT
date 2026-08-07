package com.pockt.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pockt.app.data.AppState
import com.pockt.app.data.Categories
import com.pockt.app.data.NotificationDebugEntity
import com.pockt.app.data.TransactionDirection
import com.pockt.app.data.TransactionEntity
import com.pockt.app.ui.theme.Coral
import com.pockt.app.ui.theme.Mint
import com.pockt.app.ui.theme.Muted
import com.pockt.app.ui.theme.Raised
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Tab { HOME, ACTIVITY, SETTINGS }

@Composable
fun PocktApp(
    state: AppState,
    vm: PocktViewModel,
    detectorEnabled: Boolean,
    onOpenNotificationAccess: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var addOpen by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0D1014)) {
                NavigationBarItem(tab == Tab.HOME, { tab = Tab.HOME }, { Icon(Icons.Outlined.Home, null) }, label = { Text("Overview") })
                NavigationBarItem(tab == Tab.ACTIVITY, { tab = Tab.ACTIVITY }, { Icon(Icons.Outlined.ReceiptLong, null) }, label = { Text("Activity") })
                NavigationBarItem(tab == Tab.SETTINGS, { tab = Tab.SETTINGS }, { Icon(Icons.Outlined.Settings, null) }, label = { Text("Settings") })
            }
        },
        floatingActionButton = {
            if (tab != Tab.SETTINGS) FloatingActionButton(onClick = { addOpen = true }, containerColor = Mint, contentColor = Color.Black) { Icon(Icons.Outlined.Add, "Add expense") }
        },
    ) { padding ->
        when (tab) {
            Tab.HOME -> Dashboard(state, Modifier.padding(padding))
            Tab.ACTIVITY -> ActivityScreen(state.transactions, vm::delete, Modifier.padding(padding))
            Tab.SETTINGS -> SettingsScreen(state, vm, detectorEnabled, onOpenNotificationAccess, Modifier.padding(padding))
        }
    }
    if (addOpen) AddExpenseDialog(onDismiss = { addOpen = false }) { amount, merchant, category -> vm.add(amount, merchant, category); addOpen = false }
}

@Composable
fun OnboardingScreen(
    detectorEnabled: Boolean,
    onEnableAccess: () -> Unit,
    onComplete: (String) -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var budget by rememberSaveable { mutableStateOf("4000") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            Spacer(Modifier.height(30.dp))
            Text("POCKT", color = Mint, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Spacer(Modifier.height(40.dp))
            Text(if (page == 0) "Know what every payment costs you." else "Set your monthly limit.", fontSize = 38.sp, lineHeight = 43.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(18.dp))
            Text(if (page == 0) introCopy(detectorEnabled) else "Start with the money you can actually spend this month. You can change it anytime.", color = Muted, fontSize = 17.sp, lineHeight = 25.sp)
            Spacer(Modifier.height(36.dp))
            if (page == 0) {
                PrivacyRow("Local only", "No account, cloud, ads, or tracking")
                if (detectorEnabled) {
                    PrivacyRow("Payment notifications", "Google Pay, Paytm, PhonePe and BHIM")
                    PrivacyRow("Never payment screens", "No PINs, OTPs, SMS or Accessibility access")
                } else {
                    PrivacyRow("Manual entries", "Add spends in a few taps after payment")
                    PrivacyRow("No sensitive access", "No notifications, PINs, OTPs, SMS or Accessibility")
                }
            } else {
                OutlinedTextField(value = budget, onValueChange = { budget = it.filter(Char::isDigit).take(8) }, prefix = { Text("Rs.") }, label = { Text("Monthly budget") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = LocalTextStyle.current.copy(fontSize = 28.sp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                Text("About ${money((budget.toLongOrNull() ?: 0) * 100 / 30)} per day", color = Mint)
            }
        }
        Column {
            if (page == 0 && detectorEnabled) {
                OutlinedButton(onClick = onEnableAccess, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Enable notification access") }
                Spacer(Modifier.height(12.dp))
            }
            Button(onClick = { if (page == 0) page = 1 else onComplete(budget) }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = page == 0 || (budget.toLongOrNull() ?: 0) > 0) { Text(if (page == 0) "Continue" else "Start using POCKT") }
        }
    }
}

private fun introCopy(detectorEnabled: Boolean): String =
    if (detectorEnabled) "POCKT reads supported payment confirmations on this device and instantly shows what remains. Nothing leaves your phone."
    else "POCKT tracks the expenses you add and shows what remains. Nothing leaves your phone."

@Composable private fun PrivacyRow(title: String, subtitle: String) {
    Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Raised, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Lock, null, tint = Mint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp)); Column { Text(title, fontWeight = FontWeight.Medium); Text(subtitle, color = Muted, fontSize = 13.sp) }
    }
}

@Composable private fun Dashboard(state: AppState, modifier: Modifier = Modifier) {
    val budget = state.budget
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("POCKT", color = Mint, fontSize = 13.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold); Text("Your cycle", fontSize = 28.sp, fontWeight = FontWeight.SemiBold) }
                Box(Modifier.size(42.dp).background(Raised, CircleShape), contentAlignment = Alignment.Center) { Text("P", color = Mint, fontWeight = FontWeight.Bold) }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Raised), shape = RoundedCornerShape(26.dp)) {
                Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
                    BudgetRing(budget.progress)
                    Spacer(Modifier.width(22.dp))
                    Column { Text("REMAINING", color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp); Text(money(budget.remainingPaise), fontSize = 32.sp, fontWeight = FontWeight.SemiBold); Text("of ${money(budget.monthlyBudgetPaise)}", color = Muted) }
                }
                HorizontalDivider(color = Color.White.copy(alpha = .07f))
                Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Metric("SPENT", money(budget.spentPaise)); Metric("DAYS LEFT", budget.daysRemaining.toString()); Metric("PER DAY", money(budget.safeDailyPaise), Mint)
                }
            }
        }
        item { RecoveryCard(state) }
        item {
            Text(if (budget.remainingPaise >= 0) "You can safely spend ${money(budget.safeDailyPaise)} today." else "You are ${money(-budget.remainingPaise)} over budget.", fontSize = 19.sp, fontWeight = FontWeight.Medium)
            Text("Cycle starts on day ${budget.cycleStartDay}. ${budget.daysRemaining} days left.", color = Muted, fontSize = 13.sp)
        }
        item { SectionHeader("Recent activity", "This cycle") }
        if (state.transactions.isEmpty()) item { EmptyActivity() }
        items(state.transactions.take(5), key = { it.id }) { TransactionRow(it) }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable private fun RecoveryCard(state: AppState) {
    val budget = state.budget
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF12171C)), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Recovery plan", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Metric("TODAY", money(budget.todaySpendPaise))
                Metric("TODAY LIMIT", money(budget.todayLimitPaise), Mint)
                Metric("7 DAY PLAN", money(budget.recoveryDailyPaise), if (budget.todayOverPaise > 0) Coral else Mint)
            }
            val line = when {
                budget.remainingPaise < 0 -> "You are over cycle budget. Pause optional spends and use manual review."
                budget.todayOverPaise > 0 -> "You exceeded today by ${money(budget.todayOverPaise)}. Spend about ${money(budget.recoveryDailyPaise)}/day for the next ${budget.recoveryDays} days."
                budget.projectedOverspendPaise > 0 -> "At this pace you may overshoot by ${money(budget.projectedOverspendPaise)}. Keep the next week near ${money(budget.recoveryDailyPaise)}/day."
                else -> "You are on track. Keep today near ${money(budget.todayLimitPaise)} and the cycle near ${money(budget.safeDailyPaise)}/day."
            }
            Text(line, color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable private fun BudgetRing(progress: Float) {
    Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(Color.White.copy(alpha = .08f), -90f, 360f, false, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
            drawArc(if (progress > .85f) Coral else Mint, -90f, progress * 360f, false, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${(progress * 100).toInt()}%", fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("used", color = Muted, fontSize = 11.sp) }
    }
}

@Composable private fun Metric(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) { Column { Text(label, color = Muted, fontSize = 10.sp, letterSpacing = 1.sp); Spacer(Modifier.height(4.dp)); Text(value, color = color, fontWeight = FontWeight.SemiBold) } }
@Composable private fun SectionHeader(title: String, trailing: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold); Text(trailing, color = Muted, fontSize = 13.sp) } }

@Composable private fun ActivityScreen(transactionItems: List<TransactionEntity>, delete: (Long) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Spacer(Modifier.height(12.dp)); Text("Activity", fontSize = 30.sp, fontWeight = FontWeight.SemiBold); Text("Every transaction stays on this phone.", color = Muted); Spacer(Modifier.height(14.dp)) }
        if (transactionItems.isEmpty()) item { EmptyActivity() }
        items(transactionItems, key = { it.id }) { item -> TransactionRow(item, { delete(item.id) }) }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable private fun TransactionRow(item: TransactionEntity, onDelete: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Raised).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(categoryColor(item.category).copy(alpha = .16f), CircleShape), contentAlignment = Alignment.Center) { Text(item.category.take(1), color = categoryColor(item.category), fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(item.merchant, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium); Text("${item.category} - ${SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(item.occurredAt))}", color = Muted, fontSize = 12.sp) }
        Text((if (item.direction == TransactionDirection.EXPENSE.name) "-" else "+") + money(item.amountPaise), color = if (item.direction == TransactionDirection.EXPENSE.name) MaterialTheme.colorScheme.onSurface else Mint, fontWeight = FontWeight.SemiBold)
        if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, "Delete", tint = Muted) }
    }
}

@Composable private fun EmptyActivity() { Card(colors = CardDefaults.cardColors(containerColor = Raised), shape = RoundedCornerShape(20.dp)) { Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Outlined.ReceiptLong, null, tint = Muted); Spacer(Modifier.height(10.dp)); Text("No spending yet", fontWeight = FontWeight.Medium); Text("Detected and manual expenses will appear here.", color = Muted, fontSize = 13.sp) } } }

@Composable private fun SettingsScreen(state: AppState, vm: PocktViewModel, detectorEnabled: Boolean, access: () -> Unit, modifier: Modifier = Modifier) {
    var budget by remember(state.budget.monthlyBudgetPaise) { mutableStateOf((state.budget.monthlyBudgetPaise / 100).toString()) }
    var cycleDay by remember(state.budget.cycleStartDay) { mutableStateOf(state.budget.cycleStartDay.toString()) }
    var confirmClear by remember { mutableStateOf(false) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(12.dp)); Text("Settings", fontSize = 30.sp, fontWeight = FontWeight.SemiBold); Text("Private by default.", color = Muted); Spacer(Modifier.height(12.dp)) }
        item { Text("BUDGET", color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp) }
        item { OutlinedTextField(budget, { budget = it.filter(Char::isDigit).take(8) }, modifier = Modifier.fillMaxWidth(), prefix = { Text("Rs.") }, label = { Text("Monthly spending limit") }, trailingIcon = { TextButton({ vm.setBudget(budget) }) { Text("Save") } }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
        item { OutlinedTextField(cycleDay, { cycleDay = it.filter(Char::isDigit).take(2) }, modifier = Modifier.fillMaxWidth(), label = { Text("Cycle start day, 1-28") }, trailingIcon = { TextButton({ vm.setCycleStartDay(cycleDay) }) { Text("Save") } }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
        if (detectorEnabled) {
            item { Text("DETECTION", color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp) }
            item { SettingCard("Notification access", "Required to detect payment confirmations", access) }
            item { Text("LAST NOTIFICATIONS", color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp) }
            if (state.notificationDebug.isEmpty()) item { Text("No supported payment-app notifications seen yet.", color = Muted, fontSize = 13.sp) }
            items(state.notificationDebug.take(8), key = { it.id }) { DebugRow(it) }
        }
        item { Text("DATA", color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp) }
        item { SettingCard("Stored locally", "${state.transactions.size} transactions - no cloud sync") {} }
        item { OutlinedButton(onClick = { confirmClear = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral), modifier = Modifier.fillMaxWidth()) { Text("Delete all POCKT data") } }
        item { Text(if (detectorEnabled) "POCKT reads payment notifications only after you grant Android notification access. It never reads payment screens, PINs, OTPs, SMS, contacts, or location." else "POCKT never reads notifications, payment screens, PINs, OTPs, SMS, contacts, or location.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp); Spacer(Modifier.height(70.dp)) }
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text("Delete everything?") }, text = { Text("All transactions, debug logs, and your budget will be permanently removed from this phone.") }, confirmButton = { TextButton({ vm.clear(); confirmClear = false }) { Text("Delete", color = Coral) } }, dismissButton = { TextButton({ confirmClear = false }) { Text("Cancel") } })
}

@Composable private fun DebugRow(item: NotificationDebugEntity) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Raised).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(item.appName, fontWeight = FontWeight.Medium); Text(if (item.parsed) "parsed" else item.reason, color = if (item.parsed) Mint else Coral, fontSize = 12.sp) }
        Text(item.title.ifBlank { "(no title)" }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
        Text(item.body.ifBlank { "(no body)" }, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Muted, fontSize = 12.sp)
    }
}

@Composable private fun SettingCard(title: String, subtitle: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Raised).clickable(onClick = onClick).padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium); Text(subtitle, color = Muted, fontSize = 13.sp) }; Text(">", color = Mint, fontSize = 24.sp) } }

@Composable private fun AddExpenseDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var amount by remember { mutableStateOf("") }; var merchant by remember { mutableStateOf("") }; var category by remember { mutableStateOf("Food") }; var expanded by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add expense") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' }.take(10) }, prefix = { Text("Rs.") }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(merchant, { merchant = it.take(60) }, label = { Text("Merchant or note") }, modifier = Modifier.fillMaxWidth())
            Box { OutlinedButton({ expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(category); Spacer(Modifier.weight(1f)); Text("v") }; DropdownMenu(expanded, { expanded = false }) { Categories.all.forEach { DropdownMenuItem({ Text(it) }, { category = it; expanded = false }) } } }
        }
    }, confirmButton = { Button({ onAdd(amount, merchant, category) }, enabled = (amount.toDoubleOrNull() ?: 0.0) > 0) { Text("Add") } }, dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}

private fun money(paise: Long): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(paise / 100.0).replace(".00", "")
private fun categoryColor(category: String): Color = when (category) { "Food" -> Color(0xFFFFB86B); "Transport" -> Color(0xFF81AFFF); "Shopping" -> Color(0xFFD89CFF); "Health" -> Color(0xFFFF7B8D); else -> Mint }
