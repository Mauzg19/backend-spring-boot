package com.Restaurant.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    @Bean
    VectorStore vectorStore(ObjectProvider<EmbeddingModel> embeddingModel) {
        var em = embeddingModel.getIfAvailable();
        if (em != null) {
            return SimpleVectorStore.builder(em).build();
        }
        return new NoOpVectorStore();
    }
}
