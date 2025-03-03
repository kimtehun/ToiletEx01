package com.example.toiletex01

import com.naver.maps.geometry.LatLng


data class SimpleToiletEntity(
    val num : Int ,
    val toiletName : String ,
    val latitude : String ,
    val longitude : String ,
    val pw : String
)

// 클러스터 정보를 담기 위한 데이터 클래스
data class Cluster(val center: LatLng, val items: List<SimpleToiletEntity>)