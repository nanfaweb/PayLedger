package com.payledger.app.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentParsersTest {
    @Test
    fun parsesJazzCashIncoming() {
        val parsed = PaymentParsers.parse(
            "jazzcash",
            "JazzCash",
            "You have received Rs 1,500.00 from Ali Khan. TID: JC12345678",
            null,
        )
        assertNotNull(parsed)
        assertEquals("incoming", parsed!!.direction)
        assertEquals(1500.0, parsed.amount!!, 0.01)
        assertTrue(parsed.confidentIncoming)
        assertEquals("JC12345678", parsed.reference)
    }

    @Test
    fun parsesEasypaisaIncoming() {
        val parsed = PaymentParsers.parse(
            "easypaisa",
            "Easypaisa",
            "Rs 250 received from 03001234567. Ref EP998877",
            null,
        )
        assertNotNull(parsed)
        assertEquals(250.0, parsed!!.amount!!, 0.01)
        assertNotNull(parsed.senderIdentifier)
    }

    @Test
    fun ignoresPromo() {
        val parsed = PaymentParsers.parse(
            "sadapay",
            "SadaPay",
            "Limited offer! Unlock cashback discount today",
            null,
        )
        assertNull(parsed)
    }

    @Test
    fun flagsNeedsReviewWithoutAmount() {
        val parsed = PaymentParsers.parse(
            "nayapay",
            "NayaPay",
            "You have received a payment from Sara",
            null,
        )
        assertNotNull(parsed)
        assertFalse(parsed!!.confidentIncoming)
        assertEquals("needs_review", parsed.reviewStatus)
    }

    @Test
    fun dedupeUsesReferenceWhenPresent() {
        val a = PaymentParsers.dedupeKey("jazzcash", "ABC123", 100.0, null, null, "text", 1000)
        val b = PaymentParsers.dedupeKey("jazzcash", "ABC123", 999.0, null, null, "other", 99999)
        assertEquals(a, b)
    }

    @Test
    fun dedupeDoesNotCollapseSameSenderAmountAcrossBuckets() {
        val a = PaymentParsers.dedupeKey("easypaisa", null, 100.0, "03001112222", "Ali", "recv 100", 0)
        val b = PaymentParsers.dedupeKey("easypaisa", null, 100.0, "03001112222", "Ali", "recv 100", 200_000)
        assertNotEquals(a, b)
    }
}
