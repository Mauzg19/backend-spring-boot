
package com.Restaurant.service;

import java.util.HashSet;
import java.util.Set;

public class ChatbotValidator {

    // Palabras clave permitidas para dominio del restaurante
    private static final Set<String> ALLOWED_KEYWORDS = new HashSet<>();
    
    // Palabras clave específicas para preguntas frecuentes
    private static final Set<String> FAQ_KEYWORDS = new HashSet<>();
    
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
        ALLOWED_KEYWORDS.add("domicilio");
        
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
        ALLOWED_KEYWORDS.add("vegetariano");
        ALLOWED_KEYWORDS.add("vegano");
        ALLOWED_KEYWORDS.add("recomendación");
        ALLOWED_KEYWORDS.add("mejor");
        ALLOWED_KEYWORDS.add("festivo");
        ALLOWED_KEYWORDS.add("fin de semana");
        
        // FAQ Keywords para preguntas específicas
        FAQ_KEYWORDS.add("abierto");
        FAQ_KEYWORDS.add("abre");
        FAQ_KEYWORDS.add("horario");
        FAQ_KEYWORDS.add("ubicación");
        FAQ_KEYWORDS.add("dónde");
        FAQ_KEYWORDS.add("dirección");
        FAQ_KEYWORDS.add("menú");
        FAQ_KEYWORDS.add("recomendado");
        FAQ_KEYWORDS.add("vegetariano");
        FAQ_KEYWORDS.add("vegano");
        FAQ_KEYWORDS.add("precio");
        FAQ_KEYWORDS.add("costo");
        FAQ_KEYWORDS.add("pedido");
        FAQ_KEYWORDS.add("domicilio");
        FAQ_KEYWORDS.add("envío");
        FAQ_KEYWORDS.add("pago");
        FAQ_KEYWORDS.add("promoción");
        FAQ_KEYWORDS.add("evento");
        FAQ_KEYWORDS.add("registr");
        FAQ_KEYWORDS.add("sesión");
        FAQ_KEYWORDS.add("contraseña");
        FAQ_KEYWORDS.add("olvid");
        FAQ_KEYWORDS.add("historial");
        FAQ_KEYWORDS.add("qué puedes");
        FAQ_KEYWORDS.add("humano");
        FAQ_KEYWORDS.add("recomendar");
        FAQ_KEYWORDS.add("vendido");
    }

    /**
     * Identifica el tipo de pregunta FAQ del usuario
     * @param message El mensaje del usuario
     * @return El tipo de FAQ identificado o null
     */
    public static String identifyFAQType(String message) {
        String lower = message.toLowerCase();
        
        // 1. ¿Abren hoy / los fines de semana / festivos?
        if (lower.contains("abierto") || lower.contains("abre") || 
            (lower.contains("horario") && (lower.contains("hoy") || lower.contains("fin de semana") || lower.contains("festiv")))) {
            return "HORARIOS";
        }
        
        // 2. ¿Dónde están ubicados?
        if (lower.contains("dónde") || (lower.contains("ubicad") && lower.contains("está")) || 
            (lower.contains("dirección") && lower.contains("restaurante"))) {
            return "UBICACION";
        }
        
        // 3. ¿Me puedes mostrar el menú?
        if ((lower.contains("menú") || lower.contains("menu")) && 
            (lower.contains("mostrar") || lower.contains("puedes") || lower.contains("qué hay"))) {
            return "MENU";
        }
        
        // 4. ¿Cuáles son los platos más recomendados?
        if ((lower.contains("plato") || lower.contains("platillo") || lower.contains("comida")) && 
            (lower.contains("recomendad") || lower.contains("mejor") || lower.contains("popular"))) {
            return "RECOMENDADOS";
        }
        
        // 5. ¿Qué platos son vegetarianos / veganos?
        if ((lower.contains("vegetarian") || lower.contains("vegano")) && 
            (lower.contains("plato") || lower.contains("comida"))) {
            return "VEGETARIANO_VEGANO";
        }
        
        // 6. ¿Cuáles son los precios?
        if (lower.contains("precio") || lower.contains("costo") || 
            lower.contains("cuánto cuesta") || lower.contains("cuánto sale")) {
            return "PRECIOS";
        }
        
        // 7. ¿Cómo hago un pedido?
        if ((lower.contains("cómo") || lower.contains("como")) && lower.contains("pedido")) {
            return "COMO_PEDIR";
        }
        
        // 8. ¿Hacen domicilios?
        if (lower.contains("domicilio") || lower.contains("envío") || lower.contains("entrega a domicilio")) {
            return "DOMICILIO";
        }
        
        // 9. ¿Cuál es el costo del domicilio?
        if ((lower.contains("costo") || lower.contains("precio") || lower.contains("cuánto")) && 
            lower.contains("domicilio")) {
            return "COSTO_DOMICILIO";
        }
        
        // 10. ¿Hasta qué hora reciben pedidos?
        if ((lower.contains("hasta") || lower.contains("cierre")) && 
            (lower.contains("pedido") || lower.contains("hora"))) {
            return "HORARIO_CIERRE_PEDIDOS";
        }
        
        // 11. ¿Qué métodos de pago aceptan?
        if ((lower.contains("pago") || lower.contains("método")) && 
            (lower.contains("aceptan") || lower.contains("acepta"))) {
            return "METODOS_PAGO";
        }
        
        // 12. ¿Puedo pagar con tarjeta / transferencia / efectivo?
        if ((lower.contains("tarjeta") || lower.contains("transferencia") || lower.contains("efectivo")) && 
            lower.contains("pago")) {
            return "OPCIONES_PAGO";
        }
        
        // 13. ¿Aceptan pagos en línea?
        if ((lower.contains("pago") || lower.contains("pagar")) && lower.contains("línea")) {
            return "PAGO_ONLINE";
        }
        
        // 14. ¿Tienen promociones?
        if (lower.contains("promoción") || lower.contains("descuento") || 
            lower.contains("oferta") || lower.contains("promo")) {
            return "PROMOCIONES";
        }
        
        // 15. ¿Tienen menú para eventos?
        if (lower.contains("evento") || lower.contains("celebración")) {
            return "EVENTOS";
        }
        
        // 16. ¿Cómo me registro?
        if ((lower.contains("cómo") || lower.contains("como")) && lower.contains("registro")) {
            return "REGISTRO";
        }
        
        // 17. ¿Cómo inicio sesión?
        if ((lower.contains("cómo") || lower.contains("como")) && 
            (lower.contains("sesión") || lower.contains("login") || lower.contains("iniciar"))) {
            return "LOGIN";
        }
        
        // 18. ¿Olvidé mi contraseña?
        if (lower.contains("olvid") && lower.contains("contraseña")) {
            return "RECUPERAR_CONTRASEÑA";
        }
        
        // 19. ¿Dónde veo mis pedidos anteriores?
        if ((lower.contains("pedido") || lower.contains("historial")) && 
            (lower.contains("anterior") || lower.contains("pasado") || lower.contains("historial"))) {
            return "MIS_PEDIDOS";
        }
        
        // 20. ¿Qué puedes hacer?
        if ((lower.contains("qué puedes") || lower.contains("qué podes")) && lower.contains("hacer")) {
            return "CAPACIDADES";
        }
        
        // 21. ¿Eres un humano?
        if (lower.contains("humano") || lower.contains("bot") || lower.contains("inteligencia")) {
            return "SOBRE_CHATBOT";
        }
        
        // 22. ¿Puedes recomendarme algo?
        if (lower.contains("recomendar") || lower.contains("recomendación")) {
            return "RECOMENDACION_PERSONAL";
        }
        
        // 23. ¿Cuál es el plato más vendido?
        if ((lower.contains("más vendido") || lower.contains("popular") || lower.contains("favorito") || lower.contains("mas vendido")) && 
            (lower.contains("plato") || lower.contains("platillo") || lower.contains("comida"))) {
            return "MAS_VENDIDO";
        }
        
        return null;
    }

    /**
     * Valida si el mensaje corresponde a una pregunta relevante del restaurante.
     * @param message El mensaje del usuario
     * @return true si el mensaje es relevante
     */
    public static boolean isRelevantQuestion(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        String lowerMessage = message.toLowerCase();

        if (isBlockedQuestion(lowerMessage)) {
            return false;
        }

        if (identifyFAQType(message) != null) {
            return true;
        }

        for (String keyword : ALLOWED_KEYWORDS) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }

        return lowerMessage.contains("ayuda") || lowerMessage.contains("cómo") || lowerMessage.contains("como") ||
               lowerMessage.contains("¿qué") || lowerMessage.contains("qué") || lowerMessage.contains("¿cómo") ||
               lowerMessage.contains("puedo") || lowerMessage.contains("necesito");
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
            "politica",
            "religión",
            "religion",
            "deporte",
            "película",
            "pelicula",
            "juego",
            "música",
            "musica",
            "viaje",
            "medicina",
            "legal",
            "código",
            "codigo",
            "programación",
            "programacion",
            "hola",
            "adiós",
            "adios",
            "qué tal",
            "que tal",
            "cómo estás",
            "como estas",
            "quién eres",
            "quien eres"
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
