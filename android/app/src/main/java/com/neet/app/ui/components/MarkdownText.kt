package com.neet.app.ui.components

import android.text.TextUtils
import android.widget.TextView
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin

// The model is trained overwhelmingly on standard LaTeX delimiter conventions ($...$,
// \(...\), \[...\]) and drifts back to them despite explicit prompt instructions.
// Markwon's ext-latex only recognizes double `$$` (for both inline and block), so any
// of these would otherwise render as literal text. Normalize defensively here rather
// than relying solely on prompt compliance.
private val singleDollarInline = Regex("""(?<!\$)\$(?!\$)([^$\n]+?)(?<!\$)\$(?!\$)""")
private val singleDollarLine = Regex("""(?m)^(\s*)\$(\s*)$""")
// Content tolerates one level of nested plain "(...)"/"[...]" — a naive [^)]*? content group
// breaks the moment the LaTeX itself contains a parenthesized sub-expression like "(2t_f)",
// leaving the whole \( \) pair unmatched and rendered as literal source text.
private val parenDelimited = Regex("""\\\(((?:[^()\n]|\([^()\n]*\))*)\\\)""")
private val bracketDelimited = Regex("""\\\[((?:[^\[\]\n]|\[[^\[\]\n]*\])*)\\]""")

// Catches a different drift: raw LaTeX commands (\frac, \times, \text, ...) sitting inside plain
// "(...)" parentheses with no math delimiter at all — not even \( \). A backslash followed by
// letters essentially never appears in ordinary parenthetical English text, so treating that as
// the signal (rather than e.g. presence of digits or symbols alone) keeps this from misfiring on
// normal asides like "(displacement in meters)". The content allows one level of nested "(...)"
// (e.g. "(h = ... (2t_f)^2)") since LaTeX expressions legitimately contain their own parens.
private val looseLatexInParens =
    Regex("""\(((?:[^()\n]|\([^()\n]*\))*\\[a-zA-Z]+(?:[^()\n]|\([^()\n]*\))*)\)""")

private fun normalizeLatexDelimiters(markdown: String): String {
    var normalized = singleDollarLine.replace(markdown) { match ->
        "${match.groupValues[1]}$$${match.groupValues[2]}"
    }
    normalized = singleDollarInline.replace(normalized) { match ->
        "$$" + match.groupValues[1] + "$$"
    }
    normalized = parenDelimited.replace(normalized) { match ->
        "$$" + match.groupValues[1] + "$$"
    }
    normalized = bracketDelimited.replace(normalized) { match ->
        "$$" + match.groupValues[1] + "$$"
    }
    normalized = looseLatexInParens.replace(normalized) { match ->
        "$$" + match.groupValues[1] + "$$"
    }
    return normalized
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    maxLines: Int = Int.MAX_VALUE,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val latexTextSizePx = remember(density) { with(density) { 16.sp.toPx() } }
    val markwon = remember(context, latexTextSizePx) {
        Markwon.builder(context)
            .usePlugin(MarkwonInlineParserPlugin.create())
            .usePlugin(
                JLatexMathPlugin.create(latexTextSizePx) { builder ->
                    builder.inlinesEnabled(true)
                },
            )
            .build()
    }
    val resolvedColor = if (style.color.isUnspecified) LocalContentColor.current else style.color
    val textColorArgb = resolvedColor.toArgb()
    val textSizeSp = if (style.fontSize.type == TextUnitType.Sp) style.fontSize.value else 16f

    AndroidView(
        modifier = modifier,
        factory = { TextView(it) },
        update = { textView ->
            textView.setTextColor(textColorArgb)
            textView.textSize = textSizeSp
            textView.maxLines = maxLines
            if (maxLines != Int.MAX_VALUE) {
                textView.ellipsize = TextUtils.TruncateAt.END
            }
            markwon.setMarkdown(textView, normalizeLatexDelimiters(markdown))
            // Markwon's core plugin sets a LinkMovementMethod on the TextView unconditionally
            // (to support tappable markdown links), which makes the TextView intercept touch
            // events for its own hit-testing — swallowing taps meant for an ancestor Compose
            // clickable (e.g. an option row Card) before they can register as a click. This
            // component is used purely for display; nothing here needs interactive links.
            textView.movementMethod = null
            textView.isClickable = false
            textView.isFocusable = false
        },
    )
}
