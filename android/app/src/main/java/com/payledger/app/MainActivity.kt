package com.payledger.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.payledger.app.ui.PayLedgerRoot
import com.payledger.app.ui.theme.PayLedgerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PayLedgerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PayLedgerRoot(
                        container = (application as PayLedgerApp).container
                    )
                }
            }
        }
    }
}
