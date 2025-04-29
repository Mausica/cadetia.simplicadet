package com.cadetia.simplicadet.model;

public class DestinationItem {
    private String imageUrl;
    private String id;

    public DestinationItem() {
        // Required empty constructor for Firestore
    }

    public DestinationItem(String id, String imageUrl) {
        this.id = id;
        this.imageUrl = imageUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}