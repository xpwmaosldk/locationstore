package com.example.locationstore.data


import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.locationstore.data.local.LocationDao
import com.example.locationstore.data.local.LocationEntity
import com.example.locationstore.worker.LocationWorker
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class LocationRepository(
    private val locationDao: LocationDao,
    private val context: Context
) {
    val allLocations: Flow<List<com.example.locationstore.data.local.LocationEntity>> = locationDao.getAllLocations()

    fun requestLocationUpdate(): UUID {
        val workRequest = OneTimeWorkRequestBuilder<LocationWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
        return workRequest.id
    }
    suspend fun getLastLocation(): LocationEntity? {
        return locationDao.getLastLocation()
    }
}