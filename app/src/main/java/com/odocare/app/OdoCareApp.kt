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
        Notif.schedulePolling(this)
        CoroutineScope(Dispatchers.IO).launch {
            val repo = Repo(this@OdoCareApp)
            repo.seedMasterIfNeeded()
            repo.seedVehicleTypesIfNeeded()
            repo.migrateDocNotifPrefs()
            Notif.onDataChanged(this@OdoCareApp)   // cek sekarang + pasang alarm tepat waktu berikutnya
        }
    }
}
