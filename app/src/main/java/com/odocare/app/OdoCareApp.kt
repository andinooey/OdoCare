package com.odocare.app
import android.app.Application
import com.odocare.app.data.Repo
import com.odocare.app.notif.Notif
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OdoCareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notif.setupChannels(this)
        Notif.scheduleDaily(this)
        CoroutineScope(Dispatchers.IO).launch { Repo(this@OdoCareApp).seedMasterIfNeeded() }
    }
}
