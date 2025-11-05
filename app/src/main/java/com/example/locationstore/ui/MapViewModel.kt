package com.example.locationstore.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.locationstore.data.LocationRepository
import com.example.locationstore.data.local.AppDatabase
import com.example.locationstore.data.local.LocationEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID


class MapViewModel(
    private val repository: LocationRepository,
    private val workManager: WorkManager
) : ViewModel() {

    val locations: StateFlow<List<LocationEntity>> = repository.allLocations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _workStatus = MutableSharedFlow<String>()
    val workStatus = _workStatus.asSharedFlow()

    // ★★★ 1. 로딩 상태를 관리하는 StateFlow 추가
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ★★★ 2. 새로 추가된 위치 정보를 전달할 SharedFlow 추가
    // StateFlow 대신 SharedFlow를 사용하여 일회성 이벤트를 전달
    private val _newLocation = MutableSharedFlow<LocationEntity>()
    val newLocation = _newLocation.asSharedFlow()


    fun fetchCurrentLocation() {
        // 로딩 시작
        _isLoading.value = true
        val workRequestId = repository.requestLocationUpdate()
        observeWork(workRequestId)
    }

    private fun observeWork(workRequestId: UUID) {
        // LiveData를 Flow로 변환하여 관찰
        val workInfoFlow = workManager.getWorkInfoByIdFlow(workRequestId)
        viewModelScope.launch {
            workInfoFlow.collect { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            _isLoading.value = false // 로딩 종료
                            _workStatus.emit("위치 정보가 저장되었습니다.")
                            // ★★★ 3. 성공 시 DB에서 마지막 위치를 가져와 UI에 전달
                            val lastLocation = repository.getLastLocation()
                            if (lastLocation != null) {
                                _newLocation.emit(lastLocation)
                            }
                        }
                        WorkInfo.State.FAILED -> {
                            _isLoading.value = false // 로딩 종료
                            _workStatus.emit("위치 정보 저장에 실패했습니다.")
                        }
                        WorkInfo.State.RUNNING -> {
                            // 이미 isLoading = true 이므로 별도 처리 필요 없음
                            Log.d("MapViewModel", "Work is running...")
                        }
                        else -> { /* Enqueued, Blocked, Cancelled */ }
                    }
                }
            }
        }
    }
}
// ViewModelFactory도 WorkManager를 전달하도록 수정
class MapViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val database = AppDatabase.getDatabase(application)
            val repository = LocationRepository(database.locationDao(), application)
            val workManager = WorkManager.getInstance(application) // WorkManager 인스턴스
            return MapViewModel(repository, workManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}