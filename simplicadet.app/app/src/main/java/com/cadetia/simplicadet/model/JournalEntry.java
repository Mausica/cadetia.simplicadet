package com.cadetia.simplicadet.model;

public class JournalEntry {
    private String title;
    private String subtitle;
    private String date;
    private String imageUrl;
    private String link;

    // Empty constructor for Firestore
    public JournalEntry() {}

    public JournalEntry(String title, String subtitle, String date, String imageUrl, String link) {
        this.title = title;
        this.subtitle = subtitle;
        this.date = date;
        this.imageUrl = imageUrl;
        this.link = link;
    }

    // Getters and setters
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getDate() { return date; }
    public String getImageUrl() { return imageUrl; }
    public String getLink() {return link;}
}