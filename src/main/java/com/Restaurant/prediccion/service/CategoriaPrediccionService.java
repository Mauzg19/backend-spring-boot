package com.Restaurant.prediccion.service;

import com.Restaurant.prediccion.model.CategoriaPrediccionRequest;
import com.Restaurant.prediccion.model.CategoriaPrediccionResponse;
import com.Restaurant.prediccion.model.PrediccionEntity;
import com.Restaurant.prediccion.repository.PrediccionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

@Service
public class CategoriaPrediccionService {

    private static final Logger LOGGER = Logger.getLogger(CategoriaPrediccionService.class.getName());
    private Classifier classifier;
    private Instances dataStructure;

    @Autowired
    private PrediccionRepository prediccionRepository;

    public CategoriaPrediccionService() {
        try {
            ClassPathResource modelResource = new ClassPathResource("categoria.model");
            try (InputStream modelInputStream = modelResource.getInputStream()) {
                classifier = (Classifier) weka.core.SerializationHelper.read(modelInputStream);
            }
            LOGGER.info("Modelo categoria.model cargado exitosamente.");

            ClassPathResource arffResource = new ClassPathResource("Prediccion.arff");
            try (InputStream arffInputStream = arffResource.getInputStream()) {
                DataSource source = new DataSource(arffInputStream);
                dataStructure = source.getDataSet();
                // si el índice 1 no es válido, usar el último atributo como clase
                int idxToUse = 1;
                if (dataStructure.numAttributes() <= idxToUse) {
                    idxToUse = dataStructure.numAttributes() - 1;
                }
                dataStructure.setClassIndex(idxToUse);
            }
            LOGGER.info("Estructura de datos Prediccion.arff cargada exitosamente.");

        } catch (Exception e) {
            LOGGER.severe("Error al inicializar CategoriaPrediccionService: " + e.getMessage());
            throw new RuntimeException("No se pudo inicializar el servicio de predicción de categoría", e);
        }
    }

    public CategoriaPrediccionResponse predecirCategoria(CategoriaPrediccionRequest request) throws Exception {
        if (classifier == null || dataStructure == null) {
            throw new IllegalStateException("El servicio de predicción de categoría no está inicializado correctamente.");
        }

        Instance instance = new DenseInstance(dataStructure.numAttributes());
        instance.setDataset(dataStructure);
        // validar que los atributos existen antes de asignar
        if (dataStructure.attribute("food.price") == null) {
            throw new IllegalStateException("Atributo 'food.price' no encontrado en Prediccion.arff");
        }
        instance.setValue(dataStructure.attribute("food.price"), request.price());

        if (dataStructure.attribute("food.is_vegetarian") == null) {
            throw new IllegalStateException("Atributo 'food.is_vegetarian' no encontrado en Prediccion.arff");
        }
        instance.setValue(dataStructure.attribute("food.is_vegetarian"), request.isVegetarian() ? "Yes" : "No");

        if (dataStructure.attribute("food.is_seasonal") == null) {
            throw new IllegalStateException("Atributo 'food.is_seasonal' no encontrado en Prediccion.arff");
        }
        instance.setValue(dataStructure.attribute("food.is_seasonal"), request.isSeasonal() ? "Yes" : "No");

        if (dataStructure.attribute("cart_item.quantity") == null) {
            throw new IllegalStateException("Atributo 'cart_item.quantity' no encontrado en Prediccion.arff");
        }
        instance.setValue(dataStructure.attribute("cart_item.quantity"), request.quantity());

        double pred = classifier.classifyInstance(instance);
        double[] dist = classifier.distributionForInstance(instance);
        double confidence = 0.0;
        int idx = (int) pred;
        if (dist != null && idx >= 0 && idx < dist.length) confidence = dist[idx];

        String predictedCategory = dataStructure.classAttribute().value(idx);

        double confidencePct = Math.round(confidence * 10000.0) / 100.0;

        PrediccionEntity prediccion = new PrediccionEntity(request.price(), request.isVegetarian(), request.isSeasonal(), request.quantity(), predictedCategory, confidencePct);
        prediccionRepository.save(prediccion);
        LOGGER.info("Predicción guardada en la base de datos: " + prediccion);

        return new CategoriaPrediccionResponse(predictedCategory, confidencePct);
    }

    public List<PrediccionEntity> buscarPredicciones(Double price, Boolean isVegetarian, Boolean isSeasonal, Integer quantity, String predictedCategory) {
        return prediccionRepository.findByCriteria(price, isVegetarian, isSeasonal, quantity, predictedCategory);
    }

    public List<PrediccionEntity> obtenerTodasLasPredicciones() {
        return prediccionRepository.findAll();
    }
    
    public PrediccionEntity actualizarEstadoPrediccion(Long id, boolean archived) {
        PrediccionEntity prediccion = prediccionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Predicción no encontrada con ID: " + id));
        prediccion.setArchived(archived);
        return prediccionRepository.save(prediccion);
    }

    public void eliminarPrediccion(Long id) {
        if (!prediccionRepository.existsById(id)) {
            throw new IllegalArgumentException("Predicción no encontrada con ID: " + id);
        }
        prediccionRepository.deleteById(id);
    }
}
