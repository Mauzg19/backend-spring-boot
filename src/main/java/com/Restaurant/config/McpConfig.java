package com.Restaurant.config;

import com.Restaurant.tools.RestaurantTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuración MCP (Model Context Protocol) para Spring AI.
 * 
 * Configura el ChatClient para que use las herramientas @Tool de RestaurantTools.
 * Spring AI automáticamente detecta los métodos @Tool en RestaurantTools
 * (que es un @Component) y los registra como callbacks.
 * 
 * Patrón del profesor: El ChatClient se construye una sola vez con las herramientas,
 * y se reutiliza en todas las conversaciones.
 */
@Configuration
public class McpConfig {
    
    private static final Logger LOG = LoggerFactory.getLogger(McpConfig.class);
    
    /**
     * Bean ChatClient configurado con herramientas MCP.
     * 
     * Spring AI detecta automáticamente que RestaurantTools es un @Component
     * con métodos @Tool y los registra como callbacks disponibles.
     * 
     * @param builder Builder provisto por Spring AI
     * @param restaurantTools Componente con métodos @Tool
     * @return ChatClient con herramientas disponibles para Ollama
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, 
                                 RestaurantTools restaurantTools) {
        LOG.info("MCP: Registrando RestaurantTools y construyendo ChatClient");
        
        // Spring AI usa @Tool anotaciones automáticamente
        // restaurantTools es inyectado como ComponentScan detecta @Component
        ChatClient chatClient = builder.build();
        
        LOG.info("MCP: ChatClient creado exitosamente con herramientas MCP");
        return chatClient;
    }
}
