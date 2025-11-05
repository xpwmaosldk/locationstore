package com.example.locationstore.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.locationstore.data.local.AppDatabase
import com.example.locationstore.data.local.LocationEntity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LocationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {


    private val TAG = "LocationWorker"

    private val locationClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationDao = AppDatabase.getDatabase(context).locationDao()

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork: Worker started.")

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "doWork: Location permission NOT GRANTED inside worker.")
            return Result.failure()
        }
        Log.d(TAG, "doWork: Location permission is granted.")

        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "doWork: Trying to get current location...")
                val location = getCurrentLocation()

                if (location != null) {
                    Log.d(TAG, "doWork: Location found: Lat=${location.latitude}, Lng=${location.longitude}")
                    val locationEntity = LocationEntity(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                    locationDao.insertLocation(locationEntity)
                    Log.d(TAG, "doWork: Location successfully saved to database.")
                    Result.success()
                } else {
                    Log.e(TAG, "doWork: Failed to get location, it was null.")
                    Result.failure()
                }
            } catch (e: Exception) {
                Log.e(TAG, "doWork: Exception occurred.", e)
                Result.failure()
            }
        }
    }

    private suspend fun getCurrentLocation(): android.location.Location? {
        return suspendCoroutine { continuation ->
            try {
                locationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).addOnSuccessListener { location ->
                    if (location == null) {
                        Log.w(TAG, "getCurrentLocation: FusedLocationProvider returned null.")
                    }
                    continuation.resume(location)
                }.addOnFailureListener { e ->
                    Log.e(TAG, "getCurrentLocation: Location fetch failed.", e)
                    continuation.resume(null)
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "getCurrentLocation: SecurityException.", e)
                continuation.resume(null)
            }
        }
    }
}