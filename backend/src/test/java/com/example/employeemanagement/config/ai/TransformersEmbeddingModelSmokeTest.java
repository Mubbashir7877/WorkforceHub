package com.example.employeemanagement.config.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.transformers.TransformersEmbeddingModel;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the local Hugging Face embedding model (sentence-transformers/
 * all-MiniLM-L6-v2, bundled ONNX weights via spring-ai-transformers) actually
 * loads and produces embeddings, independent of Spring context/AiClientConfig
 * wiring or any database. No network access or API key required.
 */
class TransformersEmbeddingModelSmokeTest {

    @Test
    void embedsTextLocallyWithoutAnyRemoteCall(@TempDir Path cacheDir) throws Exception {
        TransformersEmbeddingModel embeddingModel = new TransformersEmbeddingModel();
        embeddingModel.setResourceCacheDirectory(cacheDir.toString());
        embeddingModel.afterPropertiesSet();

        List<float[]> embeddings = embeddingModel.embed(List.of(
                "Employees accrue fifteen days of annual leave per year.",
                "The company's office is painted blue."));

        assertThat(embeddings).hasSize(2);
        assertThat(embeddings.get(0)).hasSizeGreaterThan(0);
        assertThat(embeddings.get(0)).isNotEqualTo(embeddings.get(1));
    }
}
