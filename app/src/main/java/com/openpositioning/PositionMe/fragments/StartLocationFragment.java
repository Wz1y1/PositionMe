package com.openpositioning.PositionMe.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
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
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass. The startLocation fragment is displayed before the trajectory
 * recording starts. This fragment displays a map in which the user can adjust their location to
 * correct the PDR when it is
 *
 * The extended version include the function of tracking and displaying the user path, and adding
 * the indoor map mechanism.
 *
 * @see HomeFragment the previous fragment in the nav graph.
 * @see RecordingFragment the next fragment in the nav graph.
 * @see SensorFusion the class containing sensors and recording.
 *
 * @author Virginia Cangelosi
 * @author Ziyi Wang
 *
 */
public class StartLocationFragment extends Fragment {

    // UI Components
    private Button button; // Button to navigate to the next fragment and save the location.
    private Button buttonFloorUp, buttonFloorDown; // Buttons for navigating between floors in indoor maps.
    private TextView elevationTextView, latitudeTextView, longitudeTextView, accuracyTextView;

    // Map Components
    private GoogleMap mMap; // Instance of GoogleMap.
    private Marker currentLocationMarker; // Previously used to display the current location on the map. Disabled in the current version.
    private Polyline path; // Polyline to draw the path on the map.
    private GroundOverlay currentOverlay; // Overlay for displaying indoor maps.
    private List<LatLng> pathPoints = new ArrayList<>(); // Points to construct the path polyline.
    private float zoom = 19f; // Zoom level for Google Maps.

    // Location Services
    private FusedLocationProviderClient fusedLocationClient; // Client for location services.
    private LocationRequest locationRequest; // Request configuration for location updates.
    private LocationCallback locationCallback; // Callback to handle location updates.

    // Sensor and Location Data
    private SensorFusion sensorFusion = SensorFusion.getInstance(); // Singleton SensorFusion class instance.
    private LatLng position; // LatLng object to pass location to the map.
    private float[] startPosition = new float[2]; // Start position of the user to be stored.

    // Indoor Map and Navigation
    private boolean isIndoorMapShown = false; // Flag to track if an indoor map is currently displayed.
    private Building currentBuilding = null; // Tracks the current building user is in for indoor navigation.
    private HashMap<Integer, Float[]> floorElevationRanges; // Elevation ranges for automatic floor switching.
    private int floorCount = 0; // Counter for the current floor being displayed.

    // Data Management
    private final List<Building> buildings = new ArrayList<>(); // Holds data for buildings, used in both initialization and indoor map display.

    // Flags
    private boolean requestingLocationUpdates = false; // Flag to track if location updates are being requested.




    /**
     * Public Constructor for the class.
     * Left empty as not required
     */
    public StartLocationFragment() {
        // Required empty public constructor
    }



