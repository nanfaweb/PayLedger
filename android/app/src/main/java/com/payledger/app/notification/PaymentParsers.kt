package com.payledger.app.notification

import java.util.Locale
import java.util.regex.Pattern

object PaymentParsers {
    private val amountPatterns = listOf(
        Pattern.compile(
            """(?i)(?:rs\.?|pkr|rupees?)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)"""
        ),
        Pattern.compile(
            """(?i)([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\s*(?:rs\.?|pkr|rupees?)"""
        ),
        // Urdu-ish / Roman Urdu cues
        Pattern.compile(
            """(?i)(?:received|recv|credit|credited|آپ کو|موصول)\D{0,20}([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)"""
        ),
    )

    private val phonePattern = Pattern.compile("""(?:\+92|0)?3[0-9]{9}""")
    private val refPatterns = listOf(
        Pattern.compile("""(?i)(?:tid|txn|trx|ref(?:erence)?|transaction(?:\s*id)?|id)[:\s#]*([A-Z0-9\-]{6,})"""),
        Pattern.compile("""(?i)\b([A-Z0-9]{8,})\b"""),
    )

    private val incomingKeywords = listOf(
        "received", "recv", "credited", "credit", "incoming", "deposit",
        "you have received", "payment received", "has sent", "sent you",
        "موصول", "وصول", "ملے", "جمع",
    )
    private val outgoingKeywords = listOf(
        "sent", "paid", "payment to", "debited", "debit", "withdraw", "outgoing",
        "transfer to", "آپ نے بھیجا", "ادائیگی کر دی",
    )
    private val promoKeywords = listOf(
        "offer", "promo", "discount", "cashback unlock", "advertisement", "marketing",
    )

    fun parse(provider: String, title: String?, text: String?, bigText: String?): ParsedPayment? {
        val combined = listOfNotNull(title, text, bigText)
            .joinToString(" ")
            .trim()
        if (combined.isBlank()) return null

        val lower = combined.lowercase(Locale.US)
        if (promoKeywords.any { lower.contains(it) } && !incomingKeywords.any { lower.contains(it) }) {
            return null
        }

        val direction = detectDirection(lower) ?: return null
        val amount = extractAmount(combined)
        val phone = extractPhone(combined)
        val reference = extractReference(combined)
        val sender = extractSender(provider, title, combined)

        val confident = direction == "incoming" && amount != null && amount > 0
        val review = when {
            !confident -> "needs_review"
            reference.isNullOrBlank() && sender.isNullOrBlank() -> "needs_review"
            else -> "confirmed"
        }

        // Only auto-create for incoming; outgoing can be flagged for review if amount present
        if (direction != "incoming") {
            return ParsedPayment(
                provider = provider,
                direction = direction,
                amount = amount,
                senderName = sender,
                senderIdentifier = phone,
                reference = reference,
                originalText = combined,
                reviewStatus = "needs_review",
                confidentIncoming = false,
            )
        }

        return ParsedPayment(
            provider = provider,
            direction = "incoming",
            amount = amount,
            senderName = sender,
            senderIdentifier = phone,
            reference = reference,
            originalText = combined,
            reviewStatus = review,
            confidentIncoming = confident,
        )
    }

    private fun detectDirection(lower: String): String? {
        val inScore = incomingKeywords.count { lower.contains(it) }
        val outScore = outgoingKeywords.count { lower.contains(it) }
        return when {
            inScore > outScore -> "incoming"
            outScore > inScore -> "outgoing"
            inScore > 0 -> "incoming"
            else -> null
        }
    }

    fun extractAmount(text: String): Double? {
        for (p in amountPatterns) {
            val m = p.matcher(text)
            if (m.find()) {
                val raw = m.group(1)?.replace(",", "") ?: continue
                return raw.toDoubleOrNull()
            }
        }
        return null
    }

    fun extractPhone(text: String): String? {
        val m = phonePattern.matcher(text.replace(" ", ""))
        return if (m.find()) m.group() else null
    }

    fun extractReference(text: String): String? {
        for (p in refPatterns) {
            val m = p.matcher(text)
            if (m.find()) {
                val g = m.group(1) ?: continue
                if (g.all { it.isDigit() } && g.length == 10 && g.startsWith("3")) continue
                return g
            }
        }
        return null
    }

    private fun extractSender(provider: String, title: String?, combined: String): String? {
        // Provider-specific heuristics
        val fromPatterns = listOf(
            Pattern.compile("""(?i)from\s+([A-Za-z\u0600-\u06FF .']{2,40})"""),
            Pattern.compile("""(?i)by\s+([A-Za-z\u0600-\u06FF .']{2,40})"""),
            Pattern.compile("""(?i)sender[:\s]+([A-Za-z\u0600-\u06FF .']{2,40})"""),
            Pattern.compile("""(?i)([A-Za-z\u0600-\u06FF .']{2,40})\s+has sent"""),
            Pattern.compile("""(?i)([A-Za-z\u0600-\u06FF .']{2,40})\s+sent you"""),
        )
        for (p in fromPatterns) {
            val m = p.matcher(combined)
            if (m.find()) {
                val name = m.group(1)?.trim()?.trimEnd('.', ',', ' ')
                if (!name.isNullOrBlank() && !name.equals(provider, true)) return name
            }
        }
        val t = title?.trim()
        if (!t.isNullOrBlank() && !t.contains("rs", true) && t.length in 2..40) {
            if (!t.equals("jazzcash", true) && !t.equals("easypaisa", true) &&
                !t.equals("sadapay", true) && !t.equals("nayapay", true)
            ) {
                return t
            }
        }
        return null
    }

    /**
     * Deduplication key: prefer reference; else hash-like combination that does NOT
     * treat same sender+amount alone as duplicate without time bucket.
     */
    fun dedupeKey(
        provider: String,
        reference: String?,
        amount: Double?,
        senderIdentifier: String?,
        senderName: String?,
        originalText: String,
        postedAtMs: Long,
    ): String {
        if (!reference.isNullOrBlank()) {
            return "$provider|ref|${reference.trim().uppercase(Locale.US)}"
        }
        // 2-minute bucket reduces duplicate notification posts without equating distinct payments
        val bucket = postedAtMs / 120_000L
        val basis = listOf(
            provider,
            amount?.toString() ?: "",
            senderIdentifier ?: "",
            senderName ?: "",
            originalText.take(120),
            bucket.toString(),
        ).joinToString("|")
        return "h|" + basis.hashCode().toUInt().toString(16)
    }
}
