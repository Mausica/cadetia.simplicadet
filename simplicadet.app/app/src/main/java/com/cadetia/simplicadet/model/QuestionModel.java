package com.cadetia.simplicadet.model;

import java.util.List;

public class QuestionModel {

    private String question;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String image;
    private String correctAns;
    private String userSelectedAnswer;
    private int points;
    public QuestionModel(String question, String optionA, String optionB, String optionC, String optionD, String correctAns, String image) {
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctAns = correctAns;
        this.image = image;
    }

    public QuestionModel(String question, String image, List<String> options, int correctAnswerIndex, int points) {
        this.question = question;
        if (options != null && options.size() >= 4) {
            this.optionA = options.get(0);
            this.optionB = options.get(1);
            this.optionC = options.get(2);
            this.optionD = options.get(3);
            if (correctAnswerIndex >= 0 && correctAnswerIndex < options.size()) {
                this.correctAns = options.get(correctAnswerIndex);
            } else {
                this.correctAns = "";
            }
        }
        this.image = image;
        this.points = points;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public String getCorrectAnswer() {
        return correctAns;
    }

    public void setCorrectAnswer(String correctAns) {
        this.correctAns = correctAns;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getUserSelectedAnswer() {
        return userSelectedAnswer;
    }

    public void setUserSelectedAnswer(String userSelectedAnswer) {
        this.userSelectedAnswer = userSelectedAnswer;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
