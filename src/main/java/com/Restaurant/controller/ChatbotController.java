package com.Restaurant.controller;

import com.Restaurant.service.ChatbotService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        Map<String, String> result = new HashMap<>();

        if (message == null || message.trim().isEmpty()) {
            result.put("error", "El mensaje no puede estar vacío. Por favor, escribe una pregunta.");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            String response = chatbotService.generateResponse(message);
            result.put("response", response);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", "Disculpa, hubo un error al procesar tu pregunta. Por favor, intenta de nuevo más tarde.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}
