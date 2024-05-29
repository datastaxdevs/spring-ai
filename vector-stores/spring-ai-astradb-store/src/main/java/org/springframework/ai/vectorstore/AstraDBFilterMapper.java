package org.springframework.ai.vectorstore;

import com.datastax.astra.client.model.Filter;
import com.datastax.astra.client.model.FilterOperator;
import com.datastax.astra.client.model.Filters;

import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.ai.vectorstore.filter.Filter.Operand;
import org.springframework.ai.vectorstore.filter.Filter.Key;
import org.springframework.ai.vectorstore.filter.Filter.Value;
import org.springframework.ai.vectorstore.filter.Filter.Group;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * AstraDB filter mapper.
 */
class AstraDBFilterMapper {

	/**
	 * Converting the SpringAI Filter into a usable AstraDB Filter.
	 * @param filter Spring AI Filter
	 * @return astraDB filter
	 */
	public static Filter mapFilter(Expression filter) {
		if (filter == null) {
			return null;
		}
		if ((filter.left() != null) && (filter.left() instanceof Group)) {
			throw new UnsupportedOperationException(
					"Filter Group (parenthesis) are not supported by AstraDB use AND/OR instead.");
		}
		if ((filter.right() != null) && (filter.right() instanceof Group)) {
			throw new UnsupportedOperationException(
					"Filter Group (parenthesis) are not supported by AstraDB use AND/OR instead.");
		}
		return switch (filter.type()) {
			case GT -> comparisonFilter(mapOperandAsIdentifier(filter.left()), mapOperandAsValue(filter.right()),
					FilterOperator.GREATER_THAN);
			case GTE -> comparisonFilter(mapOperandAsIdentifier(filter.left()), mapOperandAsValue(filter.right()),
					FilterOperator.GREATER_THAN_OR_EQUALS_TO);
			case LT -> comparisonFilter(mapOperandAsIdentifier(filter.left()), mapOperandAsValue(filter.right()),
					FilterOperator.LESS_THAN);
			case LTE -> comparisonFilter(mapOperandAsIdentifier(filter.left()), mapOperandAsValue(filter.right()),
					FilterOperator.LESS_THAN_OR_EQUALS_TO);
			case EQ -> Filters.eq(mapOperandAsIdentifier(filter.left()), mapOperandAsValue(filter.right()));
			case NE -> new Filter(mapOperandAsIdentifier(filter.left()), FilterOperator.NOT_EQUALS_TO,
					mapOperandAsValue(filter.right()));
			case IN -> Filters.in(mapOperandAsIdentifier(filter.left()),
					((List<?>) mapOperandAsValue(filter.right())).toArray());
			case NIN ->
				Filters.nin(mapOperandAsIdentifier(filter.left()), (Object[]) mapOperandAsValue(filter.right()));
			case AND -> Filters.and(mapFilter(mapOperandAsExpression(filter.left())),
					mapFilter(mapOperandAsExpression(filter.right())));
			case OR -> Filters.or(mapFilter(mapOperandAsExpression(filter.left())),
					mapFilter(mapOperandAsExpression(filter.right())));
			case NOT ->
				// Expected no right part for a NOT expression
				Filters.not(mapFilter(mapOperandAsExpression(filter.left())));
			default -> throw new IllegalArgumentException("Unsupported filter type: " + filter.type());
		};
	}

	private static String mapOperandAsIdentifier(Operand operand) {
		if (operand instanceof Key) {
			return ((Key) operand).key();
		}
		throw new IllegalArgumentException("Unsupported operand type (expecting Key) " + operand.getClass());
	}

	private static Object mapOperandAsValue(Operand operand) {
		if (operand instanceof Value) {
			return ((Value) operand).value();
		}
		throw new IllegalArgumentException("Unsupported operand type (expecting Value) " + operand.getClass());
	}

	private static Expression mapOperandAsExpression(Operand operand) {
		if (operand instanceof Expression) {
			return (Expression) operand;
		}
		throw new IllegalArgumentException("Unsupported operand type (expecting Value) " + operand.getClass());
	}

	/**
	 * Mapping a comparison filter with the given operation.
	 * @param name the name of the field
	 * @param value the value to compare
	 * @param operation the operation to use
	 * @return the filter
	 */
	private static Filter comparisonFilter(String name, Object value, FilterOperator operation) {
		if (value instanceof Number || value instanceof Date || value instanceof Instant || value instanceof Calendar) {
			return new Filter(name, operation, value);
		}
		throw new IllegalArgumentException(
				"Unsupported value type for filter, expecting Number, Date, Instant or Calendar " + value.getClass());
	}

}
