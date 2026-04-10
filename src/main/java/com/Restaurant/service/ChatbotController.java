package com.Restaurant.controller;

import com.Restaurant.service.ChatbotService;
import com.Restaurant.service.ChatbotValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") 
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    /**
     * Endpoint para procesar mensajes del chatbot
     * @param request Contiene el mensaje del usuario
     * @return ResponseEntity con la respuesta del chatbot
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        Map<String, String> result = new HashMap<>();
        
        if (message == null || message.trim().isEmpty()) {
            result.put("error", "El mensaje no puede estar vacío. Por favor, escribe una pregunta.");
            return ResponseEntity.badRequest().body(result);
        }
        
        try {
            // Validación inicial de contexto
            if (!ChatbotValidator.isRelevantQuestion(message)) {
                result.put("response", ChatbotValidator.getRejectionMessage());
                return ResponseEntity.ok(result);
            }
            
            String response = chatbotService.generateResponse(message);
            result.put("response", response);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", "Disculpa, hubo un error al procesar tu pregunta. Por favor, intenta de nuevo más tarde.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}
