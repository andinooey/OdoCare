package com.odocare.app
import android.app.Application
import com.odocare.app.notif.Notif
class OdoCareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notif.setupChannels(this)
        Notif.scheduleDaily(this)
    }
}
