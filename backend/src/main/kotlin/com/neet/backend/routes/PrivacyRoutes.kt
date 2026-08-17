package com.neet.backend.routes

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/** Public pages used by the app and the Google Play listing. Keep these routes unauthenticated. */
fun Route.privacyRoutes() {
    get("/privacy") {
        call.respondText(PRIVACY_POLICY, ContentType.Text.Html)
    }
    get("/account-deletion") {
        call.respondText(ACCOUNT_DELETION, ContentType.Text.Html)
    }
}

private const val PRIVACY_POLICY = """
<!doctype html><html lang="en"><head><meta charset="utf-8"><title>Neet Privacy Policy</title></head>
<body><main><h1>Neet Privacy Policy</h1><p>Last updated: August 18, 2026</p>
<p>Neet is a NEET exam-preparation app. We collect the email address and password you provide to create an optional account. Passwords are stored only as salted hashes.</p>
<p>When you sign in, we store your practice answers, mock-test activity, and revision progress so they can be synchronized across your devices. Photos submitted to the question solver are sent to our AI processing provider to generate a solution. Neet does not store submitted photos in its database.</p>
<p>We use this information only to provide and improve Neet's learning features. We do not sell personal data or use it for advertising.</p>
<p>Data is transmitted using HTTPS. Authentication tokens are encrypted on the device. You can delete your account and cloud-synced learning data from the app or through our <a href="/account-deletion">account deletion page</a>.</p>
<p>For privacy questions, contact <a href="mailto:srbmaury@gmail.com">srbmaury@gmail.com</a>.</p></main></body></html>
"""

private const val ACCOUNT_DELETION = """
<!doctype html><html lang="en"><head><meta charset="utf-8"><title>Delete your Neet account</title></head>
<body><main><h1>Delete your Neet account</h1><p>You can permanently delete your account and all cloud-synced learning data in Neet: open <strong>Progress</strong>, select <strong>Delete account</strong>, and confirm.</p>
<p>This removes your account, email address, and all Neet practice data, including cloud-synced answers and mock tests and data stored locally on the device.</p>
<p>If you cannot access the app, email <a href="mailto:srbmaury@gmail.com?subject=Neet%20account%20deletion">srbmaury@gmail.com</a> from the email address associated with your account.</p></main></body></html>
"""
