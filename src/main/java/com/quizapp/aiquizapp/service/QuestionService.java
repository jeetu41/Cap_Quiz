package com.quizapp.aiquizapp.service;

import com.quizapp.aiquizapp.model.Question;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class QuestionService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Question parseQuestionJson(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, Question.class);
    }
}
