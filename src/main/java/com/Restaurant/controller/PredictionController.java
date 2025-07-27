package com.Restaurant.controller;

import com.Restaurant.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prediction")
public class PredictionController {

    @Autowired
    private PredictionService predictionService;

    @PostMapping
    public double predict(@RequestBody double[] features) throws Exception {
        return predictionService.predict(features);
    }
}