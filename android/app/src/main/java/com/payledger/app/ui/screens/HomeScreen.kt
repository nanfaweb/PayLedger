package com.payledger.app.ui.screens

import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.payledger.app.R
import com.payledger.app.data.model.DashboardSummary
import com.payledger.app.data.repository.TransactionRepository
import com.payledger.app.notification.NotificationIngestBus
import com.payledger.app.ui.components.SummaryCard
import com.payledger.app.ui.components.TransactionRow
import com.payledger.app.ui.util.Format
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeScreen(
    transactionRepository: TransactionRepository,
    monitoringEnabled: Boolean,
    onOpenSettings: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    onAddTransaction: () -> Unit,
) {
    var summary by remember { mutableStateOf<DashboardSummary?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            summary = transactionRepository.summary()
        } catch (e: Exception) {
            error = friendlyError(e)
        }
    }

    LaunchedEffect(Unit) {
        NotificationIngestBus.status.collectLatest { status ->
            if (status.startsWith("queued") || status == "saved") {
                snackbar.showSnackbar(context.getString(R.string.payment_recorded))
                runCatching { summary = transactionRepository.summary() }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction, containerColor = MaterialTheme.colorScheme.primary) {
                Text("+", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(Modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PayLedger", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Text(
                            if (monitoringEnabled) stringResource(R.string.monitoring_on) else stringResource(R.string.monitoring_off),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                }
            }
            item {
                val s = summary
                if (s != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        SummaryCard(stringResource(R.string.today_received), "Rs ${Format.pkr(s.today_total)}", Modifier.weight(1f))
                        SummaryCard(stringResource(R.string.week_received), "Rs ${Format.pkr(s.week_total)}", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        SummaryCard(stringResource(R.string.month_received), "Rs ${Format.pkr(s.month_total)}", Modifier.weight(1f))
                        SummaryCard(stringResource(R.string.transaction_count), s.transaction_count.toString(), Modifier.weight(1f))
                    }
                } else if (error != null) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Loading…")
                }
            }
            item {
                Text(stringResource(R.string.provider_breakdown), style = MaterialTheme.typography.titleMedium)
                summary?.by_provider?.forEach { p ->
                    Text("${Format.providerLabel(p.provider)}: Rs ${Format.pkr(p.total)} (${p.count})")
                }
            }
            item {
                Text(stringResource(R.string.latest_transactions), style = MaterialTheme.typography.titleMedium)
            }
            items(summary?.recent.orEmpty(), key = { it.id }) { tx ->
                TransactionRow(tx) { onOpenTransaction(tx.id) }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

fun isNotificationAccessEnabled(context: android.content.Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat?.contains(context.packageName) == true
}
