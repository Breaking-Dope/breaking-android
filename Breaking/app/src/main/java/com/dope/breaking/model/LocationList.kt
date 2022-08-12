package com.dope.breaking.model

import java.io.Serializable

/* 카카오 맵에서 키워드 검색 시 결과로 주는 리사이클러 뷰의 item dto */
class LocationList (
    val name: String,
    val road: String,
    val address: String,
    val x: Double, // longitude
    val y: Double  // latitude
) : Serializable