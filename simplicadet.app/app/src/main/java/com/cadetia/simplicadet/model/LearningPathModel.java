package com.cadetia.simplicadet.model;

public class LearningPathModel {
    private int id;
    private String title;
    private boolean isCompleted;
    private boolean isUnlocked;
    private int position; // Position in the learning path (0, 1, 2, etc.)

    public LearningPathModel() {
        // Empty constructor for Firebase
    }

    public LearningPathModel(int id, String title, boolean isCompleted, boolean isUnlocked, int position) {
        this.id = id;
        this.title = title;
        this.isCompleted = isCompleted;
        this.isUnlocked = isUnlocked;
        this.position = position;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }

    public void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}