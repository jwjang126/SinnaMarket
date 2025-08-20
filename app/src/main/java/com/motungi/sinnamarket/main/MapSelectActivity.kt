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
            val (fullAddress, district, dong) = getAddressFromLatLng(
                centerLatLng.latitude,
                centerLatLng.longitude
            )

            val resultIntent = Intent()
            resultIntent.putExtra("selectedLat", centerLatLng.latitude)
            resultIntent.putExtra("selectedLng", centerLatLng.longitude)
            resultIntent.putExtra("selectedAddress", fullAddress)
            resultIntent.putExtra("district", district)
            resultIntent.putExtra("dong", dong)

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

    private fun getAddressFromLatLng(lat: Double, lng: Double): Triple<String, String, String> {
        val geocoder = Geocoder(this, Locale.KOREA) // 한국어 Locale 적용
        return try {
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val fullAddress = address.getAddressLine(0) ?: "주소 미상"

                val parts = fullAddress.split(" ")
                var district = ""
                var dong = ""

                for (i in parts.indices) {
                    if (parts[i].endsWith("구") || parts[i].endsWith("군")) {
                        district = parts[i]
                    }
                    if (parts[i].endsWith("동") || parts[i].endsWith("읍") || parts[i].endsWith("면")) {
                        dong = parts[i]
                    }

                    val dongOrRo = parts.find {
                        it.endsWith("동") || it.endsWith("로") || it.endsWith("가") || it.endsWith("길") || it.endsWith("읍") || it.endsWith("면")
                    }

                    if (dongOrRo != null) {
                        // 숫자가 나올 때까지의 문자열만 추출
                        dong = dongOrRo.takeWhile { !it.isDigit() }
                    }
                }

                if (district.isEmpty()) {
                    for (i in parts.indices) {
                        if (parts[i].endsWith("시")) {
                            district = parts[i]
                            break
                        }
                    }
                }

                Triple(fullAddress, district, dong)
            } else {
                Triple("주소 미상", "", "")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Triple("주소 변환 실패", "", "")
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