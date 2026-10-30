package com.odocare.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.odocare.app.data.*
import com.odocare.app.domain.CatalogItem
import com.odocare.app.domain.Engines
import com.odocare.app.domain.ScheduleView
import com.odocare.app.notif.Notif
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repo(app)
    private val prefs = app.getSharedPreferences("odocare", Context.MODE_PRIVATE)

    val vehicles: StateFlow<List<Vehicle>> = repo.vehicles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeVehicle: StateFlow<Vehicle?> = repo.activeVehicle.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val currentKm: StateFlow<Int?> = repo.activeVehicle.flatMapLatest { v -> if (v == null) flowOf(null) else repo.latestOdometer(v.id) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val odometerLogs: StateFlow<List<OdometerLog>> = repo.activeVehicle.flatMapLatest { v -> if (v == null) flowOf(emptyList()) else repo.odometer(v.id) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val schedules: StateFlow<List<MaintenanceSchedule>> = repo.activeVehicle.flatMapLatest { v -> if (v == null) flowOf(emptyList()) else repo.schedules(v.id) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val documents: StateFlow<List<LegalDocument>> = repo.activeVehicle.flatMapLatest { v -> if (v == null) flowOf(emptyList()) else repo.documents(v.id) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _themeMode = MutableStateFlow(prefs.getString("theme", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode

    fun setTheme(mode: String) { prefs.edit().putString("theme", mode).apply(); _themeMode.value = mode }

    fun addVehicle(name: String, type: String) = viewModelScope.launch { if (name.isNotBlank()) repo.addVehicle(Vehicle(name = name.trim(), type = type)) }
    fun updateVehicle(v: Vehicle) = viewModelScope.launch { repo.updateVehicle(v) }
    fun setActiveVehicle(id: String) = viewModelScope.launch { repo.setActive(id) }
    fun deleteVehicle(id: String) = viewModelScope.launch { repo.deleteVehicle(id) }

    fun addOdometer(km: Int) = viewModelScope.launch { activeVehicle.value?.let { if (km > 0) repo.addOdometer(OdometerLog(vehicleId = it.id, km = km)) } }
    fun updateOdometer(log: OdometerLog) = viewModelScope.launch { repo.updateOdometer(log) }
    fun deleteOdometer(id: String) = viewModelScope.launch { repo.deleteOdometer(id) }

    fun addSchedule(name: String, intervalKm: Int?, intervalMonths: Int?) = viewModelScope.launch {
        activeVehicle.value?.let { if (name.isNotBlank()) repo.addSchedule(MaintenanceSchedule(vehicleId = it.id, name = name.trim(), intervalKm = intervalKm, intervalMonths = intervalMonths, lastServiceKm = currentKm.value, lastServiceDate = System.currentTimeMillis())) }
    }
    fun addSchedulesFromCatalog(items: List<CatalogItem>) = viewModelScope.launch {
        activeVehicle.value?.let { v -> items.forEach { item -> repo.addSchedule(MaintenanceSchedule(vehicleId = v.id, name = item.name, intervalKm = item.intervalKm, intervalMonths = item.intervalMonths, lastServiceKm = currentKm.value, lastServiceDate = System.currentTimeMillis())) } }
    }
    fun updateSchedule(s: MaintenanceSchedule) = viewModelScope.launch { repo.updateSchedule(s) }
    fun deleteSchedule(id: String) = viewModelScope.launch { repo.deleteSchedule(id) }
    fun recordService(schedule: MaintenanceSchedule, km: Int, dateMillis: Long, cost: Long, notes: String?) = viewModelScope.launch { if (km > 0) repo.recordService(schedule, km, dateMillis, cost, notes) }

    fun addDocument(type: String, expiryMillis: Long, number: String?) = viewModelScope.launch { activeVehicle.value?.let { repo.addDocument(LegalDocument(vehicleId = it.id, type = type, number = number, expiryDate = expiryMillis)) } }
    fun updateDocument(d: LegalDocument) = viewModelScope.launch { repo.updateDocument(d) }
    fun renewDocument(doc: LegalDocument, newExpiry: Long) = viewModelScope.launch { repo.renewDocument(doc, newExpiry) }
    fun deleteDocument(id: String) = viewModelScope.launch { repo.deleteDocument(id) }

    fun testNotification() = Notif.show(getApplication(), Notif.CH_SERVICE, "Uji Notifikasi", "Notifikasi OdoCare berfungsi.")
}
