package com.payledger.app.notification

data class ParsedPayment(
    val provider: String,
    val direction: String,
    val amount: Double?,
    val currency: String = "PKR",
    val senderName: String?,
    val senderIdentifier: String?,
    val reference: String?,
    val originalText: String,
    val reviewStatus: String,
    val confidentIncoming: Boolean,
)
