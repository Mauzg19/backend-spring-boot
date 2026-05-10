package com.Restaurant.prediccion.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "predicciones")
public class PrediccionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "price", nullable = false)
    private double price;

    @Column(name = "is_vegetarian", nullable = false)
    private boolean isVegetarian;

    @Column(name = "is_seasonal", nullable = false)
    private boolean isSeasonal;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "predicted_category", nullable = false)
    private String predictedCategory;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "archived", nullable = false)
    private boolean archived = false;

    public PrediccionEntity() {}

    public PrediccionEntity(double price, boolean isVegetarian, boolean isSeasonal, int quantity, String predictedCategory, double confidence) {
        this.price = price;
        this.isVegetarian = isVegetarian;
        this.isSeasonal = isSeasonal;
        this.quantity = quantity;
        this.predictedCategory = predictedCategory;
        this.confidence = confidence;
        this.createdAt = LocalDateTime.now();
    }

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public boolean isVegetarian() { return isVegetarian; }
    public void setVegetarian(boolean vegetarian) { isVegetarian = vegetarian; }
    public boolean isSeasonal() { return isSeasonal; }
    public void setSeasonal(boolean seasonal) { isSeasonal = seasonal; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getPredictedCategory() { return predictedCategory; }
    public void setPredictedCategory(String predictedCategory) { this.predictedCategory = predictedCategory; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
}
