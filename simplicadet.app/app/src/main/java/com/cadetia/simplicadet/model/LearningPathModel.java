package com.cadetia.simplicadet.model;

import com.cadetia.simplicadet.R; // Make sure R is imported or accessible

public class LearningPathModel {
    private String id;
    private String title;
    private boolean isCompleted;
    private boolean isUnlocked;
    private int iconResource;
    private int type; // 0 = Quiz, 1 = Flashcard
    private int position;

    /**
     * Constructor used by HomeFragment1 (loading from Firestore)
     */
    public LearningPathModel(String id, String title, int iconResource, boolean isCompleted, boolean isUnlocked) {
        this.id = id;
        this.title = title;
        this.iconResource = iconResource;
        this.isCompleted = isCompleted;
        this.isUnlocked = isUnlocked;
        this.position = -1; // Position might not be directly relevant here or set later
        this.type = 0; // Default type, should be set via setType()
    }

    /**
     * Overloaded constructor to support LearningPathHelper.java
     */
    public LearningPathModel(int id, String title, boolean isCompleted, boolean isUnlocked, int position) {
        this.id = String.valueOf(id); // Convert int ID to String
        this.title = title;
        // Use a default icon - Make sure 'ic_quizz_node' exists in your drawables.
        this.iconResource = R.drawable.ic_play;
        this.isCompleted = isCompleted;
        this.isUnlocked = isUnlocked;
        this.position = position;
        this.type = 0; // Assume default type is Quiz for helper-generated nodes
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    public boolean isUnlocked() { return isUnlocked; }
    public void setUnlocked(boolean unlocked) { isUnlocked = unlocked; }
    public int getIconResource() { return iconResource; }
    public void setIconResource(int iconResource) { this.iconResource = iconResource; }
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}