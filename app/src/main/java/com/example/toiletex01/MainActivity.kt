package com.example.toiletex01

import androidx.appcompat.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.toiletex01.databinding.ActivityMainBinding
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.InfoWindow
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.FusedLocationSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() , OnMapReadyCallback ,
    AddToiletDialogFragment.OnAddToiletListener{

    private lateinit var binding: ActivityMainBinding
    private lateinit var naverMap: NaverMap
    private lateinit var dbHelper: LocationDatabaseHelper

    private lateinit var locationSource: FusedLocationSource

    // 고유 id(num)를 키로 하여 현재 지도에 추가된 마커들을 관리하는 Map
    private val markerMap = mutableMapOf<Int, Marker>()

    // 마커 리스트
    private val markers = mutableListOf<Marker>()

    // 줌 레벨 기준 설정 (예: 10 미만이면 마커 숨김)
    private val MIN_ZOOM_LEVEL = 10f

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000

    }

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

            // addToilet 버튼 이벤트 리스너 등록
            binding.addToilet?.setOnClickListener {
                val dialogFragment = AddToiletDialogFragment()
                dialogFragment.show(supportFragmentManager, "AddToiletDialog")
//            addToilet()
            }

        }




    }

    override fun onMapReady(map: NaverMap) {
        this.naverMap = map

        // 기본 위치 설정 (서울)
        this.naverMap.moveCamera(CameraUpdate.scrollTo(LatLng(37.5665, 126.9780)))

        // FusedLocationSource 할당 (내 위치 정보가 비동기적으로 업데이트됨)
        naverMap.locationSource = locationSource

        // 내 위치 버튼 UI 활성화 (SDK가 위치 요청 후 자동 업데이트)
        naverMap.uiSettings.isLocationButtonEnabled = true

        // 현재 위치 오버레이를 보이게 설정 (SDK가 내부적으로 현재 위치를 받아 업데이트)
        naverMap.locationOverlay.isVisible = true

        // 지도 이동이 끝났을 때마다 마커 업데이트 (줌 레벨 고려)
        naverMap.addOnCameraIdleListener {
            updateMarkersFromDb()
        }

        // 앱 최초 실행 시에도 마커 업데이트
        lifecycleScope.launch {
            updateMarkersFromDb()
        }
    }
    //클러스터 기능이 필요하면 이 부분을 다시 살려내면 된다
    //하지만 현재 이 프로젝트에는 화장실 정보가 한정적으로 있기에 넣지 않는것이 맞아보여서 주석처리해둠

    private fun updateMarkersFromDb() {
        val currentZoom = naverMap.cameraPosition.zoom

        // 줌 레벨이 낮으면 기존 마커 숨기고 return
        if (currentZoom < MIN_ZOOM_LEVEL) {
            markers.forEach { it.map = null }
            markers.clear()
            return
        }

        lifecycleScope.launch {
            // 1. 기존 마커 삭제
            markers.forEach { it.map = null }
            markers.clear()

            // 2. DB에서 마커 데이터를 가져옴 (예제에서는 로컬 DB에서 가져온다고 가정)
            val dbLocations = withContext(Dispatchers.IO) { getToiletLocationsFromDb() }



            dbLocations.forEach { location ->
                // latitude, longitude가 String 타입인 경우 안전하게 변환 (또는 이미 Double 타입이면 바로 사용)
                val lat = location.latitude.toDoubleOrNull() ?: 0.0
                val lng = location.longitude.toDoubleOrNull() ?: 0.0

                // 마커 생성 및 속성 설정, tag에 고유 id 저장
                val marker = Marker().apply {
                    position = LatLng(lat, lng)
                    captionText = location.toiletName ?: "정보 없음"
                    map = naverMap
                    tag = location.num  // DB의 고유 id를 저장 (예: Int 타입)
                }

//                Log.d("MARKER_TAG", "마커 생성 - tag: ${location.num}")


                /*
                // 마커 클릭 시, tag를 이용해 DB에서 상세 정보 조회 후 UI에 표시
                marker.setOnClickListener { overlay ->
                    val num = marker.tag as? Int
                    if (num != null) {
                        lifecycleScope.launch {
                            val detail = withContext(Dispatchers.IO) {
                                dbHelper.getToiletById(num)
                            }
                            showToiletInfo(detail)
                        }
                    }
                    true // 클릭 이벤트 처리가 완료되었음을 반환
                }
*/
                markers.add(marker)
            }

            // 지도에 보이는 영역 내의 데이터만 필터링
            val visibleLocations = dbLocations.filter { location ->
                val lat = location.latitude.toDoubleOrNull() ?: 0.0
                val lng = location.longitude.toDoubleOrNull() ?: 0.0
                naverMap.contentBounds.contains(LatLng(lat, lng))
            }

            // 클러스터링 적용 (여기서는 간단한 그리드 기반 클러스터링 사용)
            val clusters = clusterLocations(visibleLocations)

            // 각 클러스터마다 마커 생성
            clusters.forEach { cluster ->
                val marker = Marker().apply {
                    position = cluster.center
                    // 클러스터 내 항목이 2개 이상이면 클러스터 개수 표시, 아니면 빈 문자열
                    captionText = if (cluster.items.size > 1) "${cluster.items.size}" else ""
                    map = naverMap
                }

                //클러스터에 이벤트 리스너 부분을 단 아래부분이
                //updateMarkersFromDb안에서 비동기로 처리하는 코루틴 스코프 안에있는
                //market.setOnClickListener와 충돌하여 엇갈리는 결과가 나오는 현상이 있어서
                //아래 부분을 주석처리 해줬고 실행하여 문제없이 해결 되었다.
//                marker.setOnClickListener {
//                    // 예를 들어, 클러스터 내부의 첫 번째 아이템 정보를 표시하거나
//                    // 상세 화면으로 이동하는 로직 추가 가능
//                    val firstItem = cluster.items.firstOrNull()
//                    if (firstItem != null) {
//                        lifecycleScope.launch {
//                            val detail = withContext(Dispatchers.IO) {
//                                dbHelper.getToiletById(firstItem.num)
//                            }
//                            showToiletInfo(detail)
//                        }
//                    }
//                    true
//                }
                markers.add(marker)
            }
        }
    }

    //부분 업데이트로 보여주고 싶다면 아래 코드를 주석처리 해제 하면 된다
