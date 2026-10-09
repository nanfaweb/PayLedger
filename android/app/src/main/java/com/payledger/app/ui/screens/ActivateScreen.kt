package com.payledger.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.payledger.app.R
import com.payledger.app.data.repository.LicenseRepository
import com.payledger.app.ui.components.ErrorText
import com.payledger.app.ui.components.PlPrimaryButton
import com.payledger.app.ui.components.PlTextField
import kotlinx.coroutines.launch

@Composable
fun ActivateScreen(
    licenseRepository: LicenseRepository,
    renew: Boolean = false,
    onActivated: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            stringResource(if (renew) R.string.renew_title else R.string.activate_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(if (renew) R.string.renew_subtitle else R.string.activate_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        PlTextField(code, { code = it }, stringResource(R.string.activation_code))
        ErrorText(error)
        Spacer(Modifier.height(16.dp))
        PlPrimaryButton(stringResource(R.string.activate), loading = loading) {
            loading = true
            error = null
            scope.launch {
                try {
                    licenseRepository.activate(code)
                    onActivated()
                } catch (e: Exception) {
                    error = friendlyError(e)
                } finally {
                    loading = false
                }
            }
        }
    }
}
