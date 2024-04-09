package org.springframework.ai.vectorstore;

import com.datastax.astra.client.model.Filter;

import static com.datastax.astra.client.model.Filters.and;
import static com.datastax.astra.client.model.Filters.eq;

/**
 * AstraDB filter mapper.
 */
class AstraDBFilterMapper {

	public static Filter mapFilter(org.springframework.ai.vectorstore.filter.Filter.Expression filter) {
		switch (filter.type()) {
			/*
			 * case AND: filter.left(). return and(mapFilter(filter.left()),
			 * mapFilter(filter.right())); case OR: return or(mapFilter(filter.left()),
			 * mapFilter(filter.right())); case NOT: return
			 * not(mapFilter(filter.right())); case EQ: return
			 * eq((org.springframework.ai.vectorstore.filter.Filter.Keyfilter.left(),
			 * filter.right().value()); case NE: return ne(filter.left().key(),
			 * filter.right().value()); case GT: return gt(filter.left().key(),
			 * filter.right().value()); case GTE: return gte(filter.left().key(),
			 * filter.right().value()); case LT: return lt(filter.left().key(),
			 * filter.right().value()); case LTE: return lte(filter.left().key(),
			 * filter.right().value()); case IN: return in(filter.left().key(),
			 * filter.right().values()); case NIN: return nin(filter.left().key(),
			 * filter.right().values());
			 */
			default:
				throw new IllegalArgumentException("Unsupported filter type: " + filter.type());
		}
	}

}
