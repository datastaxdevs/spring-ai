package org.springframework.ai.autoconfigure.vectorstore.astra;

import org.junit.jupiter.api.Test;
import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;
import org.springframework.ai.autoconfigure.retry.SpringAiRetryAutoConfiguration;
import org.springframework.ai.autoconfigure.vectorstore.astradb.AstraDBVectorStoreAutoConfiguration;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.HashMap;
import java.util.Map;

/**
 * Testing the Store
 */
public class AstraDbAutoConfigurationIT {

	Map<String, String> astraDBSettings = new HashMap<>();

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AstraDBVectorStoreAutoConfiguration.class,
				SpringAiRetryAutoConfiguration.class, OpenAiAutoConfiguration.class))
		.withPropertyValues("spring.data.astradb.apiEndpoint=http",
				"spring.ai.vectorstore.astradb.token=" + System.getenv("ASTRA_DB_APPLICATION_TOKEN"),
				"spring.ai.openai.api-key=" + System.getenv("OPENAI_API_KEY"));

	@Test
	public void addAndSearch() {
		contextRunner.run(context -> {
			VectorStore vectorStore = context.getBean(VectorStore.class);
		});
	}

}
