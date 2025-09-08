package com.quizapp.aiquizapp.controller;

import com.quizapp.aiquizapp.model.Question;
import com.quizapp.aiquizapp.service.GeminiService;
import com.quizapp.aiquizapp.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("")
public class QuizController {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private GeminiService geminiService;

    @GetMapping("/")
    public String redirectToQuiz() {
        return "redirect:/quiz";
    }

    @GetMapping("/quiz")
    public String showQuizPage(Model model) {
        model.addAttribute("subjects", new String[]{"OS", "CN", "DSA", "Cloud", "Communication", "Pseudocode"});
        model.addAttribute("difficulties", new String[]{"Easy", "Medium", "Hard"});
        return "quiz";
    }

    @PostMapping("/generate")
    @ResponseBody
    public ResponseEntity<String> generateQuestion(
            @RequestParam String subject,
            @RequestParam(required = false, defaultValue = "General") String topic,
            @RequestParam(required = false, defaultValue = "Medium") String difficulty) {
        try {
            String questionJson = geminiService.generateQuizQuestion(subject, topic, difficulty);
            return ResponseEntity.ok(questionJson);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error generating question: " + e.getMessage());
        }
    }
}
