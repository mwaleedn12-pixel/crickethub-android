package com.crickethub

import android.app.Application
import com.crickethub.data.local.AppDatabase
import com.crickethub.data.sync.NetworkMonitor
import com.crickethub.data.sync.SyncManager

class CricketHubApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var networkMonitor: NetworkMonitor
        private set
    lateinit var syncManager: SyncManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        networkMonitor = NetworkMonitor(this)
        syncManager = SyncManager(database, networkMonitor)
    }

    companion object {
        lateinit var instance: CricketHubApp
            private set
    }
}