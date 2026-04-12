package com.Restaurant.service;

import com.Restaurant.model.Food;
import com.Restaurant.repository.foodRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private final ChatClient chatClient;
    private final foodRepository foodRepository;

    public ChatbotService(ChatClient.Builder chatClientBuilder, foodRepository foodRepository) {
        this.chatClient = chatClientBuilder.build();
        this.foodRepository = foodRepository;
    }

    /**
     * Genera una respuesta a un mensaje del usuario
     * Validando primero que sea una pregunta relevante al dominio del restaurante
     * @param message El mensaje del usuario
     * @return La respuesta del chatbot o un mensaje de rechazo
     */
    public String generateResponse(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Por favor, escribe una pregunta para que pueda ayudarte.";
        }

        String lowerMessage = message.toLowerCase(Locale.ROOT);

        // Respuestas basadas en datos reales del menú cuando se pregunta por platillos o menú.
        if (isMenuQuestion(lowerMessage)) {
            List<Food> availableMenu = foodRepository.findAll().stream()
                    .filter(Food::isAvailable)
                    .collect(Collectors.toList());

            if (!availableMenu.isEmpty()) {
                return buildMenuResponse(availableMenu);
            }

            return "Actualmente no hay platos disponibles registrados en el sistema. Por favor intenta de nuevo más tarde.";
        }

        if (!ChatbotValidator.isRelevantQuestion(message)) {
            return ChatbotValidator.getRejectionMessage();
        }

        String systemPrompt = buildSystemPrompt();
        
        try {
            return this.chatClient.prompt()
                    .system(systemPrompt)
                    .user(message)
                    .call()
                    .content();
        } catch (Exception e) {
            return "Lo siento, hubo un error al procesar tu pregunta. Por favor, intenta de nuevo.";
        }
    }

    private boolean isMenuQuestion(String message) {
        return message.contains("menú") || message.contains("menu") || message.contains("platillo") ||
               message.contains("platos") || message.contains("plato") || message.contains("disponible") ||
               message.contains("carta") || message.contains("qué hay") || message.contains("qué tenemos") ||
               message.contains("comidas") || message.contains("bebidas");
    }

    private String buildMenuResponse(List<Food> menuItems) {
        StringBuilder builder = new StringBuilder();
        builder.append("Estos son los platillos disponibles en nuestro restaurante:\n");

        int count = 0;
        for (Food item : menuItems) {
            if (count >= 12) {
                builder.append("...y más platos disponibles. Pide más detalles si lo deseas.\n");
                break;
            }
            builder.append(String.format("%d. %s", count + 1, item.getName()));
            if (item.getDescription() != null && !item.getDescription().isBlank()) {
                builder.append(String.format(" — %s", item.getDescription()));
            }
            builder.append(String.format(". Precio: $%s\n", item.getPrice()));
            count++;
        }

        builder.append("\nSi quieres más información sobre un platillo específico, pregúntame por su nombre o categoría.");
        return builder.toString();
    }

    /**
     * Construye el prompt del sistema restrictivo para el chatbot
     * @return El prompt del sistema completo
     */
    private String buildSystemPrompt() {
        return """
                Eres un Asistente de IA especializado para un aplicativo web de restaurante.
                
                REGLAS FUNDAMENTALES:
                1. Responde SIEMPRE en español
                2. Solo ayuda con preguntas sobre:
                   - Menú, comida y bebidas del restaurante
                   - Cómo usar la aplicación web (navegación, funcionalidades)
                   - Órdenes, pedidos y pagos
                   - Información del restaurante (horarios, ubicación, contacto)
                   - Perfil de usuario y cuenta
                   - Promociones y eventos disponibles
                
                3. Si el usuario pregunta algo fuera de estos temas:
                   - Rechaza amablemente la pregunta
                   - Redirecciona a un tema relacionado con el restaurante
                   - Ofrece ayuda sobre lo que SÍ puedes hacer
                
                4. Proporciona respuestas breves, claras y útiles
                5. Si el usuario tiene un problema técnico, sugiere contactar al soporte
                6. Mantén un tono amigable y profesional
                7. No inventes información sobre el restaurante o sus servicios
                
                Recuerda: Tu único propósito es ayudar a los usuarios con el restaurante y su aplicación web.
                """;
    }
}

