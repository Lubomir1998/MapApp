package com.example.mapapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mapapp.databinding.ActivityMainBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places


class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mLocationPermissionsGranted = false

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
        const val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        const val COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        getLocationPermission()


    }

    private fun initMap(){
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap ?: return

        if(mLocationPermissionsGranted) {
            getDeviceLocation()
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = false
            init()
        }
    }

    private fun init(){
        // Initialize Places
        if (!Places.isInitialized()) {
            Places.initialize(this, R.string.google_maps_API_key.toString());
        }


        binding.searchEditText.setOnEditorActionListener { _, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                || actionId == EditorInfo.IME_ACTION_DONE
                || keyEvent.action == KeyEvent.ACTION_DOWN
                || keyEvent.action == KeyEvent.KEYCODE_ENTER) {

                //execute our method for searching
                geoLocate()
            }
            false
        }
        binding.gpsImage.setOnClickListener {
            getDeviceLocation()
        }
        hideKeyBoardAfterSearchOrMoveCamera()
    }


    private fun getLocationPermission() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (ContextCompat.checkSelfPermission(
                    this.applicationContext,
                    COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mLocationPermissionsGranted = true
                initMap()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                permissions,
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.contains(PackageManager.PERMISSION_GRANTED)) {
                mLocationPermissionsGranted = true
                initMap()
            }
        }
    }



    private fun getDeviceLocation(){
        mFusedLocationClient = FusedLocationProviderClient(this)


        if(mLocationPermissionsGranted){
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val location = mFusedLocationClient.lastLocation
            location.addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    val currentLocation = task.result as Location

                    val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                    val zoomLevel = 15f

                    moveCamera(latLng, zoomLevel, "My Location")
                }
            }

        }


    }

    private fun geoLocate(){
        val searchText = binding.searchEditText.text.toString()
        val geoCoder = Geocoder(this)

        var list = mutableListOf<Address>()

        try {
            list = geoCoder.getFromLocationName(searchText, 1)
        }
        catch (e: Exception){ }

        if(list.isNotEmpty()){
            moveCamera(LatLng(list[0].latitude, list[0].longitude), 10f, list[0].getAddressLine(0))
        }
    }

    private fun moveCamera(latLng: LatLng, zoomLevel: Float, title: String){
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))

        if(title != "My Location") {
            val marker = MarkerOptions().position(latLng).title(title)
            mMap.addMarker(marker)
        }

        hideKeyBoardAfterSearchOrMoveCamera()
    }

    private fun hideKeyBoardAfterSearchOrMoveCamera(){
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    override fun onConnectionFailed(p0: ConnectionResult) {

    }

}