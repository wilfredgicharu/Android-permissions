package com.example.catagentdeployer

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.icu.text.CaseMap
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.security.identity.AccessControlProfileId
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.example.catagentdeployer.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.*
import java.util.jar.Manifest


private const val PERMISSION_CODE_REQUEST_LOCATION= 1
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var marker: Marker? = null
    private lateinit var binding: ActivityMapsBinding

    private val fusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    private fun updateMapLocation(location: LatLng){
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 7f))
    }
    private fun addMarkerAtLocation(location: LatLng,title: String ,
    markericon: BitmapDescriptor?= null)= mMap.addMarker(
        MarkerOptions()
            .title(title)
            .position(location)
            .apply {
                markericon?.let {
                    icon(markericon)
                }
            }
    )
    private fun getBitmapDescriptorFromVector(@DrawableRes vectorDrawableResourceId: Int): BitmapDescriptor?{
        val bitmap = ContextCompat.getDrawable(this, vectorDrawableResourceId)?.let {
            vectorDrawable->
            vectorDrawable.setBounds(0,0,vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)


            val drawableWidthTint= DrawableCompat.wrap(vectorDrawable)
            DrawableCompat.setTint(drawableWidthTint, Color.RED)

            val bitmap= Bitmap.createBitmap(
                vectorDrawable.intrinsicWidth,
                vectorDrawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas= Canvas(bitmap)
            drawableWidthTint.draw(canvas)
            bitmap

         }
        return BitmapDescriptorFactory.fromBitmap(bitmap).also {
            bitmap?.recycle()
        }
    }
    private fun addOrMoveSelectedPositionMarker(latLng: LatLng){
        if (marker==null){
            marker= addMarkerAtLocation(
                latLng,"Deploy here",
                getBitmapDescriptorFromVector(R.drawable.itarget_icon)
            )
        }
        else{
            marker?.apply {
                position= latLng
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap.apply {
            setOnMapClickListener {
                latLng->
                addOrMoveSelectedPositionMarker(latLng)
            }
        }

        if (hasLocationPermission()){
            getLastLocation()
        } else{
            requestPermissionWithRationaleIfNeeded()
        }
    }


    private fun requestLocationPermission(){
        ActivityCompat.requestPermissions(this,
        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_CODE_REQUEST_LOCATION
            )

    }


    //the function will call when the has granted you permission
    private fun getLastLocation(){
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationProviderClient.lastLocation
           .addOnSuccessListener { location: Location? ->
               location?.let {
                   val userLocation= LatLng(location.latitude, location.longitude)
                   updateMapLocation(userLocation)
                   addMarkerAtLocation(userLocation,"You")
               }
           }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERMISSION_CODE_REQUEST_LOCATION->if (
                grantResults[0]==PackageManager.PERMISSION_GRANTED
            ){
                getLastLocation()
            }
            else{
                requestPermissionWithRationaleIfNeeded()
            }
        }
    }
    private fun showPermissionRational(positiveAction: ()-> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Location Permission")
            .setMessage("this app will not work without knowing your current location")
            .setPositiveButton("ok"){
                _,_,->positiveAction()
            }
            .create()
            .show()
    }
    private fun requestPermissionWithRationaleIfNeeded()= if(
        ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
    ){
        showPermissionRational {
            requestLocationPermission()
        }
    } else{
        requestLocationPermission()
    }

    //checking whether the app already has the permission
    private fun hasLocationPermission()= ContextCompat.checkSelfPermission(
        this, android.Manifest.permission.ACCESS_FINE_LOCATION
    )== PackageManager.PERMISSION_GRANTED


}