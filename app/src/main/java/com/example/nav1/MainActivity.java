package com.example.nav1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


//import com.mapbox.mapboxandroiddemo.R;
//import com.mapbox.mapboxsdk.maps.Style;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationEngineListener,
        PermissionsListener, MapboxMap.OnMapClickListener {

    private final double BIUMinLongitude = 34.836282;
    private final double BIUMaxLongitude = 34.854054;
    private final double BIUMinLatitude = 32.063779;
    private final double BIUMaxLatitude = 32.076654;

    private MapView mapView;
    private MapboxMap map;
    private Button startNavigationButton;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;
    private EditText srcET;
    private EditText dstET;
    private Location currentLocation;
    private Point originPosition;
    private Marker originMarker;
    private Point destinationPosition;
    private Marker destinationMarker;
    private boolean permissionAccepted = false;
    private NavigationMapRoute navigationMapRoute;
    private static final String TAG = "MainActivity";
    private CheckBox cb;
    private Map<Long, MapIcon> icons;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);

        //check if internet is available
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            //we are connected to a network
            Toast.makeText(getBaseContext(), "internet",
                    Toast.LENGTH_LONG).show();

        }
        else
            Toast.makeText(getBaseContext(), "no internet",
                    Toast.LENGTH_LONG).show();

        mapView = (MapView) findViewById(R.id.projMap);
        startNavigationButton = findViewById(R.id.navigationBT);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        //create the tabLayout for choosing the route type
        String[] tabsTitles = {"cycling","driving","walking"};
        int[] tabsIcons = {R.drawable.cycling, R.drawable.driving, R.drawable.walking};
        tabLayout = findViewById(R.id.routeTypeTL);
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            LinearLayout customTab = (LinearLayout) LayoutInflater.from(this)
                    .inflate(R.layout.my_dropdown_item, null);
            TextView tab_text = customTab.findViewById(R.id.text);
            tab_text.setText("  " + tabsTitles[i]);
            tab_text.setCompoundDrawablesWithIntrinsicBounds(tabsIcons[i], 0, 0, 0);
            tabLayout.getTabAt(i).setCustomView(tab_text);
        }

        //selecting the "walking" route type as a default(in index 2)
        tabLayout.selectTab(tabLayout.getTabAt(2));

        //when a tab is selected - changing the route if displayed(by the chosen type)
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //origin and destination aren't null => a route is displayed on the map
                if(originPosition != null && destinationPosition != null)
                {
                    routeHandler(originPosition, destinationPosition, false);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        //start the navigation when the navigation button is clicked
        startNavigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavigationLauncherOptions options = NavigationLauncherOptions.builder().directionsProfile(tabLayout.getTabAt(tabLayout.getSelectedTabPosition()).getText().toString())
                        .origin(originPosition)
                        .destination(destinationPosition).unitType(NavigationUnitType.TYPE_METRIC)
                        .shouldSimulateRoute(false).build();
                NavigationLauncher.startNavigation(MainActivity.this, options);
            }
        });

        srcET = findViewById(R.id.srcET);
        dstET = findViewById(R.id.dstET);
        cb = findViewById(R.id.currentLocationCB);
        Bitmap mBitmap = getBitmapFromVectorDrawable(this, R.drawable.src_marker);
        Icon srcIC = IconFactory.getInstance(MainActivity.this).fromBitmap(mBitmap);

        //when the checkbox for - using the current location is checked
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                //if checked - the origin location will be the current location + adding the marker on the map
                if(b){
                    double userLongitude = currentLocation.getLongitude();
                    double userLatitude = currentLocation.getLatitude();

                    //in BIU range(legal current location)
                    if((userLongitude >= BIUMinLongitude && userLongitude <= BIUMaxLongitude)
                            && (userLatitude >=BIUMinLatitude && userLatitude <= BIUMaxLatitude)){
                        srcET.setText("Current Location");
                        srcET.setEnabled(false);
                        if(originMarker != null)
                        {
                            map.removeMarker(originMarker);
                        }

                        originMarker = map.addMarker(new MarkerOptions()
                                .position(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()))
                                .icon(srcIC));
                        originPosition = Point.fromLngLat(currentLocation.getLongitude(), currentLocation.getLatitude());

                        Location originLocation = new Location("");
                        originLocation.setLatitude(originPosition.latitude());
                        originLocation.setLongitude(originPosition.longitude());
                        setCameraPosition(originLocation);

                        //origin and destination aren't null => a route is displayed on the map(updating the route)
                        if(originPosition != null && destinationPosition != null)
                        {
                            routeHandler(originPosition, destinationPosition, false);

                            //enabling the navigation button if a route is displayed + the origin location is the current location
                            if(cb.isChecked())
                            {
                                startNavigationButton.setEnabled(true);
                            }
                        }
                    }

                    else {
                        Toast.makeText(getBaseContext(), "Invalid Current Location - It Is Not In BIU Area",
                                Toast.LENGTH_LONG).show();
                        cb.setChecked(false);
                    }

                }

                //if unchecked - removing the origin marker from the map and setting it to null
                else
                {
                    srcET.setText("");
                    srcET.setEnabled(true);
                    if(originMarker != null)
                    {
                        map.removeMarker(originMarker);
                        originMarker = null;
                        originPosition = null;

                    }
                    //rmoving the route from the map if displayed + disabling the navigation button
                    routeHandler(originPosition, destinationPosition, true);
                    startNavigationButton.setEnabled(false);
                }
            }
        });

        //by pressing the "clear" icon on that is on the source location EditText - the text will be cleared +
        //the src marker will be removed from the map
        srcET.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    if(motionEvent.getX() >= (srcET.getRight() - srcET.getCompoundDrawables()[2].getBounds().width()))  {
                        srcET.setText("");

                        if(originMarker != null)
                        {
                            map.removeMarker(originMarker);
                            originMarker = null;
                            originPosition = null;
                            routeHandler(originPosition, destinationPosition, true);
                            startNavigationButton.setEnabled(false);

                        }

                        return true;
                    }
                }
                return false;
            }
        });

        //by pressing the "clear" icon on that is on the destination location EditText - the text will be cleared +
        // the dst marker will be removed from the map
        dstET.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    if(motionEvent.getX() >= (dstET.getRight() - dstET.getCompoundDrawables()[2].getBounds().width()))  {
                        dstET.setText("");

                        if(destinationMarker != null)
                        {
                            map.removeMarker(destinationMarker);
                            destinationMarker = null;
                            destinationPosition = null;
                            routeHandler(originPosition, destinationPosition, true);
                            startNavigationButton.setEnabled(false);
                        }

                        return true;
                    }
                }
                return false;
            }
        });

        //a map of the icons on the map(icon id(long) paired with the icon (MapIcon))
        icons = new HashMap<Long, MapIcon>();
    }

    //when the map is ready
    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        map.addOnMapClickListener(this);

        //removing the irrelevant layers(different data on the map) from the map
        List<Layer> layers = map.getLayers();
        for (Layer la : layers) {
            if(la.getId().contains("poi-scalerank"))
            {
                System.out.println(la.getId());
                map.removeLayer(la.getId());
            }
        }

        //checking if the location is enabled
        enableLocation();

        //handling the markers on the map
        int drawableID = getBaseContext().getResources().getIdentifier("ic_airport", "drawable", getBaseContext().getPackageName());
        Bitmap mBitmap = getBitmapFromVectorDrawable(this, drawableID);
        Icon c = IconFactory.getInstance(MainActivity.this).fromBitmap(mBitmap);
        Marker m = map.addMarker(new MarkerOptions().position(new LatLng(32.068,34.845)).icon(c));
        Marker m1 = map.addMarker(new MarkerOptions().position(new LatLng(32.067,34.843)).icon(c));
        //Icon c = IconFactory.getInstance(MainActivity.this).fromResource(R.drawable.common_google_signin_btn_icon_dark);
        //m = map.addMarker(new MarkerOptions().position(new LatLng(32.068,34.845)).setTitle("hello"));
        //m.getPosition();
        icons.put(m.getId(), new MapIcon(m.getId(), new LatLng(32.068,34.845), "ic_airport", "airplain1", "first airplain"));
        icons.put(m1.getId(), new MapIcon(m1.getId(), new LatLng(32.067,34.843), "ic_airport", "airplain2", "second airplain"));

        //when a marker is clicked
        map.setOnMarkerClickListener(new MapboxMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                MapIcon ic = icons.get(marker.getId());
                if(ic != null)
                {
                    navigationPointsHandler(ic.getPosition(), ic.getName());

                    return true;
                }
                return false;
            }
        });
    }

    //if the location permission is enabled - initializing the current location and some settings
    private void enableLocation(){
        CheckBox cb = findViewById(R.id.currentLocationCB);
        //if the location permission accepted - allowing to use the current location
        if(PermissionsManager.areLocationPermissionsGranted(this))
        {
            cb.setEnabled(true);
            permissionAccepted = true;
            initializeLocationEngine();
            initializeLocationLayer();

            if(currentLocation != null)
            {
                double userLongitude = currentLocation.getLongitude();
                double userLatitude = currentLocation.getLatitude();

                //in BIU range
                if((userLongitude >= BIUMinLongitude && userLongitude <= BIUMaxLongitude)
                        && (userLatitude >=BIUMinLatitude && userLatitude <= BIUMaxLatitude)){
                    Toast.makeText(getBaseContext(), "valid Current Location - In BIU Area",
                            Toast.LENGTH_LONG).show();
                    //default - using current location
                    cb.setChecked(true);
                }

                else {
                    Toast.makeText(getBaseContext(), "Invalid Current Location - It Is Not In BIU Area",
                            Toast.LENGTH_LONG).show();
                }
            }
            else
            {
                //origin location is null
                Toast.makeText(getBaseContext(), "Permission Accepted But The Current Location Is Invallid",
                        Toast.LENGTH_LONG).show();
                //create without user location option + without navigation(just displaying the route)
                cb.setEnabled(false);
            }


        }

        //if the location permission isn't accepted
        else
        {
            //create without user location option + without navigation(just show the route)
            cb.setEnabled(false);
            //requesting location permission again
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    //getting the current location
    @SuppressWarnings("MissingPermission")
    private void initializeLocationEngine() {
        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();
        locationEngine.requestLocationUpdates();
        Location lastLocation = locationEngine.getLastLocation();
        if(lastLocation != null)
        {
            currentLocation = lastLocation;
        }
    }

    //initilaizing navigation settings
    @SuppressWarnings("MissingPermission")
    private void initializeLocationLayer() {
        locationLayerPlugin = new LocationLayerPlugin(mapView, map, locationEngine);
        locationLayerPlugin.setLocationLayerEnabled(true);
        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
        locationLayerPlugin.setRenderMode(RenderMode.NORMAL);
        locationLayerPlugin.setLocationLayerEnabled(false);
    }

    //setting the camera position to a location
    private void setCameraPosition(Location location){
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
                location.getLongitude()), 16.0));
    }

    //if the map is clicked(not an icon) - handling the src and dst locations initialization
    @Override
    public void onMapClick(@NonNull LatLng point) {
        navigationPointsHandler(point, "no name");
    }

    //converting vector to bitmap(for the icons)
    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    //handling a click on the map or on an icon: setting the src/dst location,
    // displaying the src/dst marker on the map, and displaying the route on the map if both locations are available
    private void navigationPointsHandler(LatLng point, String pointName)
    {
        Bitmap srcBitmap = getBitmapFromVectorDrawable(this, R.drawable.src_marker);
        Icon srcIcn = IconFactory.getInstance(MainActivity.this).fromBitmap(srcBitmap);
        Bitmap dstBitmap = getBitmapFromVectorDrawable(this, R.drawable.dst_marker);
        Icon dstIcn = IconFactory.getInstance(MainActivity.this).fromBitmap(dstBitmap);
        //in BIU range
        if((point.getLongitude() >= BIUMinLongitude && point.getLongitude() <= BIUMaxLongitude)
                && (point.getLatitude() >=BIUMinLatitude && point.getLatitude() <= BIUMaxLatitude)){
            Toast.makeText(getBaseContext(), "In BIU",
                    Toast.LENGTH_LONG).show();
            if(originMarker == null)
            {
                originMarker = map.addMarker(new MarkerOptions().position(point).icon(srcIcn));
                originPosition = Point.fromLngLat(point.getLongitude(), point.getLatitude());
                if(pointName != "no name")
                {
                    srcET.setText(pointName);
                }
                else
                {
                    srcET.setText(point.getLatitude() + " , " + point.getLongitude());
                }

                Location originLocation = new Location("");
                originLocation.setLatitude(originPosition.latitude());
                originLocation.setLongitude(originPosition.longitude());
                setCameraPosition(originLocation);
            }

            else
            {
                if(destinationMarker != null)
                {
                    map.removeMarker(destinationMarker);
                }
                destinationMarker = map.addMarker(new MarkerOptions().position(point).icon(dstIcn));
                destinationPosition = Point.fromLngLat(point.getLongitude(), point.getLatitude());

                if(pointName != "no name")
                {
                    dstET.setText(pointName);
                }
                else
                {
                    dstET.setText(point.getLatitude() + " , " + point.getLongitude());
                }
            }

            //origin and destination aren't null => a route is displayed on the map
            if(originPosition != null && destinationPosition != null)
            {
                routeHandler(originPosition, destinationPosition, false);
                if(cb.isChecked())
                {
                   // startNavigationButton.setBackgroundColor(getResources().getColor(R.color.mapbox_navigation_route_layer_blue));
                    startNavigationButton.setEnabled(true);
                }
            }
        }

        else {
            Toast.makeText(getBaseContext(), "Not In BIU",
                    Toast.LENGTH_LONG).show();
        }
    }


    //add toasts + enable choosing the route type(walking...)
    private void routeHandler(Point origin, Point destination, boolean removeRoute){
        if(removeRoute)
        {
            if(navigationMapRoute != null)
            {
                navigationMapRoute.removeRoute();
            }
        }
        else
        {
            //Spinner sp = findViewById(R.id.routeTypeSP);
            NavigationRoute.builder().accessToken(Mapbox.getAccessToken()).origin(origin).destination(destination).profile(tabLayout.getTabAt(tabLayout.getSelectedTabPosition()).getText().toString())
                    .build().getRoute(new Callback<DirectionsResponse>() {
                @Override
                public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                    if(response.body() == null)
                    {
                        Log.e(TAG, "No routes found, check right user and access token");
                        return;
                    }
                    else if(response.body().routes().size() == 0)
                    {
                        Log.e(TAG, "No routes found");
                        return;
                    }

                    //we have route - getting the highest ranked route
                    DirectionsRoute currentRoute = response.body().routes().get(0);

                    if(navigationMapRoute != null)
                    {
                        navigationMapRoute.removeRoute();
                    }
                    else
                    {
                        navigationMapRoute = new NavigationMapRoute(null, mapView, map);
                    }

                    navigationMapRoute.addRoute(currentRoute);
                }

                @Override
                public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                    Log.e(TAG, "Error:" + t.getMessage());
                }
            });
        }

    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location != null){
            currentLocation = location;
            setCameraPosition(location);
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {
        if(granted){
            enableLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //ACTIVITY LIFECYCLE EVENTS

    @Override
    @SuppressWarnings("MissingPermission")
    protected void onStart() {
        super.onStart();
        if(locationEngine != null)
        {
            locationEngine.requestLocationUpdates();
        }

        if(locationLayerPlugin != null)
        {
            locationLayerPlugin.onStart();
        }
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(locationEngine != null)
        {
            locationEngine.removeLocationUpdates();
        }
        if(locationLayerPlugin != null)
        {
            locationLayerPlugin.onStop();
        }
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(locationEngine != null) {
            locationEngine.deactivate();
        }
        mapView.onDestroy();
    }
}