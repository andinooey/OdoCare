package com.odocare.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.odocare.app.data.*
import com.odocare.app.domain.*
import com.odocare.app.notif.NotificationHelper
import com.odocare.app.notif.Rescheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

fun MaintenanceSchedule.input() = ServiceInput(intervalKm, lastKm, intervalMonths,
    lastEpochDay?.let { LocalDate.ofEpochDay(it) }, warnKm, warnDays)

class MainViewModel(private val app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()
    private fun <T> Flow<T>.state(init: T) = stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), init)

    val vehicles = dao.vehicles().state(emptyList())
    val schedules = dao.schedules().state(emptyList())
    val logs = dao.logs().state(emptyList())
    val services = dao.services().state(emptyList())
    val documents = dao.documents().state(emptyList())
    val docHistory = dao.docHistory().state(emptyList())
    val selectedId = MutableStateFlow<String?>(null)
    val showOdo = MutableStateFlow(false)
    val current = combine(vehicles, selectedId) { v, id -> v.firstOrNull { it.id == id } ?: v.firstOrNull() }.state(null)

    private fun io(block: suspend () -> Unit) = viewModelScope.launch(Dispatchers.IO) { block() }

    fun openOdometerFor(id: String) { selectedId.value = id; showOdo.value = true }

    fun addVehicle(v: Vehicle) = io {
        dao.upsertVehicle(v)
        if (v.currentKm != null) dao.insertLog(OdometerLog(vehicleId = v.id, km = v.currentKm, recordedAt = System.currentTimeMillis(), note = "Kilometer awal"))
        selectedId.value = v.id
        Rescheduler.rescheduleAll(app)
    }

    fun archiveVehicle(id: String) = io { dao.archiveVehicle(id); Rescheduler.rescheduleAll(app) }

    fun saveOdometer(v: Vehicle, km: Long, note: String) = io {
        val now = System.currentTimeMillis()
        dao.insertLog(OdometerLog(vehicleId = v.id, km = km, recordedAt = now, note = note))
        dao.upsertVehicle(v.copy(currentKm = km, lastOdoAt = now))
        alertSchedules(v, km)
        Rescheduler.rescheduleAll(app)
    }

    /** Notifikasi hanya saat status naik ke level baru (tidak berulang untuk status yang sama). */
    private suspend fun alertSchedules(v: Vehicle, km: Long) {
        dao.schedulesFor(v.id).forEach { s ->
            val st = ServiceEngine.evaluate(s.input(), km, LocalDate.now()).status
            if (st.urgency >= 1 && s.lastNotified != st.name) {
                NotificationHelper.show(app, s.id.hashCode(), NotificationHelper.CH_SERVIS,
                    "Servis: ${s.name}", "${v.name} — ${st.label}", v.id)
                dao.upsertSchedule(s.copy(lastNotified = st.name))
            } else if (st.urgency == 0 && s.lastNotified != null) dao.upsertSchedule(s.copy(lastNotified = null))
        }
    }

    fun deleteLog(id: String) = io { dao.deleteLog(id) }

    fun saveSchedule(s: MaintenanceSchedule) = io { dao.upsertSchedule(s) }
    fun archiveSchedule(id: String) = io { dao.archiveSchedule(id) }

    fun logService(s: MaintenanceSchedule, v: Vehicle, km: Long, epochDay: Long, cost: Long) = io {
        dao.insertService(ServiceRecord(vehicleId = v.id, scheduleId = s.id, name = s.name, epochDay = epochDay, km = km, cost = cost))
        dao.upsertSchedule(s.copy(lastKm = km, lastEpochDay = epochDay, lastNotified = null))
        if (v.currentKm == null || km > v.currentKm) {
            val now = System.currentTimeMillis()
            dao.insertLog(OdometerLog(vehicleId = v.id, km = km, recordedAt = now, note = "Dari catatan servis"))
            dao.upsertVehicle(v.copy(currentKm = km, lastOdoAt = now))
        }
        Rescheduler.rescheduleAll(app)
    }

    fun saveDocument(d: LegalDocument) = io { dao.upsertDocument(d); Rescheduler.rescheduleAll(app) }
    fun archiveDocument(id: String) = io { dao.archiveDocument(id); Rescheduler.rescheduleAll(app) }

    fun renewDocument(d: LegalDocument, newDue: LocalDate, amount: Long?) = io {
        val today = LocalDate.now().toEpochDay()
        dao.insertDocHistory(DocumentHistory(documentId = d.id, actionEpochDay = today, amount = amount,
            oldDueEpochDay = d.dueEpochDay, newDueEpochDay = newDue.toEpochDay()))
        dao.upsertDocument(d.copy(dueEpochDay = newDue.toEpochDay(), lastPaidEpochDay = today, amount = amount))
        Rescheduler.rescheduleAll(app)
    }
}
