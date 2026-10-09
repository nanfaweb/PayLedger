package com.payledger.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.payledger.app.R
import com.payledger.app.data.AppContainer
import com.payledger.app.data.model.MeResponse
import com.payledger.app.ui.components.ErrorText
import com.payledger.app.ui.components.PlPrimaryButton
import com.payledger.app.ui.components.PlSecondaryButton
import com.payledger.app.ui.util.Format
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    container: AppContainer,
    onLoggedOut: () -> Unit,
    onLanguageChanged: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var me by remember { mutableStateOf<MeResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val accessEnabled = isNotificationAccessEnabled(context)

    LaunchedEffect(Unit) {
        try {
            me = container.authRepository.me()
            me?.providers?.forEach { container.providerPrefs.setEnabled(it.provider, it.monitoring_enabled) }
        } catch (e: Exception) {
            error = friendlyError(e)
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))

        Text(stringResource(R.string.profile), style = MaterialTheme.typography.titleMedium)
        Text(me?.user?.full_name ?: container.tokenStore.fullName.orEmpty())
        Text(me?.user?.phone_number ?: container.tokenStore.phoneNumber.orEmpty())

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
        Row {
            TextButton(onClick = {
                scope.launch {
                    container.authRepository.updateLanguage("en")
                    onLanguageChanged("en")
                }
            }) { Text(stringResource(R.string.language_english)) }
            TextButton(onClick = {
                scope.launch {
                    container.authRepository.updateLanguage("ur")
                    onLanguageChanged("ur")
                }
            }) { Text(stringResource(R.string.language_urdu)) }
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.notification_access), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.notification_access_required),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(if (accessEnabled) stringResource(R.string.monitoring_on) else stringResource(R.string.monitoring_off))
        PlPrimaryButton(stringResource(R.string.open_notification_settings)) {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.payment_providers), style = MaterialTheme.typography.titleMedium)
        listOf("jazzcash", "easypaisa", "sadapay", "nayapay").forEach { provider ->
            var enabled by remember(provider, me) {
                mutableStateOf(container.providerPrefs.isEnabled(provider))
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(Format.providerLabel(provider), modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { value ->
                    enabled = value
                    container.providerPrefs.setEnabled(provider, value)
                    scope.launch {
                        runCatching { container.transactionRepository.updateProvider(provider, value) }
                    }
                })
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.last_notification_status), style = MaterialTheme.typography.titleMedium)
        val last = me?.last_notification
        Text(if (last == null) "—" else "${last.provider}: ${last.processing_status} @ ${last.received_at}")

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.subscription), style = MaterialTheme.typography.titleMedium)
        val license = me?.license
        Text("${stringResource(R.string.activated_on)}: ${license?.activated_at ?: "—"}")
        Text("${stringResource(R.string.expires_on)}: ${license?.expires_at ?: "—"}")
        Text(stringResource(R.string.days_remaining, license?.days_remaining ?: 0))
        when (license?.days_remaining) {
            30, 29, 28 -> Text(stringResource(R.string.expiry_reminder_30), color = MaterialTheme.colorScheme.error)
            7, 6, 5 -> Text(stringResource(R.string.expiry_reminder_7), color = MaterialTheme.colorScheme.error)
            1 -> Text(stringResource(R.string.expiry_reminder_1), color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.privacy), style = MaterialTheme.typography.titleMedium)
        Text(
            "PayLedger processes only notifications from supported payment apps after you grant access. Transaction details are stored in your account. Notification text is not bank proof.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ErrorText(error)
        Spacer(Modifier.height(20.dp))
        PlSecondaryButton(stringResource(R.string.logout)) {
            scope.launch {
                container.authRepository.logout()
                onLoggedOut()
            }
        }
        Spacer(Modifier.height(8.dp))
        PlSecondaryButton(stringResource(R.string.delete_account)) { confirmDelete = true }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_account)) },
            text = { Text(stringResource(R.string.delete_account_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        try {
                            container.authRepository.deleteAccount()
                            onLoggedOut()
                        } catch (e: Exception) {
                            error = friendlyError(e)
                        }
                    }
                }) { Text(stringResource(R.string.confirm_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
