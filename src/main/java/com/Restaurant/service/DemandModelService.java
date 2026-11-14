package com.Restaurant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Inference for the fixed academic dataset. No database history is implied. */
@Service
public class DemandModelService {
    private final JsonNode model;
    private final JsonNode report;

    public DemandModelService(ObjectMapper mapper) throws IOException {
        try (var input = new ClassPathResource("mineria/model.json").getInputStream()) {
            model = mapper.readTree(input);
        }
        try (var input = new ClassPathResource("mineria/report.json").getInputStream()) {
            report = mapper.readTree(input);
        }
        if (model.path("schemaVersion").asInt() != 1 || model.path("layers").size() != 3
                || !model.path("datasetSha256").equals(report.path("datasetSha256"))) {
            throw new IOException("Modelo de demanda incompatible con su informe");
        }
    }

    public JsonNode report() { return report.deepCopy(); }

    public Map<String, Object> predict(String product, LocalDate date, double lag1, double lag7, double mean7) {
        if (date == null || product == null || !valid(lag1) || !valid(lag7) || !valid(mean7)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revise el producto, fecha e historia de ventas");
        }
        JsonNode example = null;
        for (JsonNode item : model.path("examples")) {
            if (product.equals(item.path("request").path("product").asText())) example = item;
        }
        if (example == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Producto fuera del conjunto entrenado");
        int day = date.getDayOfWeek().getValue();
        double[] values = new double[model.path("features").size()];
        values[0] = example.path("price").asDouble();
        values[1] = day;
        values[2] = lag1; values[3] = lag7; values[4] = mean7;
        values[5] = example.path("vegetarian").asBoolean() ? 1 : 0;
        values[6] = example.path("seasonal").asBoolean() ? 1 : 0;
        values[7] = day >= 6 ? 1 : 0;
        for (int i = 0; i < model.path("products").size(); i++) {
            values[8 + i] = product.equals(model.path("products").get(i).asText()) ? 1 : 0;
        }
        for (int i = 0; i < model.path("mean").size(); i++) {
            values[i] = (values[i] - model.path("mean").get(i).asDouble()) / model.path("scale").get(i).asDouble();
        }
        for (JsonNode layer : model.path("layers")) {
            double[] next = new double[layer.path("bias").size()];
            for (int j = 0; j < next.length; j++) {
                next[j] = layer.path("bias").get(j).asDouble();
                for (int i = 0; i < values.length; i++) next[j] += values[i] * layer.path("kernel").get(i).get(j).asDouble();
                if ("relu".equals(layer.path("activation").asText())) next[j] = Math.max(0, next[j]);
            }
            values = next;
        }
        double units = Math.max(0, values[0] * model.path("targetScale").asDouble() + model.path("targetMean").asDouble());
        return Map.of("product", product, "date", date.toString(), "units", units,
                "roundedUnits", Math.round(units), "modelId", model.path("modelId").asText(),
                "scope", "Escenario académico con historia ingresada; no consulta ventas del restaurante ni estima confianza.");
    }

    private boolean valid(double value) { return Double.isFinite(value) && value >= 0 && value <= 10000; }
}
