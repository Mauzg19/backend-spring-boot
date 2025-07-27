package com.Restaurant.service;

import org.springframework.stereotype.Service;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.Classifier;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

@Service
public class PredictionService {

    private Classifier model;
    private Instances structure;

    public PredictionService() throws Exception {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("src/main/resources/weka/modeloAvanzado.model"));
        model = (Classifier) ois.readObject();
        ois.close();

        DataSource source = new DataSource("src/main/resources/weka/estructura.arff");
        structure = source.getStructure();
        structure.setClassIndex(structure.numAttributes() - 1);
    }

    public double predict(double[] features) throws Exception {
        Instance instance = new weka.core.DenseInstance(1.0, features);
        instance.setDataset(structure);
        return model.classifyInstance(instance);
    }
}