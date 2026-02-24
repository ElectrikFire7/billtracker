package com.billtracker.service;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public List<Map<String, Object>> parseReceiptImage(byte[] imageBytes, String mimeType) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            String prompt = """
                    Analyze this receipt/bill image and extract all line items.
                    Return ONLY a valid JSON array with no extra text. Each element must have:
                    - "name": the item name (string)
                    - "quantity": the quantity (number)
                    - "unitPrice": the unit price / value of one quantity (number)
                    - "totalPrice": the total price for that row (number)

                    If quantity is not visible, assume 1.
                    If unit price is not visible, set it equal to totalPrice.
                    Return ONLY the JSON array, no markdown, no explanation.
                    """;

            JsonObject inlineData = new JsonObject();
            inlineData.addProperty("mimeType", mimeType);
            inlineData.addProperty("data", base64Image);

            JsonObject imagePart = new JsonObject();
            imagePart.add("inlineData", inlineData);

            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", prompt);

            JsonArray parts = new JsonArray();
            parts.add(textPart);
            parts.add(imagePart);

            JsonObject content = new JsonObject();
            content.add("parts", parts);

            JsonArray contents = new JsonArray();
            contents.add(content);

            JsonObject requestBody = new JsonObject();
            requestBody.add("contents", contents);

            String url = apiUrl + "?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Gemini API error: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("Gemini API error: " + response.statusCode());
            }

            JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
            String text = responseJson.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

            // Clean up markdown code fences if present
            text = text.trim();
            if (text.startsWith("```json")) {
                text = text.substring(7);
            } else if (text.startsWith("```")) {
                text = text.substring(3);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            text = text.trim();

            JsonArray items = JsonParser.parseString(text).getAsJsonArray();
            List<Map<String, Object>> result = new ArrayList<>();

            for (JsonElement el : items) {
                JsonObject obj = el.getAsJsonObject();
                Map<String, Object> item = Map.of(
                        "name", obj.get("name").getAsString(),
                        "quantity", obj.get("quantity").getAsNumber(),
                        "unitPrice", obj.get("unitPrice").getAsNumber(),
                        "totalPrice", obj.get("totalPrice").getAsNumber());
                result.add(item);
            }

            return result;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing receipt image", e);
            throw new RuntimeException("Failed to parse receipt image: " + e.getMessage());
        }
    }
}
