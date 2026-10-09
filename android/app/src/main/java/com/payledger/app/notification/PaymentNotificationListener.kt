package com.payledger.app.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.payledger.app.PayLedgerApp
import com.payledger.app.data.model.CreateTransactionRequest
import java.time.Instant

class PaymentNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        try {
            process(sbn)
        } catch (_: Exception) {
            // Provider format changes must never crash the service
            NotificationIngestBus.publishStatus("parse_failed")
        }
    }

    private fun process(sbn: StatusBarNotification) {
        val provider = ProviderPackages.providerFor(sbn.packageName) ?: return
        val prefs = PayLedgerApp.instance.container.providerPrefs
        if (!prefs.isEnabled(provider)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        val parsed = PaymentParsers.parse(provider, title, text, bigText) ?: return
        if (!parsed.confidentIncoming && parsed.direction != "incoming") return
        if (parsed.amount == null && parsed.reviewStatus != "needs_review") return

        // Skip pure outgoing without enough signal
        if (parsed.direction == "outgoing" && !parsed.confidentIncoming) return

        val posted = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()
        val dedupe = PaymentParsers.dedupeKey(
            provider = provider,
            reference = parsed.reference,
            amount = parsed.amount,
            senderIdentifier = parsed.senderIdentifier,
            senderName = parsed.senderName,
            originalText = parsed.originalText,
            postedAtMs = posted,
        )

        // Only upload eligible payment notifications — never arbitrary phone notifications
        if (!parsed.confidentIncoming && parsed.amount == null) return

        val request = CreateTransactionRequest(
            provider = provider,
            sender_name = parsed.senderName,
            sender_identifier = parsed.senderIdentifier,
            amount = parsed.amount ?: 0.0,
            currency = parsed.currency,
            transaction_reference = parsed.reference,
            transaction_timestamp = Instant.ofEpochMilli(posted).toString(),
            transaction_direction = "incoming",
            original_notification = parsed.originalText.take(4000),
            source_type = "notification",
            review_status = if (parsed.confidentIncoming) parsed.reviewStatus else "needs_review",
            deduplication_key = dedupe,
        )

        NotificationIngestBus.publish(request)
        NotificationIngestBus.publishStatus("queued:$provider")
    }
}
