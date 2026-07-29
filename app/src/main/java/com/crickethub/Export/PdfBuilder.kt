package com.crickethub.export

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reusable PDF builder with branded header, tables, key-value rows,
 * section titles, and automatic pagination.
 *
 * Usage:
 *   val builder = PdfBuilder(context)
 *   builder.drawBrandedHeader("Match Scorecard", "Team A vs Team B")
 *   builder.drawSectionTitle("1st Innings - Team A")
 *   builder.drawTable(headers, rows, colWidths)
 *   val uri = builder.save("match_report.pdf")
 *   shareFile(context, uri, "application/pdf")
 */
class PdfBuilder(private val context: Context) {

    companion object {
        const val PAGE_W = 595f   // A4 points
        const val PAGE_H = 842f
        const val MARGIN = 36f
        const val CONTENT_W = PAGE_W - 2 * MARGIN
        const val ROW_H = 18f
        const val HEADER_ROW_H = 20f

        // Brand colors
        val C_PRIMARY = Color.parseColor("#10B981")
        val C_PRIMARY_DARK = Color.parseColor("#059669")
        val C_HEADER_BG = Color.parseColor("#1F2937")
        val C_ROW_ALT = Color.parseColor("#F3F4F6")
        val C_BORDER = Color.parseColor("#D1D5DB")
        val C_TEXT = Color.parseColor("#111827")
        val C_TEXT_SEC = Color.parseColor("#6B7280")
        val C_WHITE = Color.WHITE
        val C_RED = Color.parseColor("#EF4444")
        val C_BLUE = Color.parseColor("#3B82F6")
    }

    private val document = PdfDocument()
    private var page: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    var y = MARGIN
        private set
    private var pageNum = 0

    // ── Paints ───────────────────────────────────────────────

