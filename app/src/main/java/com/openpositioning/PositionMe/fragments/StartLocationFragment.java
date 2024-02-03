package com.openpositioning.PositionMe.fragments;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.location.Location;
//import android.location.LocationRequest;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

import com.google.android.gms.location.LocationRequest;


import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.openpositioning.PositionMe.R;
import com.openpositioning.PositionMe.sensors.SensorFusion;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass. The startLocation fragment is displayed before the trajectory
 * recording starts. This fragment displays a map in which the user can adjust their location to
 * correct the PDR when it is complete
 *
 * @see HomeFragment the previous fragment in the nav graph.
 * @see RecordingFragment the next fragment in the nav graph.
 * @see SensorFusion the class containing sensors and recording.
 *
 * @author Virginia Cangelosi
 */
public class StartLocationFragment extends Fragment {

    //Button to go to next fragment and save the location
    private Button button;
    //Singleton SesnorFusion class which stores data from all sensors
    private SensorFusion sensorFusion = SensorFusion.getInstance();
    //Google maps LatLong object to pass location to the map
    private LatLng position;
    //Start position of the user to be stored
    private float[] startPosition = new float[2];
    //Zoom of google maps
    private float zoom = 19f;


    private boolean requestingLocationUpdates = false; // Add this line
    private LocationRequest locationRequest;

    private GoogleMap mMap;
    private Marker currentLocationMarker;
    private Polyline path;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private List<LatLng> pathPoints = new ArrayList<>();

    private boolean isIndoorMapShown = false;

    private GroundOverlay currentOverlay;
    private final int groundFloor = 0; // Start with the ground floor
    private int floorCount = 0;
    private final int maxFloor = 3; // This represents the third floor, making it 4 floors in total including the ground floor


    LatLngBounds libraryBounds = new LatLngBounds(
            new LatLng(55.922723, -3.175191), // SW corner
            new LatLng(55.923111, -3.174434)  // NE corner
    );

    private Button buttonFloorUp;
    private Button buttonFloorDown;






    /**
     * Public Constructor for the class.
     * Left empty as not required
     */
    public StartLocationFragment() {
        // Required empty public constructor
    }



/** GPT
 *  the onCreate method sets up your fragment to receive location updates and process
 *  them by updating a polyline on a map.
 *  This is a crucial part of implementing real-time location tracking in your app.
    */

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize LocationRequest
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(500)
                .setMaxUpdateDelayMillis(1000)
                .build();


        // Initialize the LocationCallback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());
                    Log.d("LocationUpdate", "Location: " + location.getLatitude() + ", " + location.getLongitude());