/*
    private fun updateMarkersFromDb() {

        val currentZoom = naverMap.cameraPosition.zoom
        Log.d(TAG, "현재 줌 레벨: $currentZoom")
        if (currentZoom < MIN_ZOOM_LEVEL) {
            Log.d(TAG, "줌 레벨이 낮아서 모든 마커 제거")
            markerMap.values.forEach { it.map = null }
            markerMap.clear()
            return
        }

        lifecycleScope.launch {
            // DB에서 최신 데이터 가져오기 (예: List<SimpleToiletEntity>)
            val newData = withContext(Dispatchers.IO) { getToiletLocationsFromDb() }
            Log.d(TAG, "DB에서 가져온 데이터 개수: ${newData.size}")

            // 새로운 데이터의 고유 id 집합
            val newIds = newData.map { it.num }.toSet()

            // 1. markerMap에 있으나 새로운 데이터에 없는 마커 제거
            val toRemove = markerMap.keys.filter { it !in newIds }
            toRemove.forEach { id ->
                markerMap[id]?.let { marker ->
                    marker.map = null
                    Log.d(TAG, "마커 제거: id=$id")
                }
                markerMap.remove(id)
            }

            // 2. 새로운 데이터에 대해 업데이트 또는 추가
            newData.forEach { location ->
                val lat = location.latitude?.toDoubleOrNull() ?: 0.0
                val lng = location.longitude?.toDoubleOrNull() ?: 0.0
                val position = LatLng(lat, lng)
                if (markerMap.containsKey(location.num)) {
                    // 이미 존재하는 마커 업데이트
                    val marker = markerMap[location.num]
                    if (marker != null) {
                        if (marker.position != position) {
                            marker.position = position
                            Log.d(TAG, "마커 위치 업데이트: id=${location.num}")
                        }
                        val newCaption = location.toiletName ?: "정보 없음"
                        if (marker.captionText != newCaption) {
                            marker.captionText = newCaption
                            Log.d(TAG, "마커 캡션 업데이트: id=${location.num}")
                        }
                    }
                } else {
                    // 새 마커 추가
                    val marker = Marker().apply {
                        this.position = position
                        captionText = location.toiletName ?: "정보 없음"
                        map = naverMap
                        tag = location.num  // 고유 id 저장
                    }
                    // 마커 클릭 이벤트 등록: 클릭 시 DB에서 상세 정보를 조회하여 UI에 표시
                    marker.setOnClickListener { overlay ->
                        val id = marker.tag as? Int
                        Log.d(TAG, "마커 클릭됨, id=$id")
                        if (id != null) {
                            lifecycleScope.launch {
                                val detail =
                                    withContext(Dispatchers.IO) { dbHelper.getToiletById(id) }
                                Log.d(TAG, "DB 조회 결과: $detail")
                                showToiletInfo(detail)
                            }
                        }
                        true
                    }
                    markerMap[location.num] = marker
                    Log.d(TAG, "새 마커 추가: id=${location.num}")
                }
            }
        }

    }
*/
    private fun getToiletLocationsFromDb(): List<SimpleToiletEntity> {
        return dbHelper.getAllLocations() // SQLite 또는 Room에서 데이터를 가져온다고 가정
    }

    // suspend 함수로 정의하여 IO 스레드에서 복사 작업 실행
    suspend fun copyDatabaseFromAssets(context: Context) {

        withContext(Dispatchers.IO) {
            // 대상 DB 파일 경로
            val dbFile = context.getDatabasePath("toiletdb.db")
            // 파일이 이미 존재하면 복사하지 않음
            if (dbFile.exists()) {
                Log.d("DB_COPY", "Database file already exists: ${dbFile.path}")
                return@withContext
            }
            // 부모 디렉터리가 없으면 생성
            dbFile.parentFile?.mkdirs()

            try {
                val assetManager = context.assets
                assetManager.open("toiletdb.db").use { input ->
                    FileOutputStream(dbFile).use { output ->
                        val buffer = ByteArray(1024)
                        var length: Int
                        while (input.read(buffer).also { length = it } > 0) {
                            output.write(buffer, 0, length)
                        }
                        output.flush()
                    }
                }
                Log.d("DB_COPY", "Database file copied to: ${dbFile.path}")
            } catch (e: Exception) {
                Log.e("DB_COPY", "Error copying database", e)
            }
        }

//        withContext(Dispatchers.IO) {
//            try {
//                // assets에서 데이터베이스 파일 읽기
//                val assetManager = context.assets
//                val inputStream = assetManager.open("toiletdb.db")
//
//                // 복사할 경로 지정
//                val outFileName = context.getDatabasePath("toiletdb.db").path
//                val outputFile = File(outFileName)
//                outputFile.parentFile?.mkdirs()
//
//                // 파일 복사 작업
//                inputStream.use { input ->
//                    FileOutputStream(outputFile).use { output ->
//                        val buffer = ByteArray(1024)
//                        var length: Int
//                        while (input.read(buffer).also { length = it } > 0) {
//                            output.write(buffer, 0, length)
//                        }
//                        output.flush()
//                    }
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                // 예외 발생 시 적절한 처리가 필요합니다.
//            }
//        }
    }
