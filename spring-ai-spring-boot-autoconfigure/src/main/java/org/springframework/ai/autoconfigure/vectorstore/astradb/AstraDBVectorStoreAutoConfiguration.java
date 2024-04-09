package org.springframework.ai.autoconfigure.vectorstore.astradb;

import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.DataAPIOptions;
import com.datastax.astra.client.Database;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.vectorstore.AstraDBVectorStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.net.http.HttpClient;

@AutoConfiguration
@ConditionalOnClass({ DataAPIClient.class, Database.class })
@EnableConfigurationProperties({ AstraDBVectorStoreProperties.class })
public class AstraDBVectorStoreAutoConfiguration {

	/**
	 * Initialization of the client
	 * @param connectionProperties configuration
	 * @return client
	 */
	@Bean
	@ConditionalOnMissingBean
	public DataAPIClient dataAPIClient(AstraDBVectorStoreProperties connectionProperties) {
		DataAPIOptions.DataAPIClientOptionsBuilder builder = DataAPIOptions.builder();
		// Http Configuration
		if (connectionProperties.getHttpClient() != null) {
			AstraDBVectorStoreProperties.HttpClientConfig clientConfig = connectionProperties.getHttpClient();
			if (clientConfig.getHttpRedirect() != null) {
				builder.withHttpRedirect(HttpClient.Redirect.valueOf(clientConfig.getHttpRedirect()));
			}
			if (clientConfig.getHttpVersion() != null) {
				builder.withHtpVersion(HttpClient.Version.valueOf(clientConfig.getHttpVersion()));
			}
			if (clientConfig.getRetries() != null) {
				builder.withHttpRetryCount(clientConfig.getRetries().getCount());
				builder.withHttpRetryDelayMillis(clientConfig.getRetries().getDelaysMillis());
			}
			if (clientConfig.getProxy() != null) {
				builder.withHttpProxy(new DataAPIOptions.HttpProxy(clientConfig.getProxy().getHost(),
						clientConfig.getProxy().getPort()));
			}
			builder.withHttpConnectTimeout(clientConfig.getConnectTimeoutSeconds());
			builder.withHttpRequestTimeout(clientConfig.getReadTimeoutSeconds());
		}
		return new DataAPIClient(connectionProperties.getToken(), builder.build());
	}

	/**
	 * Initialization of the Database (should exists)
	 * @param dataAPIClient client connection to AstraDB
	 * @return database
	 */
	@Bean
	@ConditionalOnMissingBean
	public Database database(DataAPIClient dataAPIClient, AstraDBVectorStoreProperties connectionProperties) {
		return dataAPIClient.getDatabase(connectionProperties.getApiEndpoint(), connectionProperties.getNamespace());
	}

	/**
	 * Initialization of the Store
	 * @param database astra Database initialize above
	 * @param connectionProperties connection properties
	 * @param embeddingClient embedding client
	 * @return our embedding store
	 */
	@Bean
	@ConditionalOnMissingBean
	public AstraDBVectorStore vectorStore(Database database, AstraDBVectorStoreProperties connectionProperties,
			EmbeddingClient embeddingClient) {
		return new AstraDBVectorStore(database, connectionProperties.getCollection(), embeddingClient);
	}

}
