package com.example.locationstore

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.locationstore.ui.LocationListScreen
import com.example.locationstore.ui.MapScreen
import com.example.locationstore.ui.MapViewModel
import com.example.locationstore.ui.MapViewModelFactory
import com.example.locationstore.ui.theme.LocationStoreTheme

class MainActivity : ComponentActivity() {

    private val mapViewModel: MapViewModel by viewModels {
        MapViewModelFactory(application)
    }

    // 요청할 권한 목록을 명확하게 정의
    private val permissionsToRequest = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocationStoreTheme {
                val navController = rememberNavController()


                // 권한 상태를 관리하는 State. 초기값은 현재 권한 상태로 설정.
                var hasPermissions by remember {
                    mutableStateOf(hasRequiredPermissions())
                }

                // 권한 요청 런처
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    // 런처의 콜백이 실행된 후, 권한 상태를 다시 확인하여 State를 갱신
                    Log.d("PermissionCheck", "Launcher result received. Re-checking permissions.")
                    hasPermissions = hasRequiredPermissions()
                }

                // 앱이 처음 시작될 때 권한이 없다면 요청
                LaunchedEffect(Unit) {
                    if (!hasRequiredPermissions()) {
                        Log.d("PermissionCheck", "Initial permission check failed. Launching request.")
                        permissionLauncher.launch(permissionsToRequest)
                    }
                }

                NavHost(navController = navController, startDestination = "map_screen") {
                    // 지도 화면 경로 설정
                    composable("map_screen") {
                        MapScreen(
                            viewModel = mapViewModel,
                            hasPermission = hasPermissions,
                            onRequestPermission = {
                                permissionLauncher.launch(permissionsToRequest)
                            },
                            navController = navController // navController 전달
                        )
                    }

                    // 위치 목록 화면 경로 설정
                    composable("location_list") {
                        LocationListScreen(
                            viewModel = mapViewModel,
                            navController = navController // navController 전달
                        )
                    }
                }
            }
        }
    }

    // 현재 포그라운드 위치 권한이 있는지 확인하는 함수
    private fun hasRequiredPermissions(): Boolean {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        Log.d("PermissionCheck", "hasFine: $hasFineLocation, hasCoarse: $hasCoarseLocation")
        return hasFineLocation || hasCoarseLocation
    }
}