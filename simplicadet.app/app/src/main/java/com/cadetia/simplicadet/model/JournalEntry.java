package com.cadetia.simplicadet.model;

public class JournalEntry {
    private String title;
    private String subtitle;
    private String date;
    private String imageUrl;

    // Empty constructor for Firestore
    public JournalEntry() {}

    public JournalEntry(String title, String subtitle, String date, String imageUrl) {
        this.title = title;
        this.subtitle = subtitle;
        this.date = date;
        this.imageUrl = imageUrl;
    }

    // Getters and setters
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getDate() { return date; }
    public String getImageUrl() { return imageUrl; }
}