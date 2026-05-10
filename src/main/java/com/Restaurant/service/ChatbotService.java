package com.Restaurant.service;

import com.Restaurant.tools.ReportTools;
import com.Restaurant.tools.RestaurantTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private static final Logger LOG = LoggerFactory.getLogger(ChatbotService.class);
    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    // Track last report for this request only
    private final ThreadLocal<String> lastReportBefore = new ThreadLocal<>();

    public ChatbotService(ChatClient.Builder builder,
                          RestaurantTools restaurantTools,
                          ReportTools reportTools,
                          VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.chatClient = builder
                .defaultSystem(buildSystemPrompt())
                .defaultTools(restaurantTools, reportTools)
                .build();
    }

    public String generateResponse(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Por favor, escribe una pregunta para que pueda ayudarte.";
        }

        try {
            // Snapshot reports dir before LLM call
            lastReportBefore.set(getNewestReportFile());

            String llmResponse = this.chatClient.prompt()
                    .user(message)
                    .call()
                    .content();

            // If a new report was created during this call, show it
            String after = getNewestReportFile();
            String before = lastReportBefore.get();
            if (after != null && !after.equals(before)) {
                llmResponse += "\n\n📥 Descargar reporte: GET /api/reports/" + after;
            }
            return llmResponse;
        } catch (Exception e) {
            LOG.error("Chat error — class={} message={}", e.getClass().getSimpleName(), e.getMessage(), e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : "";
            // Check both the top-level message and the cause chain
            Throwable cause = e;
            while (cause != null) {
                String msg = cause.getMessage() != null ? cause.getMessage() : "";
                if (msg.contains("does not support image") || msg.contains("image input")) {
                    return "Lo siento, este modelo solo procesa texto. Por favor, reformula tu pregunta sin imágenes.";
                }
                cause = cause.getCause();
            }
            if (errorMsg.contains("does not support image") || errorMsg.contains("image input")) {
                return "Lo siento, este modelo solo procesa texto. Por favor, reformula tu pregunta sin imágenes.";
            }
            return "Lo siento, hubo un error al procesar tu pregunta. Por favor, intenta de nuevo.";
        }
    }

    private String buildSystemPrompt() {
        return """
                Eres un Asistente de IA para un restaurante con delivery online.
                Tienes acceso a herramientas para consultar y gestionar datos reales.
                El usuario puede incluir una sección "CONTEXTO RELEVANTE DEL RESTAURANTE" con información
                verificada de la base de conocimiento. Usa ese contexto para responder con precisión.

                HERRAMIENTAS DISPONIBLES:
                - listAllRestaurants(): lista todos los restaurantes de la plataforma
                - getMenu(restaurantId, availableOnly?): consulta el menú real de un restaurante
                - getRestaurantInfo(id): obtiene info de un restaurante (nombre, horario, etc.)
                - listMyOrders(status?): lista órdenes del usuario autenticado
                - createFood(name, price, description, restaurantId, categoryId, vegetarian?, seasonal?): crea un plato (admin/dueño)
                - updateFoodAvailability(id, available): cambia disponibilidad de un plato (admin/dueño)
                - updateFoodPrice(id, price): cambia precio de un plato (admin/dueño)
                - updateOrderStatus(orderId, status): cambia estado de una orden (admin/dueño)
                - deleteFood(id): elimina un plato (solo admin)
                - generateSalesReport(fromDate, toDate, format, restaurantId?): reporte CSV/TXT de ventas (admin/dueño)
                - generateMenuReport(restaurantId?, format): reporte CSV/TXT del menú (admin/dueño)
                - generateUsersReport(format): reporte CSV/TXT de usuarios registrados (solo admin)
                - listAvailableReports(): lista archivos de reportes descargables

                VALORES VÁLIDOS:
                - orderStatus: PENDING, PREPARING, OUT_FOR_DELIVERY, DELIVERED, COMPLETED
                - format de reporte: csv, txt
                - fechas: YYYY-MM-DD

                REGLAS:
                1. Responde SIEMPRE en español.
                2. Si el mensaje incluye "CONTEXTO RELEVANTE DEL RESTAURANTE", usa esa información como fuente primaria.
                   Es información verificada de nuestra base de datos.
                3. Cuando el usuario pregunte por el menú, llama getMenu con el restaurantId que te indique.
                   Si no especifica, usa restaurantId=1.
                4. NUNCA inventes platos ni precios. Siempre usa getMenu o el contexto proporcionado.
                5. Si el usuario pide crear/modificar/eliminar, usa la herramienta correspondiente.
                6. Si una herramienta retorna "DENEGADO", infórmale al usuario su rol y qué permisos necesita.
                7. Respuestas breves y útiles. Tono profesional y amigable.
                8. Cuando un reporte se genera, COPIA textualmente la URL de descarga que te da la herramienta.
                   Ejemplo: "Reporte generado: users_123.csv. Descarga: GET /api/reports/users_123.csv".
                """;
    }

    private String getNewestReportFile() {
        try {
            Path dir = Paths.get("./reports").toAbsolutePath().normalize();
            if (!Files.exists(dir)) return null;
            return Files.list(dir)
                    .filter(Files::isRegularFile)
                    .max((a, b) -> Long.compare(a.toFile().lastModified(), b.toFile().lastModified()))
                    .map(p -> p.getFileName().toString())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
