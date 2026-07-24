package com.neet.app.pdf

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Opens a just-exported PDF directly in whatever viewer the device has, rather than leaving the
 * user to hunt for it in Files/Downloads after a bare "Saved" toast — the SAF picker lets them
 * save to any provider/folder, so a silent save is easy to lose track of.
 */
fun openPdf(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Open PDF"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Saved, but no app found to open PDFs", Toast.LENGTH_LONG).show()
    }
}