    /**
     * Initializes the fragment, setting up location tracking and indoor map display functionalities.
     * This method performs several key operations:
     *
     * 1. Initializes the {@link FusedLocationProviderClient} for accessing location services.
     * 2. Defines and initializes building data, including geographic bounds and floor resources, for two buildings:
     *    the library and the Nucleus building. This setup includes the creation of {@link Building} objects with
     *    detailed indoor map information for each floor. (Advanced buildings are also added)
     * 3. Prepares a {@link LocationRequest} with specified criteria for location updates, including priority,
     *    interval, and accuracy settings.
     * 4. Sets up a {@link LocationCallback} to handle incoming location updates. Within this callback, the method:
     *    a. Updates UI elements with the latest location data, including altitude, latitude, longitude, and accuracy.
     *    b. Determines whether the user has entered the bounds of any predefined buildings and, if so,
     *       automatically switches to an indoor map view of the corresponding building and allows for floor switching.
     *    c. Hides the indoor map view if the user exits the bounds of the current building.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Add the library building with all details encapsulated in the constructor.
        buildings.add(new Building(
                "Library", // Name of the building
                new LatLngBounds(
                        new LatLng(55.922773986177425, -3.175203041169922), // SW corner for bounds
                        new LatLng(55.923048323680135, -3.1747086373437168) // NE corner for bounds
                ),
                new int[]{ // Array of resource IDs for the floor maps
                        R.drawable.library_g, // Ground floor
                        R.drawable.library1,  // 1st floor
                        R.drawable.library2,  // 2nd floor
                        R.drawable.library3   // 3rd floor
                }
        ));


        buildings.add(new Building(
                "The Nucleus Building", // Name of the building
                new LatLngBounds(
                        new LatLng(55.92280627904572, -3.1746705061042872), // SW corner for bounds
                        new LatLng(55.923327770298805, -3.1738470679168787) // NE corner for bounds
                ),
                new int[]{ // Array of resource IDs for the floor maps
                        R.drawable.nucleuslg, // Lower ground floor
                        R.drawable.nucleusg,  // Ground floor
                        R.drawable.nucleus1,  // 1st floor
                        R.drawable.nucleus2,  // 2nd floor
                        R.drawable.nucleus3   // 3rd floor
                }
        ));


        // The complex building(shape)
        buildings.add(new Building(
                "Sanderson Building", // Name of the building
                Arrays.asList(
                        new LatLng(55.923146629252464, -3.1726503037233202),
                        new LatLng(55.92338257654171, -3.17194488273541),
                        new LatLng(55.9229151891147, -3.1713440679016003),
                        new LatLng(55.922667216004896, -3.1720870398166237)
                ),
                new int[]{ // Array of resource IDs for the floor maps
                        R.drawable.sanderson1, // Ground floor
                        R.drawable.sanderson2,  // 1st floor
                        R.drawable.sanderson3,  // 2nd floor

                }
        ));

        buildings.add(new Building(
                "Fleeming Jenkin Building", // Name of the building
                Arrays.asList(
                        new LatLng(55.922698437858536, -3.1729796510016453),
                        new LatLng(55.92283069027464, -3.1725960951032763),
                        new LatLng(55.92224005951473, -3.171912131788002),
                        new LatLng(55.922092776142065, -3.172284958850053)
                ),
                new int[]{ // Array of resource IDs for the floor maps
                        R.drawable.fleeming1, // Ground floor
                        R.drawable.fleeming2,  // 1st floor
                }
        ));

        buildings.add(new Building(
                "Hudson Beare Building", // Name of the building
                Arrays.asList(
                        new LatLng(55.92253161882922, -3.171719012758574),
                        new LatLng(55.92273063193936, -3.1711299123674346),
                        new LatLng(55.92242554819539, -3.1707490386886255),
                        new LatLng(55.92233687808487, -3.1710333528486703),
                        new LatLng(55.922475143251596, -3.1711620988761187),
                        new LatLng(55.92236843865402, -3.1715402903531595)
                        ),
                new int[]{ // Array of resource IDs for the floor maps
                        R.drawable.hudson1, // Ground floor
                        R.drawable.hudson2,  // 1st floor
                }
        ));





        // Initialize LocationRequest with high accuracy priority and specific update intervals.
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(500)
                .setMaxUpdateDelayMillis(1000)
                .build();



        // Initialize the LocationCallback
        locationCallback = new LocationCallback() {
            /**
             * Defines a LocationCallback to receive location updates. Each update:
             * - Formats and displays the current location's altitude, latitude, longitude, and accuracy on the UI.
             * - Checks if the user has entered the bounds of any predefined buildings and switches to the corresponding indoor map.
             * - Hides the indoor map and resets UI elements when the user exits the building.
             */
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return; // Early return if no location result is available.
                }
                for (Location location : locationResult.getLocations()) {
                    // Formatting location data for display.
                    String altitudeStr = String.format(Locale.getDefault(), "ALT: %.2f m", sensorFusion.getElevation());
                    String latitudeStr = String.format(Locale.getDefault(), "LAT: %.5f", location.getLatitude());
                    String longitudeStr = String.format(Locale.getDefault(), "LNT.: %.5f", location.getLongitude());
                    String accuracyStr = String.format(Locale.getDefault(), "ACC.: ±%.2f m", location.getAccuracy());

                    // Updating UI elements with the formatted location data.
                    elevationTextView.setText(altitudeStr);
                    latitudeTextView.setText(latitudeStr);
                    longitudeTextView.setText(longitudeStr);
                    accuracyTextView.setText(accuracyStr);

                    // Adding the current location to the path and updating the map.
                    LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());
                    pathPoints.add(newPoint);
                    path.setPoints(pathPoints);

                    // Determining building presence and managing indoor map display.
                    manageIndoorMapDisplay(location);
                }
            }

            /**
             * Manages the display of indoor maps based on the user's current location.
             * Checks against predefined buildings to determine if the user has entered or exited a building
             * and updates the display accordingly.
             *
             * @param location The current location of the user.
             */
            private void manageIndoorMapDisplay(Location location) {
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                boolean isInAnyBuilding = false;

                for (Building building : buildings) {
                    boolean userIsInside = false;

                    // Check if the building is complex-shaped
                    if (building.isComplexShape()) {
                        userIsInside = PolyUtil.containsLocation(userLocation, building.getBoundaryPoints(), false);
                    } else if (building.getBounds() != null) {
                        // For rectangular buildings, continue using LatLngBounds
                        userIsInside = building.getBounds().contains(userLocation);
                    }

                    if (userIsInside) {
                        isInAnyBuilding = true;
                        if (!isIndoorMapShown || (currentBuilding == null || !currentBuilding.equals(building))) {
                            switchToIndoorMap(building);
                            buttonFloorUp.setVisibility(View.VISIBLE);
                            buttonFloorDown.setVisibility(View.VISIBLE);
                            currentBuilding = building;
                            isIndoorMapShown = true;
                        }
                        break; // Exit the loop once a building is found.
                    }
                }

                if (!isInAnyBuilding && isIndoorMapShown) {
                    hideIndoorMap();
                    buttonFloorUp.setVisibility(View.GONE);
                    buttonFloorDown.setVisibility(View.GONE);
                    isIndoorMapShown = false;
                    currentBuilding = null;
                }
            }
        };
    }




    /**
     * Displays the floor map overlay for a specified building and floor.
     * This method decides whether to handle a complex-shaped building or a regular one
     * by checking the building's shape and then applying the appropriate overlay options.
     * It updates the map with the indoor map of the selected floor and updates the UI
     * to show the current building name and floor number.
     *
     * @param building The building for which to display the floor.
     * @param floor    The floor number to display.
     */
    private void showFloor(Building building, int floor) {
        // Remove any existing overlay from the map to ensure only one overlay is displayed at a time.
        if (currentOverlay != null) {
            currentOverlay.remove();
        }

        // Retrieve the floor map as a BitmapDescriptor from the building's resource IDs.
        BitmapDescriptor floorDescriptor = BitmapDescriptorFactory.fromResource(building.getFloorMapResourceIds()[floor]);

        // Check if the building has a complex shape to determine how to display the overlay.
        if (building.isComplexShape()) {
            // Calculate the overlay's center, width, and bearing for complex-shaped buildings.
            LatLng center = calculateCenter(building.getBoundaryPoints());
            float width = building.getName().equals("Sanderson Building") ? calculateWidth(building.getBoundaryPoints()) * 0.8f : calculateWidth(building.getBoundaryPoints());
            float bearing = calculateBearing(building.getName(), building.getBoundaryPoints());

            // Create and set the overlay options for complex-shaped buildings, including position and bearing.
            GroundOverlayOptions options = new GroundOverlayOptions()
                    .image(floorDescriptor)
                    .position(center, width)
                    .bearing(bearing);
            currentOverlay = mMap.addGroundOverlay(options);
        } else {
            // For regular buildings, use LatLngBounds to define the overlay's position.
            GroundOverlayOptions options = new GroundOverlayOptions()
                    .image(floorDescriptor)
                    .positionFromBounds(building.getBounds());
            currentOverlay = mMap.addGroundOverlay(options);
        }

        // Keep track of the current floor being displayed.
        floorCount = floor;

        // Update the UI to reflect the current building name and floor number.
        updateFloorDisplay(building.getName(), floor);
    }


    /**
     * Calculates the bearing between two points of a building to determine the orientation
     * of the ground overlay on the map. This method is used for complex-shaped buildings
     * to align the indoor map overlay correctly according to the building's real-world orientation.
     *
     * @param building The name of the building, used to handle specific cases if necessary.
     * @param boundaryPoints A list of {@link LatLng} points defining the boundary of the building.
     * @return The bearing from the first to the second point in degrees.
     */
    private float calculateBearing(String building, List<LatLng> boundaryPoints) {
        // Ensure there are enough points to calculate a bearing.
        if (boundaryPoints.size() < 2) return 0;

        LatLng point1;
        LatLng point2;

        // Select points based on the building name to handle specific cases.
        if (building.equals("Sanderson Building")) {
            // For the Sanderson Building, use a specific pair of points for bearing calculation.
            point1 = boundaryPoints.get(2);
            point2 = boundaryPoints.get(3);
        } else if (building.equals("Fleeming Jenkin Building")) {
            // Different buildings may require different points for accurate bearing calculation.
            point1 = boundaryPoints.get(0);
            point2 = boundaryPoints.get(1);
        } else {
            // Default case for other buildings.
            point1 = boundaryPoints.get(1);
            point2 = boundaryPoints.get(2);
        }

        // Convert points from degrees to radians.
        double lat1 = Math.toRadians(point1.latitude);
        double long1 = Math.toRadians(point1.longitude);
        double lat2 = Math.toRadians(point2.latitude);
        double long2 = Math.toRadians(point2.longitude);

        // Calculate the bearing using the formula.
        double dLon = long2 - long1;
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double bearing = Math.toDegrees(Math.atan2(y, x));

        // Return the bearing adjusted to the map's orientation.
        return (float) bearing;
    }

    /**
     * Calculates the geographical center point of a complex-shaped building defined by a list of boundary points.
     * This method averages the latitudes and longitudes of all points to find the centroid, which is used as the center
     * for placing ground overlays on the map.
     *
     * @param boundaryPoints A list of {@link LatLng} points defining the boundary of the building.
     * @return A {@link LatLng} object representing the geographical center of the boundary points.
     */
    private LatLng calculateCenter(List<LatLng> boundaryPoints) {
        double centroidX = 0, centroidY = 0;
        for (LatLng point : boundaryPoints) {
            centroidX += point.longitude;
            centroidY += point.latitude;
        }
        int totalPoints = boundaryPoints.size();
        return new LatLng(centroidY / totalPoints, centroidX / totalPoints);
    }

    /**
     * Calculates the maximum width between any two points in a list of boundary points of a complex-shaped building.
     * This method is essential for determining the size of the ground overlay to ensure it covers the entire building area.
     *
     * @param boundaryPoints A list of {@link LatLng} points defining the boundary of the building.
     * @return The maximum width (in meters) between any two points within the boundary points.
     */
    private float calculateWidth(List<LatLng> boundaryPoints) {
        float maxWidth = 0;
        for (int i = 0; i < boundaryPoints.size(); i++) {
            LatLng point1 = boundaryPoints.get(i);
            for (int j = i + 1; j < boundaryPoints.size(); j++) {
                LatLng point2 = boundaryPoints.get(j);
                float[] results = new float[1];
                Location.distanceBetween(point1.latitude, point1.longitude, point2.latitude, point2.longitude, results);
                maxWidth = Math.max(maxWidth, results[0]);
            }
        }
        return maxWidth;
    }


    /**
     * Updates the UI to display the current building name and floor number.
     * @param buildingName The name of the building.
     * @param floor The floor number.
     */
    private void updateFloorDisplay(String buildingName, int floor) {
        TextView buildingNameTextView = getView().findViewById(R.id.textViewBuildingName);
        TextView floorNumberTextView = getView().findViewById(R.id.textViewFloorNumber);

        buildingNameTextView.setText(buildingName);
        buildingNameTextView.setVisibility(View.VISIBLE);
        String floorDisplay = formatFloorNumber(buildingName, floor);
        floorNumberTextView.setText(floorDisplay);
        floorNumberTextView.setVisibility(View.VISIBLE);
    }

    /**
     * Formats the floor number for display. Special case with lower ground is dealing separately.
     * @param buildingName The current building
     * @param floor The floor number.
     * @return A string representation of the floor number.
     */
    private String formatFloorNumber(String buildingName, int floor) {
        if (buildingName.equals("The Nucleus Building")) {
            switch (floor) {
                case 0: return "Lower Ground";
                case 1: return "Ground Floor";
                default: return "Floor" + (floor - 1);
            }
        } else {
            switch (floor) {
                case 0: return "Ground Floor";
                default: return "Floor " + floor;
            }
        }
    }


    /**
     * Advances to the next floor's map overlay if possible.
     * This method checks if there is a next floor available and updates the overlay accordingly.
     */
    private void nextFloor() {
        if (currentBuilding != null && floorCount < currentBuilding.getFloorMapResourceIds().length - 1) {
            showFloor(currentBuilding, ++floorCount);
        }
    }

    /**
     * Reverts to the previous floor's map overlay if possible.
     * This method checks if there is a previous floor available and updates the overlay accordingly.
     */
    private void previousFloor() {
        if (currentBuilding != null && floorCount > 0) {
            showFloor(currentBuilding, --floorCount);
        }
    }

    /**
     * Switches the map view to show an indoor map of the specified building.
     * If an indoor map of another building is currently displayed, it is removed first.
     * The method defaults to showing the ground floor, except for specific buildings like "Nucleus",
     * where it starts from a different floor.
     *
     * @param building The building to display on the map.
     */
    private void switchToIndoorMap(Building building) {
        currentBuilding = building;
        zoomToBuilding(building); // Focus the map on the building

        if (currentOverlay != null) {
            currentOverlay.remove(); // Remove existing overlay if present
        }

        // For the "Nucleus" building, start displaying from the first floor above the lower ground (LG) floor.
        // This adjustment accounts for the building's unique structure, which includes a lower ground floor.
        if (building.getName().equals("The Nucleus Building")) {
            showFloor(building, 1); // The first floor is displayed to account for the lower ground floor.
        } else {
            showFloor(building, 0); // For other buildings, default to showing the ground floor.
        }
    }

    /**
     * Hides the currently displayed indoor map overlay.
     * This method is called when the user exits a building or when switching between buildings.
     * Also it handle to hide the floor and building information
     */
    private void hideIndoorMap() {
        if (currentOverlay != null) {
            currentOverlay.remove(); // Remove the GroundOverlay from the map
            currentOverlay = null; // Clear the reference
        }
        TextView buildingNameTextView = getView().findViewById(R.id.textViewBuildingName);
        TextView floorNumberTextView = getView().findViewById(R.id.textViewFloorNumber);
        buildingNameTextView.setVisibility(View.GONE);
        floorNumberTextView.setVisibility(View.GONE);

    }
    /**
     * Centers the map on the specified building, adjusting the zoom level for optimal viewing.
     *
     * @param building The building to center the map view on.
     */
    private void zoomToBuilding(Building building) {
        // Check if the building has a complex shape
        if (building.isComplexShape()) {
            // Calculate the center of the building's boundary points for complex shapes
            double latSum = 0, lngSum = 0;
            for (LatLng point : building.getBoundaryPoints()) {
                latSum += point.latitude;
                lngSum += point.longitude;
            }
            LatLng center = new LatLng(latSum / building.getBoundaryPoints().size(), lngSum / building.getBoundaryPoints().size());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 18f)); // Adjust zoom level as needed
        } else if (building.getBounds() != null) {
            // Use LatLngBounds for rectangular buildings
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(building.getBounds().getCenter(), 18f)); // Adjust zoom level as needed
        }
    }


    private void updateFloorBasedOnElevation(Building building, float currentElevation) {
        // Initialize the Floor height
        floorElevationRanges = new HashMap<>();
        // Assuming each floor is approximately 3 meters high and the ground floor starts at 0 meters
        floorElevationRanges.put(0, new Float[]{0f, 3f}); // Ground floor
        floorElevationRanges.put(1, new Float[]{3f, 6f}); // First floor
        floorElevationRanges.put(2, new Float[]{6f, 9f}); // Second floor

        currentBuilding = building;
        zoomToBuilding(building);
        for (Map.Entry<Integer, Float[]> entry : floorElevationRanges.entrySet()) {
            if (currentElevation >= entry.getValue()[0] && currentElevation < entry.getValue()[1]) {
                int floorToDisplay = entry.getKey();
                showFloor(currentBuilding, floorToDisplay); // Update the displayed floor map
            }
        }
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

        elevationTextView = rootView.findViewById(R.id.altitudeTextView);
        latitudeTextView = rootView.findViewById(R.id.latitudeTextView);
        longitudeTextView = rootView.findViewById(R.id.longitudeTextView);
        accuracyTextView = rootView.findViewById(R.id.accuracyTextView);




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
            @SuppressLint("MissingPermission")
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                mMap.getUiSettings().setCompassEnabled(true);
                mMap.getUiSettings().setTiltGesturesEnabled(true);
                mMap.getUiSettings().setRotateGesturesEnabled(true);
                mMap.getUiSettings().setScrollGesturesEnabled(true);
                mMap.setMyLocationEnabled(true); // Show the current location on the map
                mMap.getUiSettings().setMyLocationButtonEnabled(true); // Show the "My Location" button




                // Draw Polyline for Each Building
                for (Building building : buildings) {
                    if (building.isComplexShape()) {
                        // Handle complex shapes using a list of LatLng points
                        PolygonOptions polygonOptions = new PolygonOptions()
                                .addAll(building.getBoundaryPoints())
                                .strokeColor(Color.parseColor("#00ffa5"))
                                .strokeWidth(10);
                        mMap.addPolygon(polygonOptions);
                    } else if (building.getBounds() != null) {
                        // Extract corners from the building's bounds
                        LatLng southwestCorner = building.getBounds().southwest;
                        LatLng northeastCorner = building.getBounds().northeast;
                        LatLng southeastCorner = new LatLng(southwestCorner.latitude, northeastCorner.longitude);
                        LatLng northwestCorner = new LatLng(northeastCorner.latitude, southwestCorner.longitude);

                        // Create a rectangle to represent the building bounds
                        PolylineOptions buildingBoundsOutline = new PolylineOptions()
                                .add(northwestCorner)
                                .add(northeastCorner)
                                .add(southeastCorner)
                                .add(southwestCorner)
                                .add(northwestCorner) // Close the loop
                                .color(Color.parseColor("#FFA500"))
                                .width(10); // Example width

                        mMap.addPolyline(buildingBoundsOutline);
                    }
                }



//                The Maker function is removed
//                // Add a marker in current GPS location and move the camera
//                position = new LatLng(startPosition[0], startPosition[1]);
//                currentLocationMarker = mMap.addMarker(new MarkerOptions().position(position).title("Start Position").draggable(true));
//                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, zoom ));
//
//                //Drag listener for the marker to execute when the markers location is changed
//                mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener()
//                {
//                    /**
//                     * {@inheritDoc}
//                     */
//                    @Override
//                    public void onMarkerDragStart(Marker marker){}
//
//                    /**
//                     * {@inheritDoc}
//                     * Updates the start position of the user.
//                     */
//                    @Override
//                    public void onMarkerDragEnd(Marker marker)
//                    {
//                        startPosition[0] = (float) marker.getPosition().latitude;
//                        startPosition[1] = (float) marker.getPosition().longitude;
//                    }
//
//                    /**
//                     * {@inheritDoc}
//                     */
//                    @Override
//                    public void onMarkerDrag(Marker marker){}
//                });

                // Instead of adding a marker and setting a drag listener, listen for location updates to center the map
                fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        // Center the map on the current location
                        LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, zoom));
                    }
                });


                //Draw polyline
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

        Button btnChangeMapType = view.findViewById(R.id.btnChangeMapType);
        btnChangeMapType.setOnClickListener(new View.OnClickListener() {
            private int currentMapTypeIndex = 0;
            private final int[] mapTypes = {GoogleMap.MAP_TYPE_NORMAL, GoogleMap.MAP_TYPE_SATELLITE, GoogleMap.MAP_TYPE_TERRAIN, GoogleMap.MAP_TYPE_HYBRID};

            @Override
            public void onClick(View v) {
                // Cycle through the map types
                currentMapTypeIndex = (currentMapTypeIndex + 1) % mapTypes.length;
                if (mMap != null) {
                    mMap.setMapType(mapTypes[currentMapTypeIndex]);
                }
            }
        });

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
                    button.setText("End");
                    if (currentLocationMarker != null) {
                        currentLocationMarker.setDraggable(false);  // Disable marker dragging
                    }
                } else {
                    stopLocationTracking();
                    requestingLocationUpdates = false;
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



            }
        });


        buttonFloorUp.setOnClickListener(v -> nextFloor());
        buttonFloorDown.setOnClickListener(v -> previousFloor());

    }

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    /**
     * Initiates location tracking by requesting location updates from the FusedLocationProviderClient.
     * Clears any previously stored path points to ensure a fresh start for new tracking.
     * This method requires the necessary location permissions to have been granted;
     */
    @SuppressLint("MissingPermission")
    private void startLocationTracking() {
        pathPoints.clear(); // Clear the list at the end of tracking
        Log.d("LocationTracking", "Starting location tracking");

        // Request location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }


    /**
     * Stops the ongoing location tracking by removing location updates from the FusedLocationProviderClient.
     * Clears the stored path points to prepare for any future tracking sessions.
     * This method ensures that location updates are no longer received, conserving resources
     * when location tracking is not needed.
     */
    private void stopLocationTracking() {
        Log.d("LocationTracking", "Stopping location tracking");
        pathPoints.clear(); // Clear the list at the end of tracking
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
