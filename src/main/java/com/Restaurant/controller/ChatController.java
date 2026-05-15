package com.Restaurant.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ChatController - Endpoint MCP para interactuar con Ollama y las herramientas del restaurante.
 * 
 * Patrón del profesor:
 * - El ChatClient es inyectado con todas las herramientas @Tool pre-registradas
 * - Ollama decide cuándo invocar las herramientas basándose en la pregunta del usuario
 * 
 * Flujo:
 * 1. Usuario envía pregunta a /mcp/chat?message=...
 * 2. Ollama analiza si necesita herramientas (getMenu, getRestaurantInfo, etc)
 * 3. Spring AI ejecuta las tools de RestaurantTools si es necesario
 * 4. Ollama procesa los resultados y genera respuesta en lenguaje natural
 */
@RestController
@RequestMapping("/mcp")
public class ChatController {
    
    private static final Logger LOG = LoggerFactory.getLogger(ChatController.class);
    
    private final ChatClient chatClient;
    
    /**
     * Constructor con inyección del ChatClient pre-configurado.
     * 
     * El ChatClient ya tiene todas las herramientas @Tool registradas
     * por McpConfig durante la inicialización de la aplicación.
     * 
     * @param chatClient ChatClient con herramientas MCP disponibles
     */
    public ChatController(ChatClient chatClient) {
        LOG.info("MCP: Inicializando ChatController");
        this.chatClient = chatClient;
        LOG.info("MCP: ChatClient inyectado y listo para usar");
    }
    
    /**
     * Endpoint MCP para consultar sobre el restaurante y sus servicios.
     * 
     * Ollama (llama3.1:8b) puede invocar herramientas automáticamente:
     * - getMenu: "¿Qué tiene el menú?"
     * - getRestaurantInfo: "¿Cuál es el horario?"
     * - listRestaurants: "¿Qué restaurantes hay disponibles?"
     * - getUserOrders: "¿Cuáles son mis órdenes?"
     * - createFood, updateFoodAvailability, updateFoodPrice: Para ADMIN/OWNER
     * 
     * @param message Pregunta del usuario (en lenguaje natural)
     * @return Respuesta generada por Ollama (con invocación de tools si es necesario)
     * 
     * Ejemplo:
     * GET /mcp/ollama?message=¿Qué platos vegetarianos tienes disponibles?
     */
    @GetMapping("/ollama")
    public String getOllama(@RequestParam("message") String message) {
        LOG.info("MCP: Recibida pregunta: {}", message);
        
        try {
            String response = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();
            
            LOG.info("MCP: Respuesta generada exitosamente");
            return response;
            
        } catch (Exception e) {
            LOG.error("MCP: Error al procesar pregunta", e);
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Endpoint alternativo: /mcp/chat (nombre más genérico).
     * Sirve para compatibilidad con diferentes clientes.
     */
    @GetMapping("/chat")
    public String getChat(@RequestParam("message") String message) {
        return getOllama(message);
    }
}


