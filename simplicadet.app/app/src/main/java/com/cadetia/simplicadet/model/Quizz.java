package com.cadetia.simplicadet.model;

public class Quizz {
    private final String title;
    private final String imageResourceUrl;
    private final String testId;
    private final String createdBy;
    private boolean hasQuestions;

    public Quizz(String title, String imageResourceUrl, String testId, boolean hasQuestions, String createdBy) {
        this.title = title;
        this.imageResourceUrl = imageResourceUrl;
        this.testId = testId;
        this.hasQuestions = hasQuestions;
        this.createdBy = createdBy;
    }

    public String getTitle() {
        return title;
    }

    public String getImageResourceUrl() {
        return imageResourceUrl;
    }

    public String getTestId() {
        return testId;
    }

    public String getCreatedBy() {return createdBy;}
    public boolean hasQuestions() { return hasQuestions; }

}