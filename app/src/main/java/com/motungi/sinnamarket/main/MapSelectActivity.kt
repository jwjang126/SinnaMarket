package com.motungi.sinnamarket.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.motungi.sinnamarket.R
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback

class MapSelectActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var naverMap: NaverMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_select)

        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.map_fragment, it).commit()
            }
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map

        // 지도 가운데로 이동 (테스트용)
        val cameraUpdate = CameraUpdate.scrollTo(LatLng(35.8714, 128.6014)) // 대구 좌표
        naverMap.moveCamera(cameraUpdate)

        // 지도 클릭하면 로그만 찍기
        naverMap.setOnMapClickListener { _, latLng ->
            Log.d("MapSelectActivity", "Clicked: ${latLng.latitude}, ${latLng.longitude}")
        }
    }
}