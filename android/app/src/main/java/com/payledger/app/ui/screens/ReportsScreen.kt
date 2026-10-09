package com.payledger.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.payledger.app.R
import com.payledger.app.data.local.TokenStore
import com.payledger.app.data.repository.TransactionRepository
import com.payledger.app.report.PdfReportGenerator
import com.payledger.app.report.ReportOptions
import com.payledger.app.ui.components.ErrorText
import com.payledger.app.ui.components.PlPrimaryButton
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ReportsScreen(
    transactionRepository: TransactionRepository,
    tokenStore: TokenStore,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var period by remember { mutableStateOf("this_month") }
    var provider by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastFile by remember { mutableStateOf<String?>(null) }

    val periods = listOf(
        "today" to R.string.period_today,
        "yesterday" to R.string.period_yesterday,
        "this_week" to R.string.period_this_week,
        "last_week" to R.string.period_last_week,
        "this_month" to R.string.period_this_month,
        "last_month" to R.string.period_last_month,
    )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(stringResource(R.string.reports_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        periods.forEach { (key, res) ->
            FilterChip(
                selected = period == key,
                onClick = { period = key },
                label = { Text(stringResource(res)) },
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.all_providers))
        listOf("" to stringResource(R.string.all_providers), "jazzcash" to "JazzCash", "easypaisa" to "Easypaisa", "sadapay" to "SadaPay", "nayapay" to "NayaPay").forEach { (key, label) ->
            FilterChip(selected = provider == key, onClick = { provider = key }, label = { Text(label) }, modifier = Modifier.padding(vertical = 2.dp))
        }
        ErrorText(error)
        if (lastFile != null) Text("Saved: $lastFile", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        PlPrimaryButton(stringResource(R.string.generate_pdf), loading = loading) {
            loading = true
            error = null
            scope.launch {
                try {
                    val range = dateRange(period)
                    val filters = mutableMapOf(
                        "date_from" to range.first,
                        "date_to" to range.second,
                        "direction" to "incoming",
                        "page" to "1",
                        "page_size" to "100",
                        "sort" to "oldest",
                    )
                    if (provider.isNotBlank()) filters["provider"] = provider
                    val page = transactionRepository.search(filters)
                    val all = page.items.toMutableList()
                    var p = 2
                    while (p <= page.total_pages) {
                        filters["page"] = p.toString()
                        all += transactionRepository.search(filters).items
                        p++
                    }
                    val file = PdfReportGenerator(context).generate(
                        ReportOptions(
                            accountName = tokenStore.fullName ?: "PayLedger user",
                            periodLabel = range.third,
                            language = tokenStore.language,
                            transactions = all,
                        )
                    )
                    lastFile = file.name
                    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(share, context.getString(R.string.preview_share)))
                } catch (e: Exception) {
                    error = friendlyError(e)
                } finally {
                    loading = false
                }
            }
        }
    }
}

private fun dateRange(period: String): Triple<String, String, String> {
    val zone = ZoneId.of("Asia/Karachi")
    val today = LocalDate.now(zone)
    val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    fun start(d: LocalDate) = d.atStartOfDay(zone).format(fmt)
    fun end(d: LocalDate) = d.plusDays(1).atStartOfDay(zone).minusNanos(1).format(fmt)
    return when (period) {
        "today" -> Triple(start(today), end(today), "Today")
        "yesterday" -> Triple(start(today.minusDays(1)), end(today.minusDays(1)), "Yesterday")
        "this_week" -> {
            val from = today.minusDays((today.dayOfWeek.value % 7).toLong())
            Triple(start(from), end(today), "This week")
        }
        "last_week" -> {
            val endDay = today.minusDays((today.dayOfWeek.value % 7).toLong() + 1)
            val startDay = endDay.minusDays(6)
            Triple(start(startDay), end(endDay), "Last week")
        }
        "last_month" -> {
            val first = today.withDayOfMonth(1).minusMonths(1)
            val last = today.withDayOfMonth(1).minusDays(1)
            Triple(start(first), end(last), "Last month")
        }
        else -> {
            val first = today.withDayOfMonth(1)
            Triple(start(first), end(today), "This month")
        }
    }
}
