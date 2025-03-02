package com.example.toiletex01

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.toiletex01.databinding.ActivityMainBinding
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.FusedLocationSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() , OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var naverMap: NaverMap
    private lateinit var dbHelper: LocationDatabaseHelper

    private lateinit var locationSource: FusedLocationSource

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    // 현재 표시 중인 마커 리스트
    private val markers = mutableListOf<Marker>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // FusedLocationSource를 생성하면 SDK가 내부적으로 위치를 비동기 요청합니다.
        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)

        // lifecycleScope를 사용해 메인 스레드에서 코루틴 실행
        lifecycleScope.launch {
            // IO 스레드에서 DB 복사 작업을 수행하는 suspend 함수 호출
            withContext(Dispatchers.IO) {

                copyDatabaseFromAssets(this@MainActivity)

            }
            // 복사가 완료된 후, 안전하게 데이터베이스 헬퍼 초기화
            dbHelper = LocationDatabaseHelper(this@MainActivity)


            // 이후 dbHelper를 이용한 데이터베이스 작업 실행
            val mapFragment = supportFragmentManager.findFragmentById(R.id.map12) as MapFragment?
                ?: MapFragment.newInstance().also {
                    supportFragmentManager.beginTransaction().add(R.id.map12, it).commit()
                }
            mapFragment.getMapAsync(this@MainActivity)

        }
    }

    override fun onMapReady(map: NaverMap) {
        this.naverMap = map

        this.naverMap.moveCamera(CameraUpdate.scrollTo(LatLng(37.5665, 126.9780)))  // 서울 기본 위치


        // FusedLocationSource 할당 (내 위치 정보가 비동기적으로 업데이트됨)
        naverMap.locationSource = locationSource

        // 내 위치 버튼 UI 활성화 (SDK가 위치 요청 후 자동 업데이트)
        naverMap.uiSettings.isLocationButtonEnabled = true

        // 현재 위치 오버레이를 보이게 설정 (SDK가 내부적으로 현재 위치를 받아 업데이트)
        naverMap.locationOverlay.isVisible = true

        // 추가로, 위치 변경 시 다른 UI 업데이트를 원한다면 아래와 같이 FusedLocationProviderClient를 활용하거나
        // NaverMap의 내장 업데이트에 콜백을 연결할 수 있습니다.

        lifecycleScope.launch {
            // 지도 이동이 끝났을 때 (idle 상태)마다 마커 업데이트
            naverMap.addOnCameraIdleListener {
                updateMarkersFromDb()
            }

            // 앱 최초 실행 시에도 한 번 마커 표시
            updateMarkersFromDb()
        }

    }

    private fun updateMarkersFromDb() {
        // 비동기적으로 DB 조회 후 마커 갱신
        lifecycleScope.launch {
            val locations = withContext(Dispatchers.IO) {
                dbHelper.getAllLocations()
            }
            refreshMarkers(locations)
        }
    }

    private fun refreshMarkers(locations: List<SimpleToiletEntity>) {
        // 기존 마커 전부 제거
        markers.forEach { it.map = null }
        markers.clear()

        // 새 데이터로 마커 추가
        locations.forEach { location ->
            val marker = Marker().apply {
                position = LatLng(location.latitude.toDouble(), location.longitude.toDouble())
                captionText = location.toiletName
                map = naverMap
            }
            markers.add(marker)
        }
    }


    // suspend 함수로 정의하여 IO 스레드에서 복사 작업 실행
    suspend fun copyDatabaseFromAssets(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                // assets에서 데이터베이스 파일 읽기
                val assetManager = context.assets
                val inputStream = assetManager.open("toiletdb.db")

                // 복사할 경로 지정
                val outFileName = context.getDatabasePath("toiletdb.db").path
                val outputFile = File(outFileName)
                outputFile.parentFile?.mkdirs()

                // 파일 복사 작업
                inputStream.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        val buffer = ByteArray(1024)
                        var length: Int
                        while (input.read(buffer).also { length = it } > 0) {
                            output.write(buffer, 0, length)
                        }
                        output.flush()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 예외 발생 시 적절한 처리가 필요합니다.
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            // 권한이 부여되지 않았다면 현재 위치 오버레이 숨기기
            if (!locationSource.isActivated) {
                naverMap.locationOverlay.isVisible = false
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
