package com.Restaurant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.web.server.ResponseStatusException;

class DemandModelServiceTest {
    @Test void rejectsReportFromAnotherModelWithTheSameDataset() throws Exception {
        var mapper = new ObjectMapper();
        var resources = new org.springframework.core.io.ClassPathResource("mineria/model.json");
        var model = mapper.readTree(resources.getInputStream());
        var report = mapper.readTree(new org.springframework.core.io.ClassPathResource("mineria/report.json").getInputStream());
        ((com.fasterxml.jackson.databind.node.ObjectNode) report).put("modelId", "demanda-mlp-v1-seed42");
        var reader = org.mockito.Mockito.mock(ObjectMapper.class);
        org.mockito.Mockito.when(reader.readTree(org.mockito.ArgumentMatchers.any(java.io.InputStream.class))).thenReturn(model, report);
        assertThrows(java.io.IOException.class, () -> new DemandModelService(reader));
    }
    @Test void exportedKerasPredictionsMatchSpringForEveryProduct() throws Exception {
        var service = new DemandModelService(new ObjectMapper());
        var report = service.report();
        assertEquals("demanda-mlp-v2-seed42", report.path("modelId").asText());
        assertEquals(32, report.path("architecture").get(1).asInt());
        var golden = service.predict("Hamburguesa", LocalDate.of(2026, 9, 1), 38, 40, 65.71428571428571);
        assertEquals(49.577291514433696, (double) golden.get("units"), 0.0001);
        assertEquals(5, report.path("examples").size());
        for (var example : report.path("examples")) {
            var req = example.path("request");
            var actual = service.predict(req.path("product").asText(), LocalDate.parse(req.path("date").asText()),
                    req.path("lag1").asDouble(), req.path("lag7").asDouble(), req.path("mean7").asDouble());
            assertEquals(example.path("expected").asDouble(), (double) actual.get("units"), 0.0001);
        }
    }
    @Test void rejectsUnseenProductsAndInvalidHistoryButAcceptsZero() throws Exception {
        var service = new DemandModelService(new ObjectMapper());
        var day = LocalDate.of(2026, 9, 21);
        assertThrows(ResponseStatusException.class, () -> service.predict("Desconocido", day, 0, 0, 0));
        assertThrows(ResponseStatusException.class, () -> service.predict("Hamburguesa", day, -1, 0, 0));
        assertThrows(ResponseStatusException.class, () -> service.predict("Hamburguesa", day, Double.NaN, 0, 0));
        assertTrue((double) service.predict("Hamburguesa", day, 0, 0, 0).get("units") >= 0);
    }
}
