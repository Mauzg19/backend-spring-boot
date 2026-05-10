package com.Restaurant.service;

import com.Restaurant.model.Events;
import com.Restaurant.model.Food;
import com.Restaurant.repository.EventRepository;
import com.Restaurant.repository.foodRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataIngestionRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DataIngestionRunner.class);
    private final RagService ragService;
    private final foodRepository foodRepository;
    private final EventRepository eventRepository;

    public DataIngestionRunner(RagService ragService, foodRepository foodRepository,
                               EventRepository eventRepository) {
        this.ragService = ragService;
        this.foodRepository = foodRepository;
        this.eventRepository = eventRepository;
    }

    @PostConstruct
    public void ingestAll() {
        try {
            LOG.info("Iniciando ingestion RAG...");

            // Foods
            List<Food> foods = foodRepository.findAll();
            for (Food f : foods) {
                ragService.ingestFood(f);
            }
            LOG.info("Ingestados {} platos.", foods.size());

            // Events
            List<Events> events = eventRepository.findAll();
            for (Events e : events) {
                ragService.ingestEvent(e);
            }
            LOG.info("Ingestados {} eventos.", events.size());

            // FAQs hardcoded
            ragService.ingestFaq("¿Cómo hago un pedido?",
                    "Selecciona un restaurante, agrega platos al carrito y haz clic en 'Realizar pedido'. Sigue los pasos de pago.");
            ragService.ingestFaq("¿Cuánto tarda la entrega?",
                    "El tiempo estimado de entrega es de 30 a 45 minutos dependiendo de tu ubicación y el restaurante.");
            ragService.ingestFaq("¿Puedo cancelar un pedido?",
                    "Sí, puedes cancelar un pedido mientras esté en estado PENDING. Una vez en PREPARING, contacta a soporte.");
            ragService.ingestFaq("¿Qué métodos de pago aceptan?",
                    "Aceptamos tarjetas de crédito/débito a través de Stripe. El pago se procesa de forma segura.");
            ragService.ingestFaq("¿Cómo contacto al restaurante?",
                    "Cada restaurante tiene su información de contacto (teléfono, email) en la página de detalles del restaurante.");
            ragService.ingestFaq("¿Hay opciones vegetarianas?",
                    "Sí, muchos restaurantes ofrecen opciones vegetarianas. Busca el ícono 'Veg' en el menú.");
            ragService.ingestFaq("¿Puedo pedir de varios restaurantes a la vez?",
                    "No, cada pedido es de un solo restaurante. Puedes hacer pedidos separados a diferentes restaurantes.");
            ragService.ingestFaq("¿Cómo creo una cuenta?",
                    "Haz clic en 'Registrarse', ingresa tu email, nombre y contraseña. Recibirás un email de confirmación.");
            LOG.info("Ingestadas {} FAQs.", 8);

            int total = (int) ragService.count();
            LOG.info("Ingestion RAG completa: ~{} documentos en vector store.", total);

        } catch (Exception e) {
            LOG.warn("Ingestion RAG parcial (DB no disponible?): {}", e.getMessage());
        }
    }
}
