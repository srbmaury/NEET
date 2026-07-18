package com.neet.app

import android.app.Application
import com.neet.app.di.AppContainer

class NeetApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
