package org.neshan.travel.model;

public class Place extends BaseModel {

    private String imageAddress;
    private double lat;
    private double lng;
    private String description;
    private String provinceId;

    public String getImageAddress() {
        return imageAddress;
    }

    public Place setImageAddress(String imageAddress) {
        this.imageAddress = imageAddress;
        return this;
    }

    public double getLat() {
        return lat;
    }

    public Place setLat(double lat) {
        this.lat = lat;
        return this;
    }

    public double getLng() {
        return lng;
    }

    public Place setLng(double lng) {
        this.lng = lng;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Place setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getProvinceId() {
        return provinceId;
    }

    public Place setProvinceId(String provinceId) {
        this.provinceId = provinceId;
        return this;
    }
}
