package com.cadetia.simplicadet.entities;

public class FirestoreDocument {
    private String id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String pdfUrl;
    private String category;
    private int position;

    // Constructor complet
    public FirestoreDocument(String id, String title, String subtitle, String imageUrl,
                             String pdfUrl, String category, int position) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.pdfUrl = pdfUrl;
        this.category = category;
        this.position = position;
    }

    public FirestoreDocument(String id, String title, String subtitle, String imageUrl, String pdfUrl) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.pdfUrl = pdfUrl;
        this.category = "";
        this.position = 0;
    }

    // Getters și Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}