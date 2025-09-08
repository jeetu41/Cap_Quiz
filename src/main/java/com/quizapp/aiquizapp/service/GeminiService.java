package com.quizapp.aiquizapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class GeminiService {

    @Value("${GEMINI_API_KEY}")
    private String apiKey;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache to store recent question texts to prevent duplicates
    private final Set<String> questionCache = ConcurrentHashMap.newKeySet();
    private static final int MAX_CACHE_SIZE = 100;

    public boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public String generateQuizQuestion(String subject, String topic, String difficulty) {
        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                // Validate inputs
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    throw new RuntimeException("GEMINI_API_KEY is not configured");
                }

                String prompt = String.format("""
                    Generate a multiple-choice question about %s - %s with difficulty level: %s.

                    IMPORTANT: Respond with ONLY a valid JSON object in this exact format:
                    {
                        "questionText": "The question text here",
                        "options": ["Option 1", "Option 2", "Option 3", "Option 4"],
                        "correctAnswerIndex": 0,
                        "subject": "%s",
                        "difficulty": "%s"
                    }

                    Make sure the correctAnswerIndex is a number (0-3) corresponding to the correct option.
                    Do not include any text before or after the JSON.
                    """, subject, topic, difficulty, subject, difficulty);

                String response = callGeminiAPI(prompt);

                // Validate the JSON response
                validateQuestionResponse(response);

                // Check for duplicate question
                JsonNode questionJson = objectMapper.readTree(response);
                String questionText = questionJson.get("questionText").asText();

                if (questionCache.contains(questionText)) {
                    log.warn("Duplicate question detected, retrying... (attempt {}/{})", attempt + 1, maxRetries);
                    continue; // Retry
                }

                // Add to cache and manage cache size
                questionCache.add(questionText);
                if (questionCache.size() > MAX_CACHE_SIZE) {
                    // Remove oldest entry (simple approach: clear and rebuild, or use LinkedHashSet)
                    // For simplicity, we'll just clear if it exceeds
                    questionCache.clear();
                    questionCache.add(questionText);
                }

                log.info("Generated unique question successfully for subject: {}, topic: {}, difficulty: {}",
                        subject, topic, difficulty);
                return response;

            } catch (Exception e) {
                log.error("Error generating quiz question (attempt {}/{}): {}", attempt + 1, maxRetries, e.getMessage(), e);
                if (attempt == maxRetries - 1) {
                    // Last attempt, return fallback
                    String fallback = createFallbackQuestion(subject, difficulty);
                    log.info("Returning fallback question after {} failed attempts", maxRetries);
                    return fallback;
                }
            }
        }
        // This should not be reached, but just in case
        return createFallbackQuestion(subject, difficulty);
    }

    private String callGeminiAPI(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, String> part = new HashMap<>();
            part.put("text", prompt);
            content.put("parts", List.of(part));
            requestBody.put("contents", List.of(content));
            
            // Add generation config for better responses
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("topP", 0.8);
            generationConfig.put("maxOutputTokens", 1024);
            requestBody.put("generationConfig", generationConfig);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String url = GEMINI_API_URL + "?key=" + apiKey;
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return extractTextFromResponse(response.getBody());
            } else {
                throw new RuntimeException("Failed to get response from Gemini API: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call Gemini API", e);
        }
    }

    private String extractTextFromResponse(String responseBody) {
        try {
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            JsonNode candidates = jsonResponse.get("candidates");
            
            if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.get("content");
                JsonNode parts = content.get("parts");
                
                if (parts != null && parts.isArray() && parts.size() > 0) {
                    JsonNode firstPart = parts.get(0);
                    String text = firstPart.get("text").asText();
                    
                    // Clean up the response - sometimes AI adds markdown formatting
                    text = text.trim();
                    if (text.startsWith("```json")) {
                        text = text.substring(7);
                    }
                    if (text.endsWith("```")) {
                        text = text.substring(0, text.length() - 3);
                    }
                    text = text.trim();
                    
                    return text;
                }
            }
            
            throw new RuntimeException("Invalid response structure from Gemini API");
            
        } catch (Exception e) {
            log.error("Error extracting text from response: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse Gemini API response", e);
        }
    }

    private void validateQuestionResponse(String jsonResponse) {
        try {
            JsonNode question = objectMapper.readTree(jsonResponse);
            
            // Validate required fields
            if (!question.has("questionText") || 
                !question.has("options") || 
                !question.has("correctAnswerIndex")) {
                throw new RuntimeException("Missing required fields in question JSON");
            }
            
            // Validate options array
            JsonNode options = question.get("options");
            if (!options.isArray() || options.size() != 4) {
                throw new RuntimeException("Options must be an array of 4 items");
            }
            
            // Validate correctAnswerIndex
            int correctIndex = question.get("correctAnswerIndex").asInt();
            if (correctIndex < 0 || correctIndex > 3) {
                throw new RuntimeException("correctAnswerIndex must be between 0 and 3");
            }
            
        } catch (Exception e) {
            log.error("Invalid question JSON: {}", jsonResponse);
            throw new RuntimeException("Generated question JSON is invalid: " + e.getMessage());
        }
    }

    private String createFallbackQuestion(String subject, String difficulty) {
        // Provide a fallback question when AI generation fails
        return String.format("""
            {
                "questionText": "What is a key concept in %s?",
                "options": [
                    "Concept A related to %s",
                    "Concept B related to %s",
                    "Concept C related to %s",
                    "Concept D related to %s"
                ],
                "correctAnswerIndex": 0,
                "subject": "%s",
                "difficulty": "%s",
                "isFallback": true
            }
            """, subject, subject, subject, subject, subject, subject, difficulty);
    }
}