//
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

    private fun clusterLocations(locations: List<SimpleToiletEntity>): List<Cluster> {
        val clusters = mutableListOf<Cluster>()
        // 그리드 크기는 줌 레벨에 따라 조정할 수 있으며, 여기서는 예시로 0.01도를 사용합니다.
        val gridSize = 0.01
        val grid = mutableMapOf<Pair<Int, Int>, MutableList<SimpleToiletEntity>>()

        locations.forEach { loc ->
            // latitude와 longitude가 null일 경우 기본값 0.0 사용 (실제 서비스에서는 필터링하는 것이 좋습니다)
            val lat = loc.latitude.toDoubleOrNull() ?: 0.0
            val lng = loc.longitude.toDoubleOrNull() ?: 0.0
            // 그리드 셀 좌표 계산 (간단한 방식)
            val cellX = (lat / gridSize).toInt()
            val cellY = (lng / gridSize).toInt()
            val key = Pair(cellX, cellY)
            grid.getOrPut(key) { mutableListOf() }.add(loc)
        }

        // 각 셀마다 클러스터 생성 (평균 좌표를 클러스터 중심으로 사용)
        grid.forEach { (_, items) ->
            val avgLat = items.map { it.latitude.toDoubleOrNull() ?: 0.0 }.average()
            val avgLng = items.map { it.longitude.toDoubleOrNull() ?: 0.0 }.average()
            clusters.add(Cluster(LatLng(avgLat, avgLng), items))
        }
        return clusters
    }

    private fun showToiletInfo(toilet: SimpleToiletEntity?) {
        if (toilet == null) {
            AlertDialog.Builder(this)
                .setTitle("오류")
                .setMessage("해당 화장실 정보를 찾을 수 없습니다.")
                .setPositiveButton("확인", null)
                .show()
            return
        }
        val infoText : String
        if(toilet.pw.toInt() == 0){
            infoText = """
        화장실 이름: ${toilet.toiletName}
        비밀번호: 없음
        기타 정보: ...
    """.trimIndent()
        } else {
            infoText = """
        화장실 이름: ${toilet.toiletName}
        비밀번호: ${toilet.pw ?: "정보 없음"}
        기타 정보: ...
    """.trimIndent()
        }

        AlertDialog.Builder(this)
            .setTitle("화장실 상세 정보")
            .setMessage(infoText)
            .setPositiveButton("확인", null)
            .show()
    }

    // addToilet 메소드: DB에 새로운 레코드를 삽입
    private fun addToilet() {
        lifecycleScope.launch {
            // 백그라운드에서 새 레코드 추가
            withContext(Dispatchers.IO) {
                dbHelper.addToiletById()  // 새 레코드 삽입
            }
            updateMarkersFromDb()
            // UI 스레드에서 결과 Toast 표시
            runOnUiThread {
                Toast.makeText(this@MainActivity, "새 화장실 정보가 추가되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // DialogFragment에서 "추가하기"를 누르면 호출되는 콜백 메소드
    override fun onAddToiletSubmit(toiletName: String, password: String) {
        // 현재 위치(내장 위치 오버레이)에서 좌표를 얻어옵니다.
        val currentLatLng: LatLng? = naverMap.locationOverlay.position
        if (currentLatLng != null) {
            val latitude = currentLatLng.latitude
            val longitude = currentLatLng.longitude
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    dbHelper.insertToiletRecord(toiletName, latitude, longitude, password)
                }
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "화장실 정보가 추가되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "현재 위치 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

}
