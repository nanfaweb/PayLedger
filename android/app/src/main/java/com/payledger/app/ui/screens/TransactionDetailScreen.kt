package com.payledger.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.payledger.app.R
import com.payledger.app.data.model.Transaction
import com.payledger.app.data.repository.TransactionRepository
import com.payledger.app.ui.components.ErrorText
import com.payledger.app.ui.components.PlPrimaryButton
import com.payledger.app.ui.components.PlSecondaryButton
import com.payledger.app.ui.components.PlTextField
import com.payledger.app.ui.util.Format
import kotlinx.coroutines.launch

@Composable
fun TransactionDetailScreen(
    id: String,
    transactionRepository: TransactionRepository,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
) {
    var tx by remember { mutableStateOf<Transaction?>(null) }
    var sender by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(id) {
        try {
            val loaded = transactionRepository.get(id)
            tx = loaded
            sender = loaded.sender_name.orEmpty()
            amount = loaded.amount.toString()
            notes = loaded.notes.orEmpty()
        } catch (e: Exception) {
            error = friendlyError(e)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        TextButton(onClick = onBack) { Text("← Back") }
        Text(stringResource(R.string.transaction_details), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        val t = tx
        if (t != null) {
            Text(Format.providerLabel(t.provider), style = MaterialTheme.typography.titleMedium)
            Text(Format.dateTime(t.transaction_timestamp))
            Text(stringResource(R.string.disclaimer_source), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            if (editing) {
                PlTextField(sender, { sender = it }, stringResource(R.string.sender_name))
                PlTextField(amount, { amount = it }, "Amount", keyboardType = KeyboardType.Decimal)
                PlTextField(notes, { notes = it }, stringResource(R.string.notes), singleLine = false)
                ErrorText(error)
                PlPrimaryButton(stringResource(R.string.save), loading = loading) {
                    loading = true
                    scope.launch {
                        try {
                            tx = transactionRepository.update(
                                id,
                                mapOf(
                                    "sender_name" to sender.ifBlank { null },
                                    "amount" to amount.toDouble(),
                                    "notes" to notes.ifBlank { null },
                                    "review_status" to "confirmed",
                                ),
                            )
                            editing = false
                        } catch (e: Exception) {
                            error = friendlyError(e)
                        } finally {
                            loading = false
                        }
                    }
                }
                PlSecondaryButton(stringResource(R.string.cancel)) { editing = false }
            } else {
                Text("${stringResource(R.string.sender_name)}: ${t.sender_name ?: stringResource(R.string.unknown_sender)}")
                Text("Amount: Rs ${Format.pkr(t.amount)}")
                Text("${stringResource(R.string.reference)}: ${t.transaction_reference ?: "—"}")
                Text("Status: ${t.review_status}")
                Text("${stringResource(R.string.notes)}: ${t.notes ?: "—"}")
                if (!t.original_notification.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.original_notification), style = MaterialTheme.typography.titleMedium)
                    Text(t.original_notification)
                }
                Spacer(Modifier.height(16.dp))
                PlPrimaryButton(stringResource(R.string.edit)) { editing = true }
                PlSecondaryButton(stringResource(R.string.delete)) {
                    scope.launch {
                        try {
                            transactionRepository.delete(id)
                            onDeleted()
                        } catch (e: Exception) {
                            error = friendlyError(e)
                        }
                    }
                }
            }
        }
        ErrorText(error)
    }
}
