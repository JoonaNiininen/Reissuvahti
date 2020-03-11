package com.example.reissuvahti.overpass;

public class OverpassLocation {
    private Double lat;
    private Double lon;
    private OverpassTag tags;

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }
    public void setTags(OverpassTag tags) {
        this.tags = tags;
    }

    public OverpassTag getTags() {
        return tags;
    }
}
