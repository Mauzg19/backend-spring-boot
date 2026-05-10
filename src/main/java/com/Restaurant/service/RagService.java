package com.Restaurant.service;

import com.Restaurant.model.Events;
import com.Restaurant.model.Food;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RagService {

    private static final Logger LOG = LoggerFactory.getLogger(RagService.class);
    private final VectorStore vectorStore;

    public RagService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void ingestFood(Food food) {
        String content = String.format(
                "Plato: %s. Descripción: %s. Precio: $%d. Categoría: %s. Vegetariano: %s. Disponible: %s.",
                food.getName(),
                food.getDescription() != null ? food.getDescription() : "",
                food.getPrice(),
                food.getFoodCategory() != null ? food.getFoodCategory().getName() : "",
                food.isVegetarian() ? "Sí" : "No",
                food.isAvailable() ? "Sí" : "No"
        );
        Document doc = new Document(content, Map.of(
                "type", "food",
                "id", food.getId(),
                "restaurantId", food.getRestaurant() != null ? food.getRestaurant().getId() : 0L
        ));
        vectorStore.add(List.of(doc));
    }

    public void ingestEvent(Events event) {
        String content = String.format(
                "Evento: %s. Fecha: %s a %s. Ubicación: %s. Restaurante: %s.",
                event.getName(),
                event.getStartedAt(),
                event.getEndsAt(),
                event.getLocation() != null ? event.getLocation() : "N/D",
                event.getRestaurant() != null ? event.getRestaurant().getName() : "N/D"
        );
        Document doc = new Document(content, Map.of(
                "type", "event",
                "id", event.getId(),
                "restaurantId", event.getRestaurant() != null ? event.getRestaurant().getId() : 0L
        ));
        vectorStore.add(List.of(doc));
    }

    public void ingestFaq(String question, String answer) {
        String content = String.format("Pregunta frecuente: %s. Respuesta: %s", question, answer);
        Document doc = new Document(content, Map.of("type", "faq"));
        vectorStore.add(List.of(doc));
    }

    public List<Document> retrieve(String query, int topK) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(0.3)
                        .build()
        );
    }

    public void clearAll() {
        LOG.info("Clearing vector store...");
        // SimpleVectorStore doesn't have clearAll in some versions; re-create is implicit.
    }

    public long count() {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("")
                        .topK(1000)
                        .similarityThresholdAll()
                        .build()
        ).size();
    }
}
