package com.skydroid.mapboundryapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.skydroid.mapboundryapp.ui.theme.MapBoundryAppTheme
import com.skydroid.mapboundryapp.ui.theme.green_button_color
import com.skydroid.mapboundryapp.ui.theme.yellow_light

class MainActivity : ComponentActivity() {

    private var locationCallback: LocationCallback? = null
    var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationRequired = false

    var c1 = 0

    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MapBoundryAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val context = LocalContext.current

                    var submitDialog by rememberSaveable { mutableStateOf(false) }
                    var openMySnackbar by remember { mutableStateOf(false)  }
                    var snackBarMessage by remember { mutableStateOf("") }
                    var currentLocation by remember {
                        mutableStateOf(LatLng(0.toDouble(), 0.toDouble()))
                    }

                    if (submitDialog) {
                        submitBoundry()
                    }

                    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                    locationCallback = object : LocationCallback() {
                        override fun onLocationResult(p0: LocationResult) {
                            for (lo in p0.locations) {
                                c1++
                                // Update UI with location data
                                currentLocation = LatLng(lo.latitude, lo.longitude)

                            }
                        }
                    }

                    val launcherMultiplePermissions = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissionsMap ->
                        val areGranted = permissionsMap.values.reduce { acc, next -> acc && next }
                        if (areGranted) {
                            locationRequired = true
                            startLocationUpdates()
                            Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val permissions = arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                        if (c1 != 0) {
                            Box {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(580.dp)
                                ) {
                                    MapScreen(currentLocation)
                                }

                            }
                        }


                        Column(modifier = Modifier.fillMaxWidth().wrapContentSize()) {

                            Row(
                                modifier = Modifier.fillMaxWidth().wrapContentSize(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(Modifier.weight(1f)) {

                                    Button(
                                        onClick = {
                                            if (permissions.all {
                                                    ContextCompat.checkSelfPermission(
                                                        context,
                                                        it
                                                    ) == PackageManager.PERMISSION_GRANTED
                                                }) {
                                                // Get the location
                                                startLocationUpdates()
                                            } else {
                                                launcherMultiplePermissions.launch(permissions)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start = 20.dp,
                                                end = 20.dp,
                                                top = 20.dp,
                                                bottom = 20.dp
                                            ),
                                        colors = ButtonDefaults.buttonColors(backgroundColor = green_button_color),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    {
                                        Text(
                                            text = "Start",
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
                                        )
                                    }


                                }
                                Box(Modifier.weight(1f)) {

                                    Button(
                                        onClick = {
                                            if (latlongList.size>0){
                                            if (permissions.all {
                                                    ContextCompat.checkSelfPermission(
                                                        context,
                                                        it
                                                    ) == PackageManager.PERMISSION_GRANTED
                                                }) {

                                                Log.e("@@finalList", latlongList.toString())
                                                submitDialog = true
                                                latlongList.clear()
                                                c1=0
                                                currentLocation = LatLng(0.0, 0.0)
                                                // Get the location
                                                locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
                                            } else {
                                                launcherMultiplePermissions.launch(permissions)
                                            }
                                            }else{
                                                openMySnackbar = true
                                                snackBarMessage = "Firstly Create GeoTagged Boundry..."
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start = 20.dp,
                                                end = 20.dp,
                                                top = 20.dp,
                                                bottom = 20.dp
                                            ),
                                        colors = ButtonDefaults.buttonColors(backgroundColor = green_button_color),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    {
                                        Text(
                                            text = "Stop",
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
                                        )
                                    }
                                    SnackbarWithoutScaffold(snackBarMessage, openMySnackbar, {openMySnackbar = it})
                                }

                            }
                            Text(text = "Latitude : " + currentLocation.latitude,Modifier.padding(start = 20.dp))
                            Text(text = "Longitude : " + currentLocation.longitude,Modifier.padding(start = 20.dp))
                        }


                    }

                }
            }
        }
    }

    @Suppress("DEPRECATION")
    fun getLocationRequest() : LocationRequest {
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 3000
        return locationRequest
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationCallback?.let {
            val locationRequest = getLocationRequest()
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                it,
                Looper.getMainLooper()
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (locationRequired) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
    }


    @Composable
    fun submitBoundry() {
        AlertDialog(
            backgroundColor = yellow_light,
            title = {
               Text(
                    text = "Geotagged Boundry",
                    color = green_button_color,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(
                            start = 2.dp,
                            top = 10.dp,
                            end = 2.dp,
                        )
                        .fillMaxWidth()
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Boundry Saved Successfully !!",
                        color = Color.Black,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(
                                start = 2.dp,
                                end = 2.dp,
                                bottom = 5.dp
                            )
                            .fillMaxWidth()
                    )


                }


            },
            onDismissRequest = {

            },

            buttons = {
                Box(modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .padding(
                                top = 10.dp,
                                bottom = 25.dp

                            )
                            .wrapContentWidth()
                            .clickable {
                                finish()
                            },
                        backgroundColor = green_button_color
                    ) {
                        Text(
                            text = "Okay",
                            color = Color.White,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(
                                start = 30.dp,
                                end = 30.dp,
                                top =5.dp,
                                bottom = 5.dp
                            )
                        )
                    }

                }

            }

        )
    }

}

var latlongList = ArrayList<LatLng>()

@SuppressLint("MissingPermission", "UnrememberedMutableState")
@Composable
fun MapScreen(currentLocation: LatLng) {
    val context = LocalContext.current

    var deviceLatLng by remember {
        mutableStateOf(LatLng(0.0, 0.0))
    }
    deviceLatLng = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
    latlongList.add(LatLng(currentLocation!!.latitude, currentLocation!!.longitude))

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(deviceLatLng, 18f)
    }
    val uiSettings = remember {
        MapUiSettings(myLocationButtonEnabled = true)
    }
    var properties by remember {
        mutableStateOf(MapProperties(mapType = MapType.SATELLITE, isMyLocationEnabled = true))
    }
    GoogleMap(
        cameraPositionState = cameraPositionState,
        uiSettings = uiSettings,
        properties = properties
    ) {
        val icon = bitmapDescriptor(
            context, R.drawable.ic_man_walking_15
        )
        Marker(
            state = MarkerState(
                position = deviceLatLng
            ),
            icon = icon,
        )

        if (latlongList.size>0){
            val pattern = listOf(Dot(), Gap(20f))
            var a = removeDuplicateLatlong(latlongList)
            Polyline(points = a.toMutableStateList(), color = Color.Green, pattern = pattern, width = 8f, geodesic = true, clickable = true)
        }

    }
}

fun bitmapDescriptor(
    context: Context,
    vectorResId: Int
): BitmapDescriptor? {

    // retrieve the actual drawable
    val drawable = ContextCompat.getDrawable(context, vectorResId) ?: return null
    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    val bm = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )

    // draw it onto the bitmap
    val canvas = android.graphics.Canvas(bm)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bm)
}

fun <LatLng> removeDuplicateLatlong(list: ArrayList<LatLng>): ArrayList<LatLng> {
    val newList = ArrayList<LatLng>()
    for (element in list) {
        if (!newList.contains(element)) {
            newList.add(element)
        }
    }
    return newList
}



