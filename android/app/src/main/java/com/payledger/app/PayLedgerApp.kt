package com.payledger.app

import android.app.Application
import com.payledger.app.data.AppContainer

class PayLedgerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
    }

    companion object {
        lateinit var instance: PayLedgerApp
            private set
    }
}