    private val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = C_TEXT; textSize = 16f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val paintSubtitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = C_TEXT_SEC; textSize = 10f
    }
    private val paintSection = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = C_PRIMARY_DARK; textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val paintBody = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = C_TEXT; textSize = 9f
    }
    private val paintBodyBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = C_TEXT; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val paintSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = C_TEXT_SEC; textSize = 7.5f
    }
    private val paintHeaderCell = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = C_WHITE; textSize = 8f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val paintCell = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = C_TEXT; textSize = 8f
    }
    private val paintLine = Paint().apply {
        color = C_BORDER; strokeWidth = 0.5f; style = Paint.Style.STROKE
    }
    private val paintFill = Paint().apply { style = Paint.Style.FILL }

    // ── Page management ──────────────────────────────────────

    fun newPage() {
        page?.let { document.finishPage(it) }
        pageNum++
        val info = PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), pageNum).create()
        page = document.startPage(info)
        canvas = page!!.canvas
        y = MARGIN
    }

    fun ensureSpace(needed: Float) {
        if (canvas == null) newPage()
        if (y + needed > PAGE_H - MARGIN - 20) {
            drawPageFooter()
            newPage()
        }
    }

    private fun c(): Canvas {
        if (canvas == null) newPage()
        return canvas!!
    }

    // ── Branded header ───────────────────────────────────────

    fun drawBrandedHeader(title: String, subtitle: String = "", info: List<String> = emptyList()) {
        ensureSpace(80f)
        val cv = c()

        // Green header bar
        paintFill.color = C_PRIMARY
        cv.drawRect(0f, 0f, PAGE_W, 52f, paintFill)

        // App name
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = C_WHITE; textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        cv.drawText("CricketHub", MARGIN, 28f, brandPaint)

        // Cricket ball icon (simple circle)
        paintFill.color = C_WHITE
        cv.drawCircle(PAGE_W - MARGIN - 12, 26f, 10f, paintFill)
        paintFill.color = C_PRIMARY
        cv.drawCircle(PAGE_W - MARGIN - 12, 26f, 7f, paintFill)

        // Tagline
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(200, 255, 255, 255); textSize = 8f }
        cv.drawText("Professional Cricket Scoring", MARGIN, 42f, tagPaint)

        y = 62f

        // Title
        cv.drawText(title, MARGIN, y, paintTitle)
        y += 18f

        // Subtitle
        if (subtitle.isNotBlank()) {
            cv.drawText(subtitle, MARGIN, y, paintSubtitle)
            y += 14f
        }

        // Info lines (date, venue, etc.)
        info.forEach { line ->
            cv.drawText(line, MARGIN, y, paintSmall)
            y += 11f
        }

        // Divider
        y += 4f
        paintFill.color = C_PRIMARY
        cv.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 1.5f, paintFill)
        y += 8f
    }

    // ── Section title ────────────────────────────────────────

    fun drawSectionTitle(text: String) {
        ensureSpace(24f)
        val cv = c()
        y += 6f
        // Small green bar
        paintFill.color = C_PRIMARY
        cv.drawRect(MARGIN, y - 9f, MARGIN + 3f, y + 3f, paintFill)
        cv.drawText(text, MARGIN + 8f, y, paintSection)
        y += 14f
    }

    // ── Table ────────────────────────────────────────────────

    /**
     * Draw a table with header row and data rows.
     * @param headers column header texts
     * @param rows list of row data (each row = list of cell strings)
     * @param colWidths relative column widths (will be normalized to CONTENT_W)
     * @param rightAlignFrom index from which columns are right-aligned (default: 1 = all except first)
     */
    fun drawTable(
        headers: List<String>,
        rows: List<List<String>>,
        colWidths: List<Float>,
        rightAlignFrom: Int = 1
    ) {
        val totalW = colWidths.sum()
        val widths = colWidths.map { it / totalW * CONTENT_W }

        // Header row
        ensureSpace(HEADER_ROW_H + ROW_H * minOf(rows.size, 3).toFloat())
        val cv = c()
        paintFill.color = C_HEADER_BG
        cv.drawRect(MARGIN, y, MARGIN + CONTENT_W, y + HEADER_ROW_H, paintFill)

        var x = MARGIN
        headers.forEachIndexed { i, h ->
            val tx = if (i >= rightAlignFrom) x + widths[i] - 4 else x + 4
            val align = if (i >= rightAlignFrom) Paint.Align.RIGHT else Paint.Align.LEFT
            paintHeaderCell.textAlign = align
            cv.drawText(h, tx, y + 14f, paintHeaderCell)
            x += widths[i]
        }
        y += HEADER_ROW_H

        // Data rows
        rows.forEachIndexed { rowIdx, row ->
            ensureSpace(ROW_H)
            // Alternating background
            if (rowIdx % 2 == 0) {
                paintFill.color = C_ROW_ALT
                c().drawRect(MARGIN, y, MARGIN + CONTENT_W, y + ROW_H, paintFill)
            }

            x = MARGIN
            row.forEachIndexed { colIdx, cell ->
                val tx = if (colIdx >= rightAlignFrom) x + widths[colIdx] - 4 else x + 4
                val align = if (colIdx >= rightAlignFrom) Paint.Align.RIGHT else Paint.Align.LEFT
                paintCell.textAlign = align
                // Truncate if too wide
                val maxChars = (widths[colIdx] / 4.5f).toInt().coerceAtLeast(3)
                val display = if (cell.length > maxChars) cell.take(maxChars - 1) + "…" else cell
                c().drawText(display, tx, y + 13f, paintCell)
                x += widths[colIdx]
            }
            // Bottom border
            c().drawLine(MARGIN, y + ROW_H, MARGIN + CONTENT_W, y + ROW_H, paintLine)
            y += ROW_H
        }
        y += 4f
    }

    // ── Key-value pairs ──────────────────────────────────────

    fun drawKeyValuePairs(pairs: List<Pair<String, String>>, columns: Int = 2) {
        val colW = CONTENT_W / columns
        var col = 0
        var startY = y

        pairs.forEach { (key, value) ->
            ensureSpace(16f)
            val cv = c()
            val x = MARGIN + col * colW
            paintSmall.textAlign = Paint.Align.LEFT
            cv.drawText(key, x + 4, y + 10f, paintSmall)
            paintBodyBold.textAlign = Paint.Align.LEFT
            cv.drawText(value, x + 4, y + 20f, paintBodyBold)

            col++
            if (col >= columns) {
                col = 0
                y += 26f
            }
        }
        if (col != 0) y += 26f
        y += 4f
    }

    // ── Simple text line ─────────────────────────────────────

    fun drawText(text: String, bold: Boolean = false, color: Int = C_TEXT, size: Float = 9f) {
        ensureSpace(14f)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; textSize = size; textAlign = Paint.Align.LEFT
            typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        }
        c().drawText(text, MARGIN, y + 10f, p)
        y += size + 4f
    }

    // ── Highlighted result row ───────────────────────────────

    fun drawResultBanner(text: String) {
        ensureSpace(28f)
        val cv = c()
        paintFill.color = C_PRIMARY
        cv.drawRect(MARGIN, y, MARGIN + CONTENT_W, y + 24f, paintFill)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = C_WHITE; textSize = 11f; textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        cv.drawText(text, PAGE_W / 2, y + 16f, p)
        y += 30f
    }

    // ── Spacer ───────────────────────────────────────────────

    fun space(amount: Float = 8f) { y += amount }

    // ── Page footer ──────────────────────────────────────────

    private fun drawPageFooter() {
        val cv = c()
        val footY = PAGE_H - 16f
        paintSmall.textAlign = Paint.Align.LEFT
        cv.drawText("Generated by CricketHub", MARGIN, footY, paintSmall)
        paintSmall.textAlign = Paint.Align.CENTER
        cv.drawText("Page $pageNum", PAGE_W / 2, footY, paintSmall)
        paintSmall.textAlign = Paint.Align.RIGHT
        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        cv.drawText(dateStr, PAGE_W - MARGIN, footY, paintSmall)
        // Green line above footer
        paintFill.color = C_PRIMARY
        cv.drawRect(MARGIN, footY - 10f, PAGE_W - MARGIN, footY - 9f, paintFill)
    }

    // ── Save & share ─────────────────────────────────────────

    fun save(filename: String): Uri {
        drawPageFooter()
        page?.let { document.finishPage(it) }

        val dir = File(context.cacheDir, "reports")
        dir.mkdirs()
        val file = File(dir, filename)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        return FileProvider.getUriForFile(
            context, "${context.packageName}.provider", file
        )
    }
}

// ── File sharing utility ─────────────────────────────────────

fun shareFile(context: Context, uri: Uri, mimeType: String, title: String = "Share Report") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

fun saveCsvFile(context: Context, filename: String, content: String): Uri {
    val dir = File(context.cacheDir, "reports")
    dir.mkdirs()
    val file = File(dir, filename)
    file.writeText(content)
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}