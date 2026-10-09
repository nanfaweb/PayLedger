package com.payledger.app.report

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import com.payledger.app.data.model.Transaction
import com.payledger.app.ui.util.Format
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ReportOptions(
    val accountName: String,
    val periodLabel: String,
    val language: String,
    val transactions: List<Transaction>,
)

class PdfReportGenerator(private val context: Context) {
    fun generate(options: ReportOptions): File {
        val rtl = options.language == "ur"
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "payledger-report-${System.currentTimeMillis()}.pdf")

        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val lineHeight = 16f

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = if (rtl) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = if (rtl) Paint.Align.RIGHT else Paint.Align.LEFT
        }
        val headerPaint = TextPaint(bodyPaint).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val incoming = options.transactions.filter { it.transaction_direction == "incoming" }
        val total = incoming.sumOf { it.amount }
        val byProvider = incoming.groupBy { it.provider }.mapValues { e -> e.value.sumOf { it.amount } }
        val generated = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.US)
            .withZone(ZoneId.of("Asia/Karachi"))
            .format(Instant.now())

        var pageNumber = 1
        var pageInfo = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = pageInfo.canvas
        var y = margin

        fun xStart(): Float = if (rtl) pageWidth - margin else margin
        fun newPage() {
            doc.finishPage(pageInfo)
            pageNumber++
            pageInfo = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = pageInfo.canvas
            y = margin
            canvas.drawText("PayLedger — ${options.periodLabel} (cont.)", xStart(), y, headerPaint)
            y += lineHeight * 1.5f
            drawTableHeader()
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - margin) newPage()
        }

        fun drawTableHeader() {
            canvas.drawText("Date", if (rtl) xStart() - 0 else margin, y, headerPaint)
            canvas.drawText("Sender", margin + 110, y, headerPaint)
            canvas.drawText("Provider", margin + 260, y, headerPaint)
            canvas.drawText("Amount", margin + 360, y, headerPaint)
            canvas.drawText("Ref", margin + 450, y, headerPaint)
            y += lineHeight
        }

        canvas.drawText("PayLedger", xStart(), y, titlePaint)
        y += lineHeight * 1.4f
        canvas.drawText(options.accountName, xStart(), y, bodyPaint)
        y += lineHeight
        canvas.drawText("Period: ${options.periodLabel}", xStart(), y, bodyPaint)
        y += lineHeight
        canvas.drawText("Generated: $generated", xStart(), y, bodyPaint)
        y += lineHeight * 1.5f
        canvas.drawText("Total incoming: Rs ${Format.pkr(total)}", xStart(), y, headerPaint)
        y += lineHeight
        canvas.drawText("Transactions: ${incoming.size}", xStart(), y, bodyPaint)
        y += lineHeight * 1.2f
        byProvider.forEach { (p, amt) ->
            canvas.drawText("${Format.providerLabel(p)}: Rs ${Format.pkr(amt)}", xStart(), y, bodyPaint)
            y += lineHeight
        }
        y += lineHeight
        drawTableHeader()

        incoming.forEach { tx ->
            ensureSpace(lineHeight * 2)
            val date = Format.dateTime(tx.transaction_timestamp).take(17)
            val sender = (tx.sender_name ?: "Unknown").take(18)
            val provider = Format.providerLabel(tx.provider).take(10)
            val amount = Format.pkr(tx.amount)
            val ref = (tx.transaction_reference ?: "—").take(10)
            canvas.drawText(date, margin, y, bodyPaint)
            canvas.drawText(sender, margin + 110, y, bodyPaint)
            canvas.drawText(provider, margin + 260, y, bodyPaint)
            canvas.drawText(amount, margin + 360, y, bodyPaint)
            canvas.drawText(ref, margin + 450, y, bodyPaint)
            y += lineHeight
            if (!tx.notes.isNullOrBlank()) {
                ensureSpace(lineHeight)
                canvas.drawText("Note: ${tx.notes.take(80)}", margin + 12, y, bodyPaint)
                y += lineHeight
            }
        }

        y += lineHeight
        ensureSpace(lineHeight * 2)
        canvas.drawText("Report total: Rs ${Format.pkr(total)} PKR", xStart(), y, headerPaint)
        canvas.drawText("Page $pageNumber", pageWidth - margin - 40, pageHeight - 20f, bodyPaint)

        doc.finishPage(pageInfo)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
