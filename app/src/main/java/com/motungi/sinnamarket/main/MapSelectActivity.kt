package com.motungi.sinnamarket.main

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.motungi.sinnamarket.R
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.FusedLocationSource
import java.util.Locale

class MapSelectActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var naverMap: NaverMap
    private lateinit var locationSource: FusedLocationSource
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_select)

        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)

        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map_fragment) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.map_fragment, it).commit()
            }
        mapFragment.getMapAsync(this)

        // 확인 버튼 클릭
        findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            val centerLatLng = naverMap.cameraPosition.target
            val address = getAddressFromLatLng(centerLatLng.latitude, centerLatLng.longitude)

            val resultIntent = Intent()
            resultIntent.putExtra("selectedLat", centerLatLng.latitude)
            resultIntent.putExtra("selectedLng", centerLatLng.longitude)
            resultIntent.putExtra("selectedAddress", address)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map
        naverMap.locationSource = locationSource
        naverMap.locationTrackingMode = LocationTrackingMode.Follow
        naverMap.uiSettings.isLocationButtonEnabled = true

        // 권한 확인
        if (!checkLocationPermission()) {
            requestLocationPermission()
        }
    }

    private fun getAddressFromLatLng(lat: Double, lng: Double): String {
        val geocoder = Geocoder(this, Locale.KOREA) // 한국어 Locale 강제 적용
        return try {
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].getAddressLine(0) ?: "주소 미상"
            } else {
                "주소 미상"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "주소 변환 실패"
        }
    }

    // 권한 확인
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 권한 요청
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    // 권한 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}