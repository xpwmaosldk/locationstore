package com.example.locationstore.ui


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.locationstore.data.local.LocationEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationListScreen(
    viewModel: MapViewModel,
    navController: NavController
) {
    // ViewModel로부터 위치 목록을 State로 구독
    val locations by viewModel.locations.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("저장된 위치 목록") },
                navigationIcon = {
                    // 뒤로가기 버튼
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (locations.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("저장된 위치가 없습니다.")
            }
        } else {
            // 스크롤 가능한 효율적인 목록
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(locations) { location ->
                    LocationItem(location = location)
                }
            }
        }
    }
}

@Composable
fun LocationItem(location: LocationEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "저장 시간: ${formatTimestamp(location.timestamp)}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Row(modifier = Modifier.padding(top = 8.dp)) {
                Text(text = "위도: ", fontWeight = FontWeight.SemiBold)
                Text(text = "%.6f".format(location.latitude))
            }
            Row {
                Text(text = "경도: ", fontWeight = FontWeight.SemiBold)
                Text(text = "%.6f".format(location.longitude))
            }
        }
    }
}

// Long 타입의 timestamp를 사람이 읽기 쉬운 문자열로 변환
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}