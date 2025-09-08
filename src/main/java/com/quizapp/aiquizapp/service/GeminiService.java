package com.quizapp.aiquizapp.service;

import com.google.cloud.aiplatform.v1beta1.EndpointName;
import com.google.cloud.aiplatform.v1beta1.PredictRequest;
import com.google.cloud.aiplatform.v1beta1.PredictResponse;
import com.google.cloud.aiplatform.v1beta1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1beta1.PredictionServiceSettings;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GeminiService {

    @Value("${GCP_PROJECT_ID:your-project-id}")
    private String projectId;

    @Value("${GCP_LOCATION:us-central1}")
    private String location;

    @Value("${GEMINI_API_ENDPOINT:us-central1-aiplatform.googleapis.com:443}")
    private String apiEndpoint;

    @Value("${GEMINI_MODEL:gemini-pro}")
    private String modelName;

    public String generateQuizQuestion(String subject, String topic, String difficulty) {
        try {
            String prompt = String.format("""
                Generate a multiple-choice question about %s - %s with difficulty level: %s.
                Format the response as a valid JSON object with the following structure:
                {
                    "questionText": "The question text",
                    "options": ["Option 1", "Option 2", "Option 3", "Option 4"],
                    "correctAnswerIndex": 0,
                    "subject": "%s",
                    "difficulty": "%s"
                }
                """, subject, topic, difficulty, subject, difficulty);

            String response = predictTextPrompt(prompt);
            log.info("Generated question: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Error generating quiz question: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate quiz question", e);
        }
    }

    private String predictTextPrompt(String prompt) throws IOException {
        try (PredictionServiceClient predictionServiceClient = PredictionServiceClient.create(
                PredictionServiceSettings.newBuilder()
                        .setEndpoint(apiEndpoint)
                        .build())) {

            EndpointName endpoint = EndpointName.ofProjectLocationPublisherModelName(
                    projectId, location, "google", modelName);

            // Prepare the request content
            Map<String, Object> contentMap = new HashMap<>();
            contentMap.put("role", "user");
            contentMap.put("parts", List.of(Map.of("text", prompt)));

            Map<String, Object> instance = new HashMap<>();
            instance.put("contents", List.of(contentMap));

            // Set generation config
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("topP", 1.0);
            generationConfig.put("maxOutputTokens", 1024);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("generationConfig", generationConfig);

            // Convert to protobuf Value
            Value.Builder instanceValue = Value.newBuilder();
            JsonFormat.parser().merge(JsonFormat.printer().print(Value.newBuilder()
                    .putAllFields(instance).build()), instanceValue);

            List<Value> instances = new ArrayList<>();
            instances.add(instanceValue.build());

            Value parameterValue = Value.newBuilder()
                    .putAllFields(Value.newBuilder().putAllFields(parameters).build().getFieldsMap())
                    .build();

            // Create the prediction request
            PredictRequest predictRequest = PredictRequest.newBuilder()
                    .setEndpoint(endpoint.toString())
                    .addAllInstances(instances)
                    .setParameters(parameterValue)
                    .build();

            // Get the prediction
            PredictResponse predictResponse = predictionServiceClient.predict(predictRequest);
            
            // Extract and return the generated text
            if (predictResponse.getPredictionsCount() > 0) {
                Value prediction = predictResponse.getPredictions(0);
                return prediction.getStructValue().getFieldsOrThrow("content").getStringValue();
            }
            
            throw new RuntimeException("No predictions returned from the model");
        }
    }
}
