package com.openpositioning.PositionMe.fragments;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.openpositioning.PositionMe.R;

import java.util.Arrays;
import java.util.List;

public class Building {
    private String name;
    private LatLngBounds bounds;
    private List<BitmapDescriptor> floorResourceIds; // Resource IDs for each floor's map

    private Building(String name, LatLngBounds bounds, List<BitmapDescriptor> floorResourceIds) {
        this.name = name;
        this.bounds = bounds;
        this.floorResourceIds = floorResourceIds;
    }
    public static Building createLibrary() {
        return new Building(
                "Library",
                new LatLngBounds(
                        new LatLng(55.922723, -3.175191), // SW corner
                        new LatLng(55.923111, -3.174434)  // NE corner
                ),
                Arrays.asList(
                        BitmapDescriptorFactory.fromResource(R.drawable.library_g),
                        BitmapDescriptorFactory.fromResource(R.drawable.library1),
                        BitmapDescriptorFactory.fromResource(R.drawable.library2),
                        BitmapDescriptorFactory.fromResource(R.drawable.library3)
                        )
        );
    }
}
