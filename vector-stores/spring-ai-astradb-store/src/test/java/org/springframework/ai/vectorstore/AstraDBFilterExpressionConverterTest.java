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

import com.datastax.astra.client.model.Filters;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.ai.vectorstore.filter.Filter.ExpressionType.AND;
import static org.springframework.ai.vectorstore.filter.Filter.ExpressionType.EQ;
import static org.springframework.ai.vectorstore.filter.Filter.ExpressionType.GTE;
import static org.springframework.ai.vectorstore.filter.Filter.ExpressionType.IN;
import static org.springframework.ai.vectorstore.filter.Filter.ExpressionType.LTE;
import static org.springframework.ai.vectorstore.filter.Filter.ExpressionType.NE;
import static org.springframework.ai.vectorstore.filter.Filter.ExpressionType.NIN;
import static org.springframework.ai.vectorstore.filter.Filter.ExpressionType.OR;

class AstraDBFilterExpressionConverterTest {

	@Test
	public void testDate() {
		Filter.Expression springFilter = new Filter.Expression(EQ, new Filter.Key("activationDate"),
				new Filter.Value(new Date(1704637752148L)));
		com.datastax.astra.client.model.Filter astraVector = AstraDBFilterMapper.mapFilter(springFilter);
		assertThat(astraVector.toJson()).isEqualTo("{\"activationDate\":{\"$date\":1704637752148}}");
		assertThat(astraVector).isEqualTo(Filters.eq("activationDate", new Date(1704637752148L)));
	}

	@Test
	public void testEQ() {
		Filter.Expression springFilter = new Filter.Expression(EQ, new Filter.Key("country"), new Filter.Value("BG"));
		com.datastax.astra.client.model.Filter astraFilter = AstraDBFilterMapper.mapFilter(springFilter);
		assertThat(astraFilter.toJson()).isEqualTo("{\"country\":\"BG\"}");
		assertThat(astraFilter).isEqualTo(Filters.eq("country", "BG"));
	}

	@Test
	public void tesEqAndGte() {
		Filter.Expression springFilter = new Filter.Expression(AND,
				new Filter.Expression(EQ, new Filter.Key("genre"), new Filter.Value("drama")),
				new Filter.Expression(GTE, new Filter.Key("year"), new Filter.Value(2020)));
		com.datastax.astra.client.model.Filter astraFilter = AstraDBFilterMapper.mapFilter(springFilter);
		assertThat(astraFilter.toJson()).isEqualTo("{\"$and\":[{\"genre\":\"drama\"},{\"year\":{\"$gte\":2020}}]}");
		assertThat(astraFilter).isEqualTo(Filters.and(Filters.eq("genre", "drama"), Filters.gte("year", 2020)));
	}

	@Test
	public void testIn() {
		Filter.Expression springFilter = new Filter.Expression(IN, new Filter.Key("genre"),
				new Filter.Value(List.of("comedy", "documentary", "drama")));
		com.datastax.astra.client.model.Filter astraFilter = AstraDBFilterMapper.mapFilter(springFilter);
		assertThat(astraFilter.toJson()).isEqualTo("{\"genre\":{\"$in\":[\"comedy\",\"documentary\",\"drama\"]}}");
		assertThat(astraFilter.toJson()).isEqualTo(Filters.in("genre", "comedy", "documentary", "drama").toJson());
	}

	@Test
	public void testNe() {
		Filter.Expression springFilter = new Filter.Expression(OR,
				new Filter.Expression(GTE, new Filter.Key("year"), new Filter.Value(2020)),
				new Filter.Expression(AND, new Filter.Expression(EQ, new Filter.Key("country"), new Filter.Value("BG")),
						new Filter.Expression(NE, new Filter.Key("city"), new Filter.Value("Sofia"))));
		com.datastax.astra.client.model.Filter astraFilter = AstraDBFilterMapper.mapFilter(springFilter);
		assertThat(astraFilter.toJson()).isEqualTo("{\"$or\":[" + "{\"year\":{\"$gte\":2020}},"
				+ "{\"$and\":[{\"country\":\"BG\"}," + "{\"city\":{\"$ne\":\"Sofia\"}}]}" + "]}");
		assertThat(astraFilter.toJson()).isEqualTo(Filters
			.or(Filters.gte("year", 2020), Filters.and(Filters.eq("country", "BG"), Filters.ne("city", "Sofia")))
			.toJson());
	}

	@Test
	public void testGroup() {
		Filter.Expression springFilter = new Filter.Expression(AND,
				new Filter.Group(new Filter.Expression(OR,
						new Filter.Expression(GTE, new Filter.Key("year"), new Filter.Value(2020)),
						new Filter.Expression(EQ, new Filter.Key("country"), new Filter.Value("BG")))),
				new Filter.Expression(NIN, new Filter.Key("city"), new Filter.Value(List.of("Sofia", "Plovdiv"))));
		assertThatThrownBy(() -> AstraDBFilterMapper.mapFilter(springFilter))
			.isInstanceOf(UnsupportedOperationException.class)
			.hasMessage("Filter Group (parenthesis) are not supported by AstraDB use AND/OR instead.");
	}

	@Test
	public void testBoolean() {
		// seriously ?
		Filter.Expression springFilter = new Filter.Expression(AND,
				new Filter.Expression(AND, new Filter.Expression(EQ, new Filter.Key("isOpen"), new Filter.Value(true)),
						new Filter.Expression(GTE, new Filter.Key("year"), new Filter.Value(2020))),
				new Filter.Expression(IN, new Filter.Key("country"), new Filter.Value(List.of("BG", "NL", "US"))));
		com.datastax.astra.client.model.Filter astraFilter = AstraDBFilterMapper.mapFilter(springFilter);
		assertThat(astraFilter.toJson()).isEqualTo(
				Filters
					.and(Filters.and(Filters.eq("isOpen", true), Filters.gte("year", 2020)),
							Filters.in("country", "BG", "NL", "US"))
					.toJson());
	}

	@Test
	public void testDecimal() {
		Filter.Expression springFilter = new Filter.Expression(AND,
				new Filter.Expression(GTE, new Filter.Key("temperature"), new Filter.Value(-15.6)),
				new Filter.Expression(LTE, new Filter.Key("temperature"), new Filter.Value(20.13)));
		com.datastax.astra.client.model.Filter astraFilter = AstraDBFilterMapper.mapFilter(springFilter);
		assertThat(astraFilter.toJson())
			.isEqualTo(Filters.and(Filters.gte("temperature", -15.6), Filters.lte("temperature", 20.13)).toJson());
	}

}
