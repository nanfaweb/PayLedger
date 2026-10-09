package com.payledger.app.ui.util

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Format {
    private val amountFmt = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }

    private val dateTimeFmt = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.US)
        .withZone(ZoneId.of("Asia/Karachi"))

    fun pkr(amount: Double): String = amountFmt.format(amount)

    fun dateTime(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        return try {
            dateTimeFmt.format(Instant.parse(iso))
        } catch (_: Exception) {
            iso
        }
    }

    fun providerLabel(provider: String): String = when (provider.lowercase(Locale.US)) {
        "jazzcash" -> "JazzCash"
        "easypaisa" -> "Easypaisa"
        "sadapay" -> "SadaPay"
        "nayapay" -> "NayaPay"
        else -> provider.replaceFirstChar { it.uppercase() }
    }
}
