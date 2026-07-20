package com.neet.app.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.view.View
import android.widget.TextView
import com.neet.app.ui.components.buildMarkwon
import com.neet.app.ui.components.normalizeLatexDelimiters
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.min

// A4 at ~150dpi — high enough that embedded LaTeX bitmaps (JLatexMathPlugin renders at a fixed
// pixel size, not vector) stay crisp when the PDF is viewed or printed at real size.
private const val PAGE_WIDTH_PX = 1240
private const val PAGE_HEIGHT_PX = 1754
private const val MARGIN_PX = 60
private const val CONTENT_WIDTH_PX = PAGE_WIDTH_PX - MARGIN_PX * 2
private const val CONTENT_HEIGHT_PX = PAGE_HEIGHT_PX - MARGIN_PX * 2
private const val BODY_TEXT_SIZE_PX = 28f

// JLatexMathPlugin normally renders formula bitmaps on a background executor, then invalidates
// the live TextView once ready — a mechanism built for on-screen views attached to a real window.
// This renderer builds an orphan, never-attached TextView specifically so it can call
// measure/layout/draw on its own timeline for pagination; there's no window to receive that
// invalidate callback. Running the executor same-thread instead makes every LaTeX bitmap finish
// synchronously inside setMarkdown() itself, before measure/layout/draw ever runs — no race.
private val directExecutor = object : AbstractExecutorService() {
    override fun execute(command: Runnable) = command.run()
    override fun shutdown() = Unit
    override fun shutdownNow(): MutableList<Runnable> = mutableListOf()
    override fun isShutdown() = true
    override fun isTerminated() = true
    override fun awaitTermination(timeout: Long, unit: TimeUnit) = true
}

/**
 * Renders [markdown] (using the exact same normalization + Markwon/LaTeX pipeline as the on-screen
 * [com.neet.app.ui.components.MarkdownText]) into a paginated [PdfDocument]. Pagination always cuts
 * at a full text-line boundary — never mid-line — by consulting the laid-out [android.text.Layout]
 * directly rather than guessing a fixed lines-per-page count.
 *
 * Caller owns the returned document: write it and call `.close()`.
 */
fun renderMarkdownToPdf(context: Context, markdown: String): PdfDocument {
    val textView = TextView(context).apply {
        setTextColor(Color.BLACK)
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, BODY_TEXT_SIZE_PX)
    }
    val markwon = buildMarkwon(context, BODY_TEXT_SIZE_PX, directExecutor)
    markwon.setMarkdown(textView, normalizeLatexDelimiters(markdown))

    val widthSpec = View.MeasureSpec.makeMeasureSpec(CONTENT_WIDTH_PX, View.MeasureSpec.EXACTLY)
    val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    textView.measure(widthSpec, heightSpec)
    textView.layout(0, 0, textView.measuredWidth, textView.measuredHeight)

    val layout = textView.layout
    val totalHeight = textView.measuredHeight
    val document = PdfDocument()

    if (layout == null || totalHeight <= 0) {
        // Nothing rendered (e.g. empty input) — still return a valid one-page document.
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH_PX, PAGE_HEIGHT_PX, 1).create())
        document.finishPage(page)
        return document
    }

    var currentY = 0
    var pageNumber = 1
    while (currentY < totalHeight) {
        val targetBottom = min(currentY + CONTENT_HEIGHT_PX, totalHeight)
        val startLine = layout.getLineForVertical(currentY)
        var cutLine = layout.getLineForVertical(targetBottom - 1)
        if (targetBottom < totalHeight && layout.getLineBottom(cutLine) > targetBottom && cutLine > startLine) {
            cutLine -= 1
        }
        val pageEndY = if (targetBottom >= totalHeight) totalHeight else layout.getLineBottom(cutLine)
        // Safety net: a single line taller than one page's content height (shouldn't happen at
        // this body text size, but would otherwise spin forever re-selecting the same cut point).
        val effectiveEndY = if (pageEndY <= currentY) targetBottom else pageEndY

        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PX, PAGE_HEIGHT_PX, pageNumber).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        canvas.save()
        canvas.clipRect(MARGIN_PX, MARGIN_PX, PAGE_WIDTH_PX - MARGIN_PX, PAGE_HEIGHT_PX - MARGIN_PX)
        canvas.translate(MARGIN_PX.toFloat(), MARGIN_PX.toFloat() - currentY)
        textView.draw(canvas)
        canvas.restore()
        document.finishPage(page)

        currentY = effectiveEndY
        pageNumber += 1
    }

    return document
}
