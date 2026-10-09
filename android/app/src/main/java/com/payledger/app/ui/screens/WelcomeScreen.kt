package com.payledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.payledger.app.R
import com.payledger.app.ui.components.PlPrimaryButton
import com.payledger.app.ui.components.PlSecondaryButton
import com.payledger.app.ui.theme.GreenSoft

@Composable
fun WelcomeScreen(onRegister: () -> Unit, onLogin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape).background(GreenSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text("PL", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(40.dp))
        PlPrimaryButton(stringResource(R.string.get_started), onClick = onRegister)
        Spacer(Modifier.height(12.dp))
        PlSecondaryButton(stringResource(R.string.already_have_account), onClick = onLogin)
    }
}
