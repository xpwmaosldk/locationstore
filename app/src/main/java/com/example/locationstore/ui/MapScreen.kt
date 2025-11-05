package com.example.locationstore.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.google.android.gms.maps.CameraUpdateFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    navController: NavController // ★★★ NavController를 파라미터로 받음
) {
    val locations by viewModel.locations.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()

    val seoul = LatLng(37.5665, 126.9780)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(seoul, 10f)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    // 코루틴 스코프 생성 (카메라 애니메이션에 사용)
    val coroutineScope = rememberCoroutineScope()

    // ViewModel의 일회성 이벤트를 수신하는 LaunchedEffect
    LaunchedEffect(Unit) {
        // Snackbar 메시지 처리
        viewModel.workStatus.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }


    LaunchedEffect(Unit) {
        viewModel.newLocation.collectLatest { location ->
            coroutineScope.launch {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude),
                        15f // 줌 레벨 (15 정도가 적당)
                    ),
                    durationMs = 1000 // 애니메이션 시간 (1초)
                )
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("위치 마커 앱") },
                actions = {
                    // ★★★ 목록 화면으로 이동하는 아이콘 버튼
                    IconButton(onClick = { navController.navigate("location_list") }) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "저장 목록 보기"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                locations.forEach { location ->
                    Marker(
                        state = MarkerState(position = LatLng(location.latitude, location.longitude)),
                        title = "저장된 위치",
                        snippet = "시간: ${formatTimestamp(location.timestamp)}" // ★★★ 여기서도 시간 포맷 함수 재사용
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (hasPermission) {
                            viewModel.fetchCurrentLocation()
                        } else {
                            onRequestPermission()
                        }
                    },
                ) {
                    val buttonText = if (hasPermission) "현 위치 조회 및 저장" else "권한 요청하기"
                    Text(text = buttonText)
                }
            }
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }
    }
}


private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}