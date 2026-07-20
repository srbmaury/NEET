package com.neet.app.pdf

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri

/** Writes [document] to [uri] (from the system "Save As" picker) and closes the document either
 * way — a leaked PdfDocument holds native memory until finalized. */
fun writePdfToUri(context: Context, document: PdfDocument, uri: Uri) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            document.writeTo(out)
        } ?: error("Could not open an output stream for the chosen location")
    } finally {
        document.close()
    }
}
