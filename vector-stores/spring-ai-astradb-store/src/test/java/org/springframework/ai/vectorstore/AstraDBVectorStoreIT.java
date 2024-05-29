/*
 * Copyright 2023 - 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.ai.vectorstore;

import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.admin.AstraDBAdmin;
import com.datastax.astra.client.admin.AstraDBDatabaseAdmin;
import com.datastax.astra.client.model.CollectionOptions;
import com.datastax.astra.client.model.SimilarityMetric;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "ASTRA_DB_APPLICATION_TOKEN", matches = "Astra.*")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class AstraDBVectorStoreIT {

	static final String TEST_DB = "test_spring_ai";
	static final String TEST_COLLECTION = "test_collection";
	static String astraDBURL;

	private List<Document> documents = List.of(
			new Document("1", getText("classpath:/test/data/spring.ai.txt"), Map.of("meta1", "meta1")),
			new Document("2", getText("classpath:/test/data/time.shelter.txt"), Map.of()),
			new Document("3", getText("classpath:/test/data/great.depression.txt"), Map.of("meta2", "meta2")));

	private String getText(String uri) {
		var resource = new DefaultResourceLoader().getResource(uri);
		try {
			return resource.getContentAsString(StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
	public static class TestApplication {

		@Bean
		public EmbeddingModel embeddingModel() {
			return new OpenAiEmbeddingModel(new OpenAiApi(System.getenv("OPENAI_API_KEY")));
		}

		@Bean
		public AstraDBVectorStore vectorStore(EmbeddingModel embeddingModel) {
			return new AstraDBVectorStore(AstraDBVectorStoreConfig.builder()
				.withToken(System.getenv("ASTRA_DB_APPLICATION_TOKEN"))
				.withApiEndpoint(astraDBURL)
				.withCollectionName(TEST_COLLECTION)
				.withEnableLogging()
				.withInitializeSchema()
				.withEmbed("chunk")
				.withCollectionOptions(CollectionOptions.builder().vector(1535, SimilarityMetric.COSINE).build())
				.build(), embeddingModel);
		}

	}

	@BeforeAll
	static void initializeDB() {
		/*
		 * Token Value is retrieved from environment Variable
		 * 'ASTRA_DB_APPLICATION_TOKEN', it should have Organization Administration
		 * permissions (to create db)
		 */
		DataAPIClient client = new DataAPIClient(System.getenv("ASTRA_DB_APPLICATION_TOKEN"));
		AstraDBAdmin astraDBAdmin = client.getAdmin();

		/*
		 * Will create a Database in Astra with the name 'test_langchain4j' if does not
		 * exist and work with its identifier. The call is blocking and will wait until
		 * the database is ready.
		 */
		AstraDBDatabaseAdmin databaseAdmin = (AstraDBDatabaseAdmin) astraDBAdmin.createDatabase(TEST_DB);
		UUID dbId = UUID.fromString(databaseAdmin.getDatabaseInformations().getId());
		assertThat(dbId).isNotNull();
		astraDBURL = "https://" + dbId.toString() + "-" + AstraDBAdmin.FREE_TIER_CLOUD_REGION
				+ ".apps.astra.datastax.com";
	}

	@Test
	public void shouldInsert() {
		getContextRunner().run(context -> {
			AstraDBVectorStore vectorStore = context.getBean(AstraDBVectorStore.class);
			System.out.println(vectorStore.getAstraDBCollection().estimatedDocumentCount());
		});
	}

	private ApplicationContextRunner getContextRunner() {
		return new ApplicationContextRunner().withUserConfiguration(TestApplication.class);
	}

	@BeforeEach
	void cleanDatabase() {
		getContextRunner().run(context -> {
			context.getBean(AstraDBVectorStore.class).clear();
		});
	}

}
