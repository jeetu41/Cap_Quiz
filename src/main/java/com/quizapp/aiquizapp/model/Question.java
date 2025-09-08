package com.quizapp.aiquizapp.model;

import lombok.Data;
import java.util.List;

@Data
public class Question {
    private String questionText;
    private List<String> options;
    private int correctAnswerIndex;
    private String subject;
    private String difficulty;
}
