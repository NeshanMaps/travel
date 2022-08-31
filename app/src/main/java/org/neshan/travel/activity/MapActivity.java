package org.neshan.travel.activity;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;

import com.carto.core.ScreenBounds;
import com.carto.core.ScreenPos;
import com.carto.graphics.Color;
import com.carto.styles.AnimationStyle;
import com.carto.styles.AnimationStyleBuilder;
import com.carto.styles.AnimationType;
import com.carto.styles.LineStyle;
import com.carto.styles.LineStyleBuilder;
import com.carto.styles.MarkerStyle;
import com.carto.styles.MarkerStyleBuilder;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.squareup.picasso.Picasso;

import org.neshan.common.model.LatLng;
import org.neshan.common.model.LatLngBounds;
import org.neshan.common.utils.PolylineEncoding;
import org.neshan.mapsdk.MapView;
import org.neshan.mapsdk.internal.utils.BitmapUtils;
import org.neshan.mapsdk.model.Marker;
import org.neshan.mapsdk.model.Polyline;
import org.neshan.servicessdk.direction.NeshanDirection;
import org.neshan.servicessdk.direction.model.DirectionStep;
import org.neshan.servicessdk.direction.model.NeshanDirectionResult;
import org.neshan.servicessdk.direction.model.Route;
import org.neshan.servicessdk.distancematrix.NeshanDistanceMatrix;
import org.neshan.servicessdk.distancematrix.model.NeshanDistanceMatrixResult;
import org.neshan.travel.BuildConfig;
import org.neshan.travel.R;
import org.neshan.travel.database_helper.AssetDatabaseHelper;
import org.neshan.travel.model.Place;
import org.neshan.travel.model.Province;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends AppCompatActivity {

    public static final String CATEGORY_ID = "CATEGORY_ID";
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final float NEAR_PLACES_DISTANCE_KILOMETERS = 3;
    private final int REQUEST_CODE = 123;

    private SQLiteDatabase dB;

    private MapView mapView;

    private Location userLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private Marker marker;
    private Polyline mapPolyline;
    private BottomSheetDialog placeInfoBottomSheetDialog;
    private BottomSheetDialog routingBottomSheetDialog;
    private ProgressBar routingProgressBar;
    private AppCompatButton btnRouting;

    private ArrayList<LatLng> decodedStepByStepPath;

    private List<Place> places;
    private Place selectedPlace;

    private boolean routing;

    private final String NEAR_PLACES_CATEGORY_ID = "2be3a2ee-ea18-11ec-8fea-0242ac120002";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initLayoutReferences();
        if (getIntent() != null && getIntent().getStringExtra(CATEGORY_ID) != null) {
            if (!getIntent().getStringExtra(CATEGORY_ID).equals(NEAR_PLACES_CATEGORY_ID)) {
                places = getPlaces(getIntent().getStringExtra(CATEGORY_ID));
                addPlacesMarker(places);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkLocationPermission()) {
                        routing = false;
                        initLocation();
                        startLocationUpdates();
                    } else {
                        getLocationPermission();
                    }
                } else {
                    initLocation();
                    startLocationUpdates();
                }
            }
        }
    }

    private List<Place> findAndShowNearPlaces() {
        AssetDatabaseHelper myDbHelper = new AssetDatabaseHelper(this);

        try {
            myDbHelper.createDataBase();
        } catch (IOException ioe) {
            throw new Error("Unable to create database");
        }

        try {
            dB = myDbHelper.openDataBase();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        Cursor cursor = dB.rawQuery("select * from place", null);

        List<Place> places = new ArrayList<>();
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                Place place = new Place();
                place.setImageAddress(cursor.getString(cursor.getColumnIndex("image_address")))
                        .setDescription(cursor.getString(cursor.getColumnIndex("description")))
                        .setLat(cursor.getDouble(cursor.getColumnIndex("lat")))
                        .setLng(cursor.getDouble(cursor.getColumnIndex("lng")))
                        .setProvinceId(cursor.getString(cursor.getColumnIndex("province_id")))
                        .setId(cursor.getString(cursor.getColumnIndex("id")))
                        .setName(cursor.getString(cursor.getColumnIndex("name")));
                places.add(place);
                cursor.moveToNext();
            }

        }
        cursor.close();
        List<Place> nearPlaces = new ArrayList<>();
        for (Place place : places) {
            Location location = new Location("newlocation");
            location.setLatitude(place.getLat());
            location.setLongitude(place.getLng());
            if (userLocation != null && userLocation.distanceTo(location) / 1000 <= NEAR_PLACES_DISTANCE_KILOMETERS) {
                nearPlaces.add(place);
            }
        }
        if (!nearPlaces.isEmpty()) {
            addPlacesMarker(nearPlaces);
        } else {
            Toast.makeText(MapActivity.this, getString(R.string.no_places_found), Toast.LENGTH_LONG).show();
            finish();
        }

        return nearPlaces;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapPolyline != null) {

        }
    }

    private void addPlacesMarker(List<Place> places) {
        double minLat = places.get(0).getLat();
        double minLng = places.get(0).getLng();
        double maxLat = places.get(0).getLat();
        double maxLng = places.get(0).getLng();
        for (Place place : places) {
            LatLng latLng = new LatLng(place.getLat(), place.getLng());
            Marker marker = createMarker(latLng);
            marker.putMetadata("place_id", place.getId());
            mapView.addMarker(marker);
            minLat = Math.min(latLng.getLatitude(), minLat);
            minLng = Math.min(latLng.getLongitude(), minLng);
            maxLat = Math.max(latLng.getLatitude(), maxLat);
            maxLng = Math.max(latLng.getLongitude(), maxLng);
        }
//        mapView.moveToCameraBounds(
//                new LatLngBounds(new LatLng(minLat, maxLng), new LatLng(maxLat, minLng)),
//                new ScreenBounds(
//                        new ScreenPos(0, 0),
//                        new ScreenPos(mapView.getWidth(), mapView.getHeight())
//                ),
//                true, 0.5f);

        LatLng northEast = new LatLng(maxLat, maxLng);
        LatLng southWest = new LatLng(minLat, minLng);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mapView.moveToCameraBounds(new LatLngBounds(northEast, southWest), new ScreenBounds(new ScreenPos(0, 0), new ScreenPos(mapView.getWidth(), mapView.getHeight())), true, 0);
            }
        },200);

    }

    private void initLayoutReferences() {
        initViews();
        initMap();
    }

    private void initMap() {
//        mapView.moveCamera(new LatLng(35.767234, 51.330743), 0);
//        mapView.setZoom(14, 0);
        mapView.getSettings().setZoomControlsEnabled(true);

        mapView.setOnMarkerClickListener(new MapView.OnMarkerClickListener() {
            @Override
            public void OnMarkerClicked(Marker marker) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (Place place : places) {
                            if (place.getId().equals(marker.getMetadata("place_id"))) {
                                selectedPlace = place;
                                break;
                            }
                        }

                        showPlaceInfoBottomSheetDialog();
                    }
                });
            }
        });
    }

    private void initViews() {
        mapView = findViewById(R.id.map_view);
    }

    private Province getProvince(String id) {
        AssetDatabaseHelper myDbHelper = new AssetDatabaseHelper(this);

        try {
            myDbHelper.createDataBase();
        } catch (IOException ioe) {
            throw new Error("Unable to create database");
        }

        try {
            dB = myDbHelper.openDataBase();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        Cursor cursor = dB.rawQuery("select * from province where id='" + id + "'", null);

        Province province = null;
        if (cursor.moveToFirst()) {
            province = new Province();
            province.setNorthEastLat(cursor.getDouble(cursor.getColumnIndex("north_east_lat")))
                    .setNorthEastLng(cursor.getDouble(cursor.getColumnIndex("north_east_lng")))
                    .setSouthWestLat(cursor.getDouble(cursor.getColumnIndex("south_west_lat")))
                    .setSouthWestLng(cursor.getDouble(cursor.getColumnIndex("south_west_lng")))
                    .setId(cursor.getString(cursor.getColumnIndex("id")))
                    .setName(cursor.getString(cursor.getColumnIndex("name")));
        }
        cursor.close();
        return province;
    }

    private List<Place> getPlaces(String categoryId) {
        AssetDatabaseHelper myDbHelper = new AssetDatabaseHelper(this);

        try {
            myDbHelper.createDataBase();
        } catch (IOException ioe) {
            throw new Error("Unable to create database");
        }

        try {
            dB = myDbHelper.openDataBase();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }

        Cursor cursor = dB.rawQuery("select * from place where category_id='" + categoryId + "'", null);

        List<Place> places = new ArrayList<>();
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                Place place = new Place();
                place.setImageAddress(cursor.getString(cursor.getColumnIndex("image_address")))
                        .setDescription(cursor.getString(cursor.getColumnIndex("description")))
                        .setLat(cursor.getDouble(cursor.getColumnIndex("lat")))
                        .setLng(cursor.getDouble(cursor.getColumnIndex("lng")))
                        .setProvinceId(cursor.getString(cursor.getColumnIndex("province_id")))
                        .setId(cursor.getString(cursor.getColumnIndex("id")))
                        .setName(cursor.getString(cursor.getColumnIndex("name")));
                places.add(place);
                cursor.moveToNext();
            }

        }
        cursor.close();
        return places;
    }

    private Marker createMarker(LatLng loc) {
        AnimationStyleBuilder animStBl = new AnimationStyleBuilder();
        animStBl.setFadeAnimationType(AnimationType.ANIMATION_TYPE_SMOOTHSTEP);
        animStBl.setSizeAnimationType(AnimationType.ANIMATION_TYPE_SPRING);
        animStBl.setPhaseInDuration(0.5f);
        animStBl.setPhaseOutDuration(0.5f);
        AnimationStyle animSt = animStBl.buildStyle();

        MarkerStyleBuilder markStCr = new MarkerStyleBuilder();
        markStCr.setSize(40f);
        markStCr.setClickSize(70);
        markStCr.setBitmap(BitmapUtils.createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_marker)));
        markStCr.setAnimationStyle(animSt);
        MarkerStyle markSt = markStCr.buildStyle();

        // Creating marker
        return new Marker(loc, markSt);
    }

    private void showPlaceInfoBottomSheetDialog() {
        placeInfoBottomSheetDialog = new BottomSheetDialog(mapView.getContext());
        placeInfoBottomSheetDialog.setContentView(R.layout.place_bottom_sheet);

        ImageView imgPlace = placeInfoBottomSheetDialog.findViewById(R.id.img_place);
        AppCompatTextView lblName = placeInfoBottomSheetDialog.findViewById(R.id.lbl_name);
        AppCompatTextView lblDescription = placeInfoBottomSheetDialog.findViewById(R.id.lbl_desc);
        btnRouting = placeInfoBottomSheetDialog.findViewById(R.id.btn_routing);
        routingProgressBar = placeInfoBottomSheetDialog.findViewById(R.id.progress_routing);

        Picasso.get().load(selectedPlace.getImageAddress()).into(imgPlace);
        lblName.setText(selectedPlace.getName());
        lblDescription.setText(selectedPlace.getDescription());

        btnRouting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                routing = true;
                btnRouting.setVisibility(View.GONE);
                routingProgressBar.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkLocationPermission()) {
                        initLocation();
                        startLocationUpdates();
                    } else {
                        getLocationPermission();
                    }
                } else {
                    initLocation();
                    startLocationUpdates();
                }
            }
        });

        placeInfoBottomSheetDialog.show();
    }

    private void showRoutingBottomSheetDialog(String duration, String distance) {
        routingBottomSheetDialog = new BottomSheetDialog(mapView.getContext());
        routingBottomSheetDialog.setContentView(R.layout.routing_detail_bottom_sheet);

        AppCompatTextView lblDuration = routingBottomSheetDialog.findViewById(R.id.lbl_duration);
        AppCompatTextView lblDistance = routingBottomSheetDialog.findViewById(R.id.lbl_distance);

        lblDuration.setText(duration);
        lblDistance.setText(distance);

        routingBottomSheetDialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE && checkLocationPermission()) {
            initLocation();
            startLocationUpdates();
        }
    }

    private boolean checkGpsStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void initLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // location is received
                userLocation = locationResult.getLastLocation();

                onLocationChange();
                stopLocationUpdates();
            }
        };

        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();

    }

    private void neshanRoutingApi(LatLng source, LatLng destination) {
        new NeshanDirection.Builder("service.UKmFt1MJn5oHjwf1QJWoQwrP3lsGrVjcu3eNujx7", source, destination)
                .build().call(new Callback<NeshanDirectionResult>() {
                    @Override
                    public void onResponse(Call<NeshanDirectionResult> call, Response<NeshanDirectionResult> response) {
                        btnRouting.setVisibility(View.GONE);
                        routingProgressBar.setVisibility(View.GONE);
                        // two type of routing
                        if (response != null && response.body() != null && response.body().getRoutes() != null && !response.body().getRoutes().isEmpty()) {
                            Route route = response.body().getRoutes().get(0);
                            decodedStepByStepPath = new ArrayList<>();

                            // decoding each segment of steps and putting to an array
                            for (DirectionStep step : route.getLegs().get(0).getDirectionSteps()) {
                                decodedStepByStepPath.addAll(PolylineEncoding.decode(step.getEncodedPolyline()));
                            }

                            if (mapPolyline != null) {
                                mapView.removePolyline(mapPolyline);
                            }
                            mapPolyline = new Polyline(decodedStepByStepPath, getLineStyle());

                            //draw polyline between route points
                            mapView.addPolyline(mapPolyline);

                            // focusing camera on first point of drawn line
                            mapSetPosition(source, destination);
                            calculateDistance(destination);
                            Location sourceLocation = new Location("");
                            sourceLocation.setLatitude(source.getLatitude());
                            sourceLocation.setLongitude(source.getLongitude());
                        }
                    }

                    @Override
                    public void onFailure(Call<NeshanDirectionResult> call, Throwable t) {
                        routingProgressBar.setVisibility(View.GONE);
                        btnRouting.setVisibility(View.VISIBLE);
                        Log.e("err", t.toString());
                    }
                });
    }

    private void calculateDistance(LatLng destination) {
        ArrayList<LatLng> sourceArr = new ArrayList<>();
        sourceArr.add(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()));
        ArrayList<LatLng> destinationArr = new ArrayList<>();
        destinationArr.add(destination);
        new NeshanDistanceMatrix.Builder("service.UKmFt1MJn5oHjwf1QJWoQwrP3lsGrVjcu3eNujx7", sourceArr, destinationArr).build().call(new Callback<NeshanDistanceMatrixResult>() {
            @Override
            public void onResponse(Call<NeshanDistanceMatrixResult> call, Response<NeshanDistanceMatrixResult> response) {
                showRoutingBottomSheetDialog(response.body().getRows().get(0).getElements().get(0).getDuration().getText(), response.body().getRows().get(0).getElements().get(0).getDistance().getText());
            }

            @Override
            public void onFailure(Call<NeshanDistanceMatrixResult> call, Throwable t) {
                Log.d("asd", "asd");
            }
        });
    }

    private void mapSetPosition(LatLng source, LatLng destination) {

        double minLat = Math.min(source.getLatitude(), destination.getLatitude());
        double minLng = Math.min(source.getLongitude(), destination.getLongitude());

        double maxLat = Math.max(source.getLatitude(), destination.getLatitude());
        double maxLng = Math.max(source.getLongitude(), destination.getLongitude());

        LatLng northEast = new LatLng(maxLat, maxLng);
        LatLng southWest = new LatLng(minLat, minLng);

        mapView.moveToCameraBounds(new LatLngBounds(northEast, southWest), new ScreenBounds(new ScreenPos(0, 0), new ScreenPos(mapView.getWidth(), mapView.getHeight())), true, .5f);

    }

    private LineStyle getLineStyle() {
        LineStyleBuilder lineStCr = new LineStyleBuilder();
        lineStCr.setColor(new Color((short) 2, (short) 119, (short) 189, (short) 190));
        lineStCr.setWidth(10f);
        lineStCr.setStretchFactor(0f);
        return lineStCr.buildStyle();
    }

    /**
     * Starting location updates
     * Check whether location settings are satisfied and then
     * location updates will be requested
     */
    private void startLocationUpdates() {
        settingsClient
                .checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        //noinspection MissingPermission
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MapActivity.this, REQUEST_CODE);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);

                                Toast.makeText(MapActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }

