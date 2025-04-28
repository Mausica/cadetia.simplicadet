package com.cadetia.simplicadet.model;

public class RankModel {
    private String name;
    private String imageUrl;

    public RankModel(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
