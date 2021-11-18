package com.mapbox.vision.examples

import android.location.Location
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener
import com.mapbox.services.android.navigation.v5.route.RouteFetcher
import com.mapbox.services.android.navigation.v5.route.RouteListener
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.vision.VisionManager
import com.mapbox.vision.ar.VisionArManager
import com.mapbox.vision.ar.core.models.ManeuverType
import com.mapbox.vision.ar.core.models.Route
import com.mapbox.vision.ar.core.models.RoutePoint
import com.mapbox.vision.ar.view.gl.VisionArView
import com.mapbox.vision.mobile.core.interfaces.VisionEventsListener
import com.mapbox.vision.mobile.core.models.position.GeoCoordinate
import com.mapbox.vision.performance.ModelPerformance
import com.mapbox.vision.performance.ModelPerformanceMode
import com.mapbox.vision.performance.ModelPerformanceRate
import com.mapbox.vision.utils.VisionLogger
import kotlinx.android.synthetic.main.activity_ar_navigation.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Example shows how Vision and VisionAR SDKs are used to draw AR lane over the video stream from camera.
 * Also, Mapbox navigation services are used to build route and  navigation session.
 */
open class ArActivityKt : BaseActivity(), RouteListener, ProgressChangeListener, OffRouteListener {

    companion object {
        private var TAG = ArActivityKt::class.java.simpleName
    }

    // Handles navigation.
    private lateinit var mapboxNavigation: MapboxNavigation
    // Fetches route from points.
    private lateinit var routeFetcher: RouteFetcher
    private lateinit var lastRouteProgress: RouteProgress
    private lateinit var directionsRoute: DirectionsRoute

    private var visionManagerWasInit = false
    private var navigationWasStarted = false

    private val arLocationEngine by lazy {
        LocationEngineProvider.getBestLocationEngine(this)
    }

    private val arLocationEngineRequest by lazy {
        LocationEngineRequest.Builder(0)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setFastestInterval(1000)
            .build()
    }

    private val locationCallback by lazy {
        object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {}

            override fun onFailure(exception: Exception) {}
        }
    }

    // This dummy points will be used to build route. For real world test this needs to be changed to real values for
    // source and target locations.
//    24.4867629,54.4033715
    private var ROUTE_ORIGIN = Point.fromLngLat(24.4867629,54.4033715)