                    pathPoints.add(newPoint);
                    path.setPoints(pathPoints);

                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                    // Check if the user has entered the library bounds
                    if (libraryBounds.contains(userLocation) && !isIndoorMapShown) {
                        switchToIndoorMap();
                        isIndoorMapShown = true; // Set flag to true
                    } else if (!libraryBounds.contains(userLocation) && isIndoorMapShown) {
                        hideIndoorMap(); // Call a method to hide the indoor map
                        buttonFloorUp.setVisibility(View.GONE);
                        buttonFloorDown.setVisibility(View.GONE);

                        isIndoorMapShown = false; // Set flag to false
                    }
                }
            }
        };
    }

    private void showFloor(int floor) {
        if (currentOverlay != null) {
            currentOverlay.remove(); // Remove the current overlay
        }

        BitmapDescriptor floorDescriptor;
        switch (floor) {
            case 0:
                floorDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.library_g);
                break;
            case 1:
                floorDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.library1);
                break;
            case 2:
                floorDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.library2);
                break;
            case 3:
                floorDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.library3);
                break;
            default:
                throw new IllegalStateException("Unexpected floor: " + floor);
        }

        GroundOverlayOptions options = new GroundOverlayOptions()
                .image(floorDescriptor)
                .positionFromBounds(libraryBounds);
        currentOverlay = mMap.addGroundOverlay(options);

        floorCount = floor; // Update the current floor
    }

    private void nextFloor() {
        if (floorCount < maxFloor) {
            showFloor(++floorCount);
        }
    }

    private void previousFloor() {
        if (floorCount >= 1) {
            showFloor(--floorCount);
        }
    }

    private void switchToIndoorMap() {
        zoomToBuilding();
        // Remove existing overlay if present
        if (currentOverlay != null) {
            currentOverlay.remove();
        }

        buttonFloorUp.setVisibility(View.VISIBLE);
        buttonFloorDown.setVisibility(View.VISIBLE);
        showFloor(0); // Start by showing the ground floor
    }

    private void hideIndoorMap() {
        if (currentOverlay != null) {
            currentOverlay.remove(); // Remove the GroundOverlay from the map
            currentOverlay = null; // Clear the reference
        }
        // Optionally, adjust the map's camera or reset to default view
    }

    private void zoomToBuilding() {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                libraryBounds.getCenter(), // Center of the building
                18f // Desired zoom level
        ));
    }

    /**
     * {@inheritDoc}
     * The map is loaded and configured so that it displays a draggable marker for the start location
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ((AppCompatActivity)getActivity()).getSupportActionBar().hide();
        View rootView = inflater.inflate(R.layout.fragment_startlocation, container, false);

        //Obtain the start position from the GPS data from the SensorFusion class
        startPosition = sensorFusion.getGNSSLatitude(false);
        //If not location found zoom the map out
        if(startPosition[0]==0 && startPosition[1]==0){
            zoom = 1f;
        }
        else {
            zoom = 19f;
        }
        // Initialize map fragment
        SupportMapFragment supportMapFragment=(SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.startMap);

        // Asynchronous map which can be configured
        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            /**
             * {@inheritDoc}
             * Controls to allow scrolling, tilting, rotating and a compass view of the
             * map are enabled. A marker is added to the map with the start position and a marker
             * drag listener is generated to detect when the marker has moved to obtain the new
             * location.
             */
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                mMap.getUiSettings().setCompassEnabled(true);
                mMap.getUiSettings().setTiltGesturesEnabled(true);
                mMap.getUiSettings().setRotateGesturesEnabled(true);
                mMap.getUiSettings().setScrollGesturesEnabled(true);



                // Define corners of the library
                LatLng southwestCorner = new LatLng(55.922723, -3.175191);
                LatLng northeastCorner = new LatLng(55.923111, -3.174434);
                LatLng southeastCorner = new LatLng(southwestCorner.latitude, northeastCorner.longitude);
                LatLng northwestCorner = new LatLng(northeastCorner.latitude, southwestCorner.longitude);

                // Create a rectangle to represent the library bounds
                PolylineOptions libraryBoundsOutline = new PolylineOptions()
                        .add(northwestCorner)
                        .add(northeastCorner)
                        .add(southeastCorner)
                        .add(southwestCorner)
                        .add(northwestCorner) // Close the loop
                        .color(Color.parseColor("#FFA500")) // Orange color
                        .width(10); // Width of the polyline

                mMap.addPolyline(libraryBoundsOutline);




                // Add a marker in current GPS location and move the camera
                position = new LatLng(startPosition[0], startPosition[1]);
                currentLocationMarker = mMap.addMarker(new MarkerOptions().position(position).title("Start Position").draggable(true));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, zoom ));

                //Drag listener for the marker to execute when the markers location is changed
                mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener()
                {
                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void onMarkerDragStart(Marker marker){}

                    /**
                     * {@inheritDoc}
                     * Updates the start position of the user.
                     */
                    @Override
                    public void onMarkerDragEnd(Marker marker)
                    {
                        startPosition[0] = (float) marker.getPosition().latitude;
                        startPosition[1] = (float) marker.getPosition().longitude;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void onMarkerDrag(Marker marker){}
                });

                // Initialize polyline
                if (path == null) {
                    path = mMap.addPolyline(new PolylineOptions()
                            .width(10)
                            .color(Color.RED)
                            .addAll(pathPoints) // Add existing points, if any
                            .visible(true) // Ensure it's visible
                            .zIndex(1000)); // Ensure it's drawn above other map elements
                }
            }
        });
        return rootView;
    }

    /**
     * {@inheritDoc}
     * Button onClick listener enabled to detect when to go to next fragment and start PDR recording.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        buttonFloorUp = view.findViewById(R.id.buttonFloorUp);
        buttonFloorDown = view.findViewById(R.id.buttonFloorDown);

        // Add button to begin PDR recording and go to recording fragment.
        this.button = (Button) getView().findViewById(R.id.startLocationDone);
        this.button.setOnClickListener(new View.OnClickListener() {
            /**
             * {@inheritDoc}
             * When button clicked the PDR recording can start and the start position is stored for
             * the {@link CorrectionFragment} to display. The {@link RecordingFragment} is loaded.
             */
            @Override
            public void onClick(View view) {
                // Starts recording data from the sensor fusion
                sensorFusion.startRecording();
                // Set the start location obtained
                sensorFusion.setStartGNSSLatitude(startPosition);


                if (!requestingLocationUpdates) {
                    startLocationTracking();
                    requestingLocationUpdates = true;
                    button.setText("Stop"); // Optional: Change button text to indicate tracking is active
                    if (currentLocationMarker != null) {
                        currentLocationMarker.setDraggable(false);  // Disable marker dragging
                    }
                } else {
                    stopLocationTracking();
                    requestingLocationUpdates = false;
                    button.setText("Start"); // Optional: Change button text to indicate tracking is stopped
                    if (currentLocationMarker != null) {
                        currentLocationMarker.setDraggable(true);  // Enable marker dragging
                    }
                    sensorFusion.stopRecording();
                    NavDirections action = StartLocationFragmentDirections.actionStartLocationFragmentToCorrectionFragment();
                    Navigation.findNavController(view).navigate(action);
                }


                // Navigate to the RecordingFragment
//                NavDirections action = StartLocationFragmentDirections.actionStartLocationFragmentToRecordingFragment();
//                Navigation.findNavController(view).navigate(action);
//


            }
        });


        buttonFloorUp.setOnClickListener(v -> nextFloor());
        buttonFloorDown.setOnClickListener(v -> previousFloor());

    }

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;


    @SuppressLint("MissingPermission")
    private void startLocationTracking() {
        pathPoints.clear(); // Clear the list at the end of tracking

        Log.d("LocationTracking", "Starting location tracking");

        // Check for Location Permissions
//        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
//                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//
//            // Requesting the missing permissions
//            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
//                    LOCATION_PERMISSION_REQUEST_CODE);
//            return;
//        }

        // Request location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }



    private void stopLocationTracking() {
        Log.d("LocationTracking", "Stopping location tracking");
        pathPoints.clear(); // Clear the list at the end of tracking

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
