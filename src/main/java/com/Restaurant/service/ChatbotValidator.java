package com.Restaurant.service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class ChatbotValidator {

    // Palabras clave permitidas para dominio del restaurante
    private static final Set<String> ALLOWED_KEYWORDS = new HashSet<>();
    
    static {
        // Menú y comida
        ALLOWED_KEYWORDS.add("menú");
        ALLOWED_KEYWORDS.add("menu");
        ALLOWED_KEYWORDS.add("comida");
        ALLOWED_KEYWORDS.add("plato");
        ALLOWED_KEYWORDS.add("bebida");
        ALLOWED_KEYWORDS.add("ingredientes");
        ALLOWED_KEYWORDS.add("alérgeno");
        ALLOWED_KEYWORDS.add("precio");
        ALLOWED_KEYWORDS.add("receta");
        
        // Órdenes y pedidos
        ALLOWED_KEYWORDS.add("pedido");
        ALLOWED_KEYWORDS.add("orden");
        ALLOWED_KEYWORDS.add("compra");
        ALLOWED_KEYWORDS.add("carrito");
        ALLOWED_KEYWORDS.add("factura");
        ALLOWED_KEYWORDS.add("pago");
        ALLOWED_KEYWORDS.add("entregar");
        ALLOWED_KEYWORDS.add("entrega");
        ALLOWED_KEYWORDS.add("envío");
        
        // Restaurante
        ALLOWED_KEYWORDS.add("restaurante");
        ALLOWED_KEYWORDS.add("local");
        ALLOWED_KEYWORDS.add("sucursal");
        ALLOWED_KEYWORDS.add("horario");
        ALLOWED_KEYWORDS.add("dirección");
        ALLOWED_KEYWORDS.add("ubicación");
        ALLOWED_KEYWORDS.add("teléfono");
        ALLOWED_KEYWORDS.add("contacto");
        
        // Aplicación web
        ALLOWED_KEYWORDS.add("aplicación");
        ALLOWED_KEYWORDS.add("sitio");
        ALLOWED_KEYWORDS.add("web");
        ALLOWED_KEYWORDS.add("página");
        ALLOWED_KEYWORDS.add("navegación");
        ALLOWED_KEYWORDS.add("buscar");
        ALLOWED_KEYWORDS.add("filtro");
        ALLOWED_KEYWORDS.add("categoría");
        ALLOWED_KEYWORDS.add("favorito");
        
        // Perfil y cuenta
        ALLOWED_KEYWORDS.add("perfil");
        ALLOWED_KEYWORDS.add("cuenta");
        ALLOWED_KEYWORDS.add("usuario");
        ALLOWED_KEYWORDS.add("contraseña");
        ALLOWED_KEYWORDS.add("email");
        ALLOWED_KEYWORDS.add("dirección");
        ALLOWED_KEYWORDS.add("dirección");
        ALLOWED_KEYWORDS.add("historial");
        ALLOWED_KEYWORDS.add("mis pedidos");
        ALLOWED_KEYWORDS.add("registr");
        ALLOWED_KEYWORDS.add("iniciar sesión");
        ALLOWED_KEYWORDS.add("login");
        ALLOWED_KEYWORDS.add("logout");
        
        // General del aplicativo
        ALLOWED_KEYWORDS.add("cómo");
        ALLOWED_KEYWORDS.add("ayuda");
        ALLOWED_KEYWORDS.add("error");
        ALLOWED_KEYWORDS.add("problema");
        ALLOWED_KEYWORDS.add("función");
        ALLOWED_KEYWORDS.add("característica");
        ALLOWED_KEYWORDS.add("evento");
        ALLOWED_KEYWORDS.add("promoción");
        ALLOWED_KEYWORDS.add("descuento");
    }

    /**
     * Valida si la pregunta es relevante para el dominio del restaurante
     * @param message El mensaje del usuario
     * @return true si la pregunta es relevante, false en caso contrario
     */
    public static boolean isRelevantQuestion(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        String lowerMessage = message.toLowerCase();
        
        // Preguntas que deben ser rechazadas claramente
        if (isBlockedQuestion(lowerMessage)) {
            return false;
        }

        // Validar que contenga al menos una palabra clave permitida
        for (String keyword : ALLOWED_KEYWORDS) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }

        // Preguntas de ayuda general sobre la aplicación
        if (lowerMessage.contains("ayuda") || lowerMessage.contains("cómo") || 
            lowerMessage.contains("¿qué?") || lowerMessage.contains("¿cómo?")) {
            return true;
        }

        return false;
    }

    /**
     * Detecta si la pregunta debe ser bloqueada
     * @param message El mensaje en minúsculas
     * @return true si debe ser bloqueado
     */
    private static boolean isBlockedQuestion(String message) {
        // Palabras clave que indican preguntas fuera de contexto
        String[] blockedPatterns = {
            "política",
            "religión",
            "deporte",
            "película",
            "juego",
            "música",
            "viaje",
            "medicina",
            "legal",
            "código",
            "programación",
            "hola",
            "adiós",
            "qué tal",
            "cómo estás",
            "quién eres"
        };

        for (String pattern : blockedPatterns) {
            if (message.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Obtiene un mensaje de rechazo amigable
     * @return Mensaje informativo
     */
    public static String getRejectionMessage() {
        return "Lo siento, solo puedo ayudarte con preguntas relacionadas con el restaurante, " +
               "el menú, nuestros servicios y el funcionamiento de la aplicación web. " +
               "¿Hay algo específico sobre el restaurante o la app que pueda ayudarte?";
    }
}
