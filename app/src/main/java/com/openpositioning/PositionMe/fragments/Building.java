package com.openpositioning.PositionMe.fragments;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import java.util.List;

/**
 * Represents a building with geographical boundaries and resources for indoor maps.
 * This class is designed to accommodate both rectangular and complex polygonal building shapes,
 * supporting indoor maps visualization through specified floor resource IDs.
 */
public class Building {
    private final String name; // The name of the building.
    private LatLngBounds bounds; // Rectangular geographical boundaries of the building.
    private List<LatLng> boundaryPoints; // Boundary points for polygonal buildings.
    private final int[] floorResourceIds; // Resource IDs for each floor's map.

    /**
     * Constructor for a rectangular-shaped building using LatLngBounds.
     * This constructor is suitable for buildings that can be accurately represented by a rectangle.
     *
     * @param name            The name of the building.
     * @param bounds          The geographical boundaries represented by LatLngBounds.
     * @param floorResourceIds An array of resource IDs for the maps of each floor within the building.
     */
    public Building(String name, LatLngBounds bounds, int[] floorResourceIds) {
        this.name = name;
        this.bounds = bounds;
        this.floorResourceIds = floorResourceIds;
        this.boundaryPoints = null; // Polygonal boundaries not used for rectangular buildings.
    }

    /**
     * Constructor for a polygonal-shaped building using a list of boundary points.
     * This constructor is suitable for buildings with complex shapes that cannot be accurately
     * represented by a rectangle.
     *
     * @param name            The name of the building.
     * @param boundaryPoints  A list of LatLng points defining the polygonal boundary of the building.
     * @param floorResourceIds An array of resource IDs for the maps of each floor within the building.
     */
    public Building(String name, List<LatLng> boundaryPoints, int[] floorResourceIds) {
        this.name = name;
        this.bounds = null; // Rectangular bounds not used for polygonal buildings.
        this.boundaryPoints = boundaryPoints;
        this.floorResourceIds = floorResourceIds;
    }

    /**
     * Returns the name of the building.
     *
     * @return The building's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the rectangular geographical boundaries of the building, if available.
     * Note: This method returns null for polygonal-shaped buildings.
     *
     * @return The LatLngBounds representing the rectangular boundaries, or null.
     */
    public LatLngBounds getBounds() {
        return bounds;
    }

    /**
     * Returns the list of boundary points for a polygonal-shaped building, if available.
     * Note: This method returns null for rectangular-shaped buildings.
     *
     * @return A list of LatLng points defining the polygonal boundary, or null.
     */
    public List<LatLng> getBoundaryPoints() {
        return boundaryPoints;
    }

    /**
     * Returns the resource IDs for maps of each floor within the building.
     * These IDs refer to drawable resources representing the layout of each floor.
     *
     * @return An array of resource IDs for the floor maps.
     */
    public int[] getFloorMapResourceIds() {
        return floorResourceIds;
    }

    /**
     * Determines whether the building has a complex polygonal shape.
     *
     * @return true if the building is polygonal, false otherwise.
     */
    public boolean isComplexShape() {
        return boundaryPoints != null && !boundaryPoints.isEmpty();
    }
}

