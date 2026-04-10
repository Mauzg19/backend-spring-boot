package com.Restaurant.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatbotService {

    private final ChatClient chatClient;

    public ChatbotService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Genera una respuesta a un mensaje del usuario
     * Validando primero que sea una pregunta relevante al dominio del restaurante
     * @param message El mensaje del usuario
     * @return La respuesta del chatbot o un mensaje de rechazo
     */
    public String generateResponse(String message) {
        // Validar que la pregunta sea relevante al dominio
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
            // En caso de error, devolver mensaje amigable
            return "Lo siento, hubo un error al procesar tu pregunta. Por favor, intenta de nuevo.";
        }
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
