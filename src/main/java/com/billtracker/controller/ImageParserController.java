package com.billtracker.controller;

import com.billtracker.service.GeminiService;
import com.billtracker.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parse-receipt")
@RequiredArgsConstructor
public class ImageParserController {

    private final GeminiService geminiService;

    @PostMapping
    public ResponseEntity<List<Map<String, Object>>> parseReceipt(
            @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal User user) {
        try {
            String contentType = image.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("Please upload a valid image file");
            }

            byte[] imageBytes = image.getBytes();
            List<Map<String, Object>> items = geminiService.parseReceiptImage(imageBytes, contentType, user);
            return ResponseEntity.ok(items);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse receipt: " + e.getMessage());
        }
    }
}
