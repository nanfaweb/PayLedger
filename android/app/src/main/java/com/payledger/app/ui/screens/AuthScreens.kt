package com.payledger.app.ui.screens

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
import com.payledger.app.data.repository.AuthRepository
import com.payledger.app.ui.components.ErrorText
import com.payledger.app.ui.components.PlPrimaryButton
import com.payledger.app.ui.components.PlTextField
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onSuccess: () -> Unit,
    onRegister: () -> Unit,
    onForgot: () -> Unit,
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AuthForm(title = stringResource(R.string.login)) {
        PlTextField(phone, { phone = it }, stringResource(R.string.phone_number), keyboardType = KeyboardType.Phone)
        PlTextField(password, { password = it }, stringResource(R.string.password), isPassword = true)
        ErrorText(error)
        PlPrimaryButton(stringResource(R.string.login), loading = loading) {
            loading = true
            error = null
            scope.launch {
                try {
                    authRepository.login(phone, password)
                    onSuccess()
                } catch (e: Exception) {
                    error = friendlyError(e)
                } finally {
                    loading = false
                }
            }
        }
        TextButton(onClick = onForgot) { Text(stringResource(R.string.forgot_password)) }
        TextButton(onClick = onRegister) { Text(stringResource(R.string.register)) }
    }
}

@Composable
fun RegisterScreen(
    authRepository: AuthRepository,
    onSuccess: () -> Unit,
    onLogin: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AuthForm(title = stringResource(R.string.register)) {
        PlTextField(name, { name = it }, stringResource(R.string.full_name))
        PlTextField(phone, { phone = it }, stringResource(R.string.phone_number), keyboardType = KeyboardType.Phone)
        PlTextField(password, { password = it }, stringResource(R.string.password), isPassword = true)
        PlTextField(confirm, { confirm = it }, stringResource(R.string.confirm_password), isPassword = true)
        ErrorText(error)
        PlPrimaryButton(stringResource(R.string.register), loading = loading) {
            if (password != confirm) {
                error = "Passwords do not match"
                return@PlPrimaryButton
            }
            loading = true
            error = null
            scope.launch {
                try {
                    authRepository.register(name, phone, password, confirm)
                    onSuccess()
                } catch (e: Exception) {
                    error = friendlyError(e)
                } finally {
                    loading = false
                }
            }
        }
        TextButton(onClick = onLogin) { Text(stringResource(R.string.login)) }
    }
}

@Composable
fun ResetPasswordScreen(
    authRepository: AuthRepository,
    onDone: () -> Unit,
) {
    var phone by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AuthForm(title = stringResource(R.string.reset_password)) {
        Text(
            "Ask the developer for a temporary reset token, then enter it below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PlTextField(phone, { phone = it }, stringResource(R.string.phone_number), keyboardType = KeyboardType.Phone)
        PlTextField(token, { token = it }, stringResource(R.string.reset_token))
        PlTextField(password, { password = it }, stringResource(R.string.new_password), isPassword = true)
        PlTextField(confirm, { confirm = it }, stringResource(R.string.confirm_password), isPassword = true)
        ErrorText(error)
        if (success) Text("Password updated. You can log in.", color = MaterialTheme.colorScheme.primary)
        PlPrimaryButton(stringResource(R.string.reset_password), loading = loading) {
            loading = true
            error = null
            scope.launch {
                try {
                    authRepository.resetPassword(phone, token, password, confirm)
                    success = true
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

@Composable
private fun AuthForm(title: String, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(20.dp))
        content()
    }
}

fun friendlyError(e: Exception): String {
    return when (e) {
        is HttpException -> {
            val body = e.response()?.errorBody()?.string().orEmpty()
            Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.getOrNull(1)
                ?: "Request failed (${e.code()})"
        }
        else -> e.message ?: "Something went wrong"
    }
}