//                        onLocationChange();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                initLocation();
                startLocationUpdates();
            } else {
                routingProgressBar.setVisibility(View.GONE);
                btnRouting.setVisibility(View.VISIBLE);
            }
        }
    }

    public void stopLocationUpdates() {
        // Removing location updates
        fusedLocationClient
                .removeLocationUpdates(locationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
//                        Toast.makeText(getApplicationContext(), "Location updates stopped!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void startReceivingLocationUpdates() {
        // Requesting ACCESS_FINE_LOCATION using Dexter library
//        Dexter.withActivity(this)
//                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
//                .withListener(new PermissionListener() {
//                    @Override
//                    public void onPermissionGranted(PermissionGrantedResponse response) {
//                        startLocationUpdates();
//                    }
//
//                    @Override
//                    public void onPermissionDenied(PermissionDeniedResponse response) {
//                        if (response.isPermanentlyDenied()) {
//                            // open device settings when the permission is
//                            // denied permanently
//                            openSettings();
//                        }
//                    }
//
//                    @Override
//                    public void onPermissionRationaleShouldBeShown(com.karumi.dexter.listener.PermissionRequest permission, PermissionToken token) {
//                        token.continuePermissionRequest();
//                    }
//
//                }).check();
    }

    private void openSettings() {
        Intent intent = new Intent();
        intent.setAction(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package",
                BuildConfig.APPLICATION_ID, null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void onLocationChange() {
        if (userLocation != null) {
            addUserMarker(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()));
            if (!routing) {
                places = findAndShowNearPlaces();
            } else {
                neshanRoutingApi(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()), new LatLng(selectedPlace.getLat(), selectedPlace.getLng()));
                placeInfoBottomSheetDialog.dismiss();
            }
        }
    }

    // This method gets a LatLng as input and adds a marker on that position
    private void addUserMarker(LatLng loc) {
        //remove existing marker from map
        if (marker != null) {
            mapView.removeMarker(marker);
        }
        // Creating marker style. We should use an object of type MarkerStyleCreator, set all features on it
        // and then call buildStyle method on it. This method returns an object of type MarkerStyle
        MarkerStyleBuilder markStCr = new MarkerStyleBuilder();
        markStCr.setSize(20f);
        markStCr.setBitmap(com.carto.utils.BitmapUtils.createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_user_marker)));
        MarkerStyle markSt = markStCr.buildStyle();

        // Creating user marker
        marker = new Marker(loc, markSt);

        // Adding user marker to map!
        mapView.addMarker(marker);
    }

//    public void focusOnUserLocation(View view) {
//        if (userLocation != null) {
//            mapView.moveCamera(
//                    new LatLng(userLocation.getLatitude(), userLocation.getLongitude()), 0.25f);
//            mapView.setZoom(15, 0.25f);
//        }
//    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean checkLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void getLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
    }
}