//    24.488158844051057, 54.380316833657396
    private var ROUTE_DESTINATION = Point.fromLngLat(24.488158844051057, 54.380316833657396)

    protected open fun setArRenderOptions(visionArView: VisionArView) {
        // enable fence rendering
        visionArView.setFenceVisible(true)
    }

    override fun onPermissionsGranted() {
        startVisionManager()
        startNavigation()
        Log.e("coofee", "$ROUTE_ORIGIN+$ROUTE_DESTINATION")
    }

    override fun initViews() {
        setContentView(R.layout.activity_ar_navigation)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        try {
            val startLat = intent.getStringExtra("startLat").toDouble()
            val startLng = intent.getStringExtra("startLng").toDouble()
            val endLat = intent.getStringExtra("endLat").toDouble()
            val endLng = intent.getStringExtra("endLng").toDouble()
            ROUTE_ORIGIN = Point.fromLngLat(startLng, startLat)
            ROUTE_DESTINATION = Point.fromLngLat(endLng, endLat)
        } catch (e: java.lang.Exception) {
            Toast.makeText(this, "Data format error!!!", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        startVisionManager()
        startNavigation()
    }

    override fun onStop() {
        super.onStop()
        stopVisionManager()
        stopNavigation()
    }

    private fun startVisionManager() {
        if (allPermissionsGranted() && !visionManagerWasInit) {
            // Create and start VisionManager.
            VisionManager.create()
            VisionManager.setModelPerformance(
                ModelPerformance.On(
                    ModelPerformanceMode.DYNAMIC,
                    ModelPerformanceRate.LOW
                )
            )
            VisionManager.start()
            VisionManager.visionEventsListener = object : VisionEventsListener {}

            // Create VisionArManager.
            VisionArManager.create(VisionManager)
            mapbox_ar_view.setArManager(VisionArManager)
            setArRenderOptions(mapbox_ar_view)

            visionManagerWasInit = true
        }
    }

    private fun stopVisionManager() {
        if (visionManagerWasInit) {
            VisionArManager.destroy()
            VisionManager.stop()
            VisionManager.destroy()

            visionManagerWasInit = false
        }
    }

    private fun startNavigation() {
        if (allPermissionsGranted() && !navigationWasStarted) {
            // Initialize navigation with your Mapbox access token.
            mapboxNavigation = MapboxNavigation(
                this,
                getString(R.string.mapbox_access_token),
                MapboxNavigationOptions.builder().build()
            )

            // Initialize route fetcher with your Mapbox access token.
            routeFetcher = RouteFetcher(this, getString(R.string.mapbox_access_token))
            routeFetcher.addRouteListener(this)

            try {
                arLocationEngine.requestLocationUpdates(
                    arLocationEngineRequest,
                    locationCallback,
                    mainLooper
                )
            } catch (se: SecurityException) {
                VisionLogger.e(TAG, se.toString())
            }

            initDirectionsRoute()

            // Route need to be reestablished if off route happens.
            mapboxNavigation.addOffRouteListener(this)
            mapboxNavigation.addProgressChangeListener(this)

            navigationWasStarted = true
        }
    }

    private fun stopNavigation() {
        if (navigationWasStarted) {
            arLocationEngine.removeLocationUpdates(locationCallback)

            mapboxNavigation.removeProgressChangeListener(this)
            mapboxNavigation.removeOffRouteListener(this)
            mapboxNavigation.stopNavigation()

            navigationWasStarted = false
        }
    }

    private fun initDirectionsRoute() {
        NavigationRoute.builder(this)
            .accessToken(getString(R.string.mapbox_access_token))
            .origin(ROUTE_ORIGIN)
            .destination(ROUTE_DESTINATION)
            .build()
            .getRoute(object : Callback<DirectionsResponse> {
                override fun onResponse(
                    call: Call<DirectionsResponse>,
                    response: Response<DirectionsResponse>
                ) {
                    if (response.body() == null || response.body()!!.routes().isEmpty()) {
                        return
                    }

                    directionsRoute = response.body()!!.routes()[0]
                    mapboxNavigation.startNavigation(directionsRoute)

                    // Set route progress.
                    VisionArManager.setRoute(
                        Route(
                            directionsRoute.getRoutePoints(),
                            directionsRoute.duration()?.toFloat() ?: 0f,
                            "",
                            ""
                        )
                    )
                }

                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

    override fun onErrorReceived(throwable: Throwable?) {
        throwable?.printStackTrace()

        mapboxNavigation.stopNavigation()
        Toast.makeText(this, "Can not calculate the route requested", Toast.LENGTH_SHORT).show()
    }

    override fun onResponseReceived(response: DirectionsResponse, routeProgress: RouteProgress?) {
        mapboxNavigation.stopNavigation()
        if (response.routes().isEmpty()) {
            Toast.makeText(this, "Can not calculate the route requested", Toast.LENGTH_SHORT).show()
        } else {
            mapboxNavigation.startNavigation(response.routes()[0])

            val route = response.routes()[0]

            // Set route progress.
            VisionArManager.setRoute(
                Route(
                    route.getRoutePoints(),
                    route.duration()?.toFloat() ?: 0f,
                    "",
                    ""
                )
            )
        }
    }

    override fun onProgressChange(location: Location, routeProgress: RouteProgress) {
        lastRouteProgress = routeProgress
    }

    override fun userOffRoute(location: Location) {
        routeFetcher.findRouteFromRouteProgress(location, lastRouteProgress)
    }

    private fun DirectionsRoute.getRoutePoints(): Array<RoutePoint> {
        val routePoints = arrayListOf<RoutePoint>()
        legs()?.forEach { leg ->
            leg.steps()?.forEach { step ->
                val maneuverPoint = RoutePoint(
                    GeoCoordinate(
                        latitude = step.maneuver().location().latitude(),
                        longitude = step.maneuver().location().longitude()
                    ),
                    step.maneuver().type().mapToManeuverType()
                )
                routePoints.add(maneuverPoint)

                step.geometry()
                    ?.buildStepPointsFromGeometry()
                    ?.map { geometryStep ->
                        RoutePoint(
                            GeoCoordinate(
                                latitude = geometryStep.latitude(),
                                longitude = geometryStep.longitude()
                            )
                        )
                    }
                    ?.let { stepPoints ->
                        routePoints.addAll(stepPoints)
                    }
            }
        }

        return routePoints.toTypedArray()
    }

    private fun String.buildStepPointsFromGeometry(): List<Point> {
        return PolylineUtils.decode(this, Constants.PRECISION_6)
    }

    private fun String?.mapToManeuverType(): ManeuverType = when (this) {
        "turn" -> ManeuverType.Turn
        "depart" -> ManeuverType.Depart
        "arrive" -> ManeuverType.Arrive
        "merge" -> ManeuverType.Merge
        "on ramp" -> ManeuverType.OnRamp
        "off ramp" -> ManeuverType.OffRamp
        "fork" -> ManeuverType.Fork
        "roundabout" -> ManeuverType.Roundabout
        "exit roundabout" -> ManeuverType.RoundaboutExit
        "end of road" -> ManeuverType.EndOfRoad
        "new name" -> ManeuverType.NewName
        "continue" -> ManeuverType.Continue
        "rotary" -> ManeuverType.Rotary
        "roundabout turn" -> ManeuverType.RoundaboutTurn
        "notification" -> ManeuverType.Notification
        "exit rotary" -> ManeuverType.RoundaboutExit
        else -> ManeuverType.None
    }
    fun back(v: View?) {
        finish()
    }
}
