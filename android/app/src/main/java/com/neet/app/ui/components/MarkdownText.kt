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

// Everything below here runs only on text OUTSIDE already-valid $$...$$ blocks (see
// mathBlock/replaceOutsideMathBlocks) — these patterns match on a bare literal "(" or "\", with
// no awareness of surrounding context, so run unguarded they'll happily re-match content a
// well-formed $$ block already contains (e.g. the "(\rho)" annotation inside
// "$$\text{Density } (\rho) = ...$$") and shatter it into broken fragments.
//
// The four alternatives below are combined into one pattern and applied in a single pass — critical
// so an earlier alternative's replacement text (freshly inserted $$) is never re-scanned and
// double-wrapped by a later alternative.
private val latexOutsideMathBlocks = Regex(
    // 1: \( ... \) — content tolerates one level of nested plain parens, since a naive [^)]*?
    // content group breaks the moment the LaTeX itself contains a parenthesized sub-expression
    // like "(2t_f)", leaving the whole \( \) pair unmatched and rendered as literal source text.
    """\\\(((?:[^()\n]|\([^()\n]*\))*)\\\)""" +
        "|" +
        // 2: \[ ... \], same nested-bracket tolerance.
        """\\\[((?:[^\[\]\n]|\[[^\[\]\n]*\])*)\\]""" +
        "|" +
        // 3: raw LaTeX commands (\frac, \times, \text, ...) sitting inside plain "(...)" with no
        // math delimiter at all — not even \( \). A backslash followed by letters essentially
        // never appears in ordinary parenthetical English text, so treating that as the signal
        // keeps this from misfiring on normal asides like "(displacement in meters)". One level
        // of nested "(...)" tolerated, same as alternative 1.
        """\(((?:[^()\n]|\([^()\n]*\))*\\[a-zA-Z]+(?:[^()\n]|\([^()\n]*\))*)\)""" +
        "|" +
        // 4: a bare LaTeX command with NO delimiter at all, not even parens — e.g. an answer
        // option that's just "\frac{1}{2}Mv^2". Anchored to start at a backslash-command (the
        // same never-appears-in-prose signal as alternative 3), then greedily extends through
        // adjacent tight math tokens (letters/digits/operators/braces/more commands) with no
        // spaces, stopping at the first character that isn't plausibly still part of the
        // expression — deliberately conservative so it can't run on into surrounding prose.
        """(\\[a-zA-Z]+(?:\{[^{}\n]*\})*(?:\\[a-zA-Z]+(?:\{[^{}\n]*\})*|[A-Za-z0-9^_+\-*/=.])*)""",
)

private val mathBlock = Regex("""\$\$.*?\$\$""", RegexOption.DOT_MATCHES_ALL)

private fun replaceOutsideMathBlocks(text: String, transform: (String) -> String): String {
    val result = StringBuilder()
    var lastEnd = 0
    for (match in mathBlock.findAll(text)) {
        result.append(transform(text.substring(lastEnd, match.range.first)))
        result.append(match.value)
        lastEnd = match.range.last + 1
    }
    result.append(transform(text.substring(lastEnd)))
    return result.toString()
}

private fun normalizeLatexDelimiters(markdown: String): String {
    var normalized = singleDollarLine.replace(markdown) { match ->
        "${match.groupValues[1]}$$${match.groupValues[2]}"
    }
    normalized = singleDollarInline.replace(normalized) { match ->
        "$$" + match.groupValues[1] + "$$"
    }
    normalized = replaceOutsideMathBlocks(normalized) { segment ->
        latexOutsideMathBlocks.replace(segment) { match ->
            val inner = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: match.value
            "$$" + inner + "$$"
        }
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
