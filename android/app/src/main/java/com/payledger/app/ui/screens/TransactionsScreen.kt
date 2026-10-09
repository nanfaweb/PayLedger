package com.payledger.app.ui.screens

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.payledger.app.R
import com.payledger.app.data.model.Transaction
import com.payledger.app.data.repository.TransactionRepository
import com.payledger.app.ui.components.PlTextField
import com.payledger.app.ui.components.TransactionRow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactionRepository: TransactionRepository,
    onOpenTransaction: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("") }
    var review by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("newest") }
    var amountMin by remember { mutableStateOf("") }
    var amountMax by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }

    suspend fun load(reset: Boolean = false) {
        val p = if (reset) 1 else page
        try {
            val filters = mutableMapOf(
                "page" to p.toString(),
                "page_size" to "20",
                "sort" to sort,
            )
            if (query.isNotBlank()) filters["q"] = query
            if (provider.isNotBlank()) filters["provider"] = provider
            if (direction.isNotBlank()) filters["direction"] = direction
            if (review.isNotBlank()) filters["review_status"] = review
            if (amountMin.isNotBlank()) filters["amount_min"] = amountMin
            if (amountMax.isNotBlank()) filters["amount_max"] = amountMax
            val result = transactionRepository.search(filters)
            items = if (reset || p == 1) result.items else items + result.items
            total = result.total
            page = result.page
            error = null
        } catch (e: Exception) {
            error = friendlyError(e)
        }
    }

    LaunchedEffect(Unit) { load(reset = true) }

    LaunchedEffect(query, provider, direction, review, sort, amountMin, amountMax) {
        snapshotFlow { listOf(query, provider, direction, review, sort, amountMin, amountMax) }
            .debounce(350)
            .distinctUntilChanged()
            .collect {
                page = 1
                load(reset = true)
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(stringResource(R.string.nav_transactions), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        PlTextField(query, { query = it }, stringResource(R.string.search_hint))
        TextButton(onClick = { showFilters = !showFilters }) {
            Text(if (showFilters) stringResource(R.string.clear_filters) else stringResource(R.string.filters))
        }
        if (showFilters) {
            FilterDropdown("Provider", provider, listOf("" to "All", "jazzcash" to "JazzCash", "easypaisa" to "Easypaisa", "sadapay" to "SadaPay", "nayapay" to "NayaPay")) { provider = it }
            FilterDropdown("Direction", direction, listOf("" to "All", "incoming" to "Incoming", "outgoing" to "Outgoing")) { direction = it }
            FilterDropdown("Status", review, listOf("" to "All", "confirmed" to "Confirmed", "needs_review" to "Needs review", "manual" to "Manual")) { review = it }
            FilterDropdown("Sort", sort, listOf("newest" to "Newest first", "oldest" to "Oldest first")) { sort = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(amountMin, { amountMin = it }, label = { Text("Min amount") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(amountMax, { amountMax = it }, label = { Text("Max amount") }, modifier = Modifier.weight(1f), singleLine = true)
            }
        }
        if (error != null) Text(error ?: "", color = MaterialTheme.colorScheme.error)
        if (items.isEmpty()) {
            Text(stringResource(R.string.no_matches), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 24.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(items, key = { it.id }) { tx ->
                TransactionRow(tx) { onOpenTransaction(tx.id) }
            }
            if (items.size < total) {
                item {
                    TextButton(onClick = {
                        page += 1
                        // load more
                    }) { Text("Load more ($total total)") }
                    LaunchedEffect(page) {
                        if (page > 1) load(reset = false)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(label: String, value: String, options: List<Pair<String, String>>, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.first == value }?.second ?: label
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = {
                    onChange(key)
                    expanded = false
                })
            }
        }
    }
}
