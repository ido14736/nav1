package com.example.nav1;

import com.mapbox.mapboxsdk.geometry.LatLng;

public class MapIcon {
    private long id;
    private LatLng position;
    private String type;
    private String name;
    private String description;
    // add image

    public MapIcon(long id, LatLng position,String type, String name, String description)
    {
        this.id = id;
        this.position = position;
        this.type = type;
        this.name = name;
        this.description = description;
    }

    public long getId() {
        return id;
    }

    public LatLng getPosition() {
        return position;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
