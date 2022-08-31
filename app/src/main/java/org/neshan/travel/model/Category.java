package org.neshan.travel.model;

public class Category extends BaseModel {

    private String imageAddress;

    public String getImageAddress() {
        return imageAddress;
    }

    public Category setImageAddress(String imageAddress) {
        this.imageAddress = imageAddress;
        return this;
    }
}
