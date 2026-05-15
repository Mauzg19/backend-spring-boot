package com.Restaurant.listener;

import com.Restaurant.service.DataWarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DataWarehouseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataWarehouseInitializer.class);

    @Autowired
    private DataWarehouseService dataWarehouseService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeDataWarehouse() {
        logger.info("Aplicación iniciada. Inicializando Data Warehouse...");
        try {
            dataWarehouseService.initializeDataWarehouse();
        } catch (Exception e) {
            logger.error("Error al inicializar Data Warehouse", e);
        }
    }
}
