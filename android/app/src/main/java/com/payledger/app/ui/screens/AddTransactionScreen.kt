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
import com.payledger.app.data.model.CreateTransactionRequest
import com.payledger.app.data.repository.TransactionRepository
import com.payledger.app.ui.components.ErrorText
import com.payledger.app.ui.components.PlPrimaryButton
import com.payledger.app.ui.components.PlTextField
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
fun AddTransactionScreen(
    transactionRepository: TransactionRepository,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    var provider by remember { mutableStateOf("jazzcash") }
    var sender by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        TextButton(onClick = onBack) { Text("← Back") }
        Text(stringResource(R.string.add_transaction), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        PlTextField(provider, { provider = it.lowercase() }, "Provider (jazzcash/easypaisa/sadapay/nayapay)")
        PlTextField(sender, { sender = it }, stringResource(R.string.sender_name))
        PlTextField(phone, { phone = it }, stringResource(R.string.phone_number), keyboardType = KeyboardType.Phone)
        PlTextField(amount, { amount = it }, "Amount", keyboardType = KeyboardType.Decimal)
        PlTextField(reference, { reference = it }, stringResource(R.string.reference))
        PlTextField(notes, { notes = it }, stringResource(R.string.notes), singleLine = false)
        ErrorText(error)
        Spacer(Modifier.height(12.dp))
        PlPrimaryButton(stringResource(R.string.save), loading = loading) {
            val amt = amount.toDoubleOrNull()
            if (amt == null) {
                error = "Enter a valid amount"
                return@PlPrimaryButton
            }
            loading = true
            scope.launch {
                try {
                    transactionRepository.create(
                        CreateTransactionRequest(
                            provider = provider,
                            sender_name = sender.ifBlank { null },
                            sender_identifier = phone.ifBlank { null },
                            amount = amt,
                            transaction_reference = reference.ifBlank { null },
                            transaction_timestamp = Instant.now().toString(),
                            source_type = "manual",
                            review_status = "manual",
                            notes = notes.ifBlank { null },
                        )
                    )
                    onDone()
                } catch (e: Exception) {
                    error = friendlyError(e)
                } finally {
                    loading = false
                }
            }
        }
    }
}
