package com.neet.app

import android.app.Application
import com.neet.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NeetApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)

        // Best-effort: a free-tier backend host can be spun down after inactivity and take up to
        // ~60s to wake on the next request. Firing this the moment the process starts means that
        // wake-up happens while the user is still looking at a topic picker, rather than being
        // eaten by their first real request. Result is intentionally ignored — this is purely to
        // nudge the host awake, not something the UI waits on or reacts to.
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { AppContainer.healthApiService.ping() }
        }
    }
}
