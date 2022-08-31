package org.neshan.travel.model;

public class Province extends BaseModel {

    private double northEastLat;
    private double northEastLng;
    private double southWestLat;
    private double southWestLng;

    public double getNorthEastLat() {
        return northEastLat;
    }

    public Province setNorthEastLat(double northEastLat) {
        this.northEastLat = northEastLat;
        return this;
    }

    public double getNorthEastLng() {
        return northEastLng;
    }

    public Province setNorthEastLng(double northEastLng) {
        this.northEastLng = northEastLng;
        return this;
    }

    public double getSouthWestLat() {
        return southWestLat;
    }

    public Province setSouthWestLat(double southWestLat) {
        this.southWestLat = southWestLat;
        return this;
    }

    public double getSouthWestLng() {
        return southWestLng;
    }

    public Province setSouthWestLng(double southWestLng) {
        this.southWestLng = southWestLng;
        return this;
    }
}
