/*
 * Copyright (C) 2024 jtalbut
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.co.spudsoft.query.exec.procs.query;

import com.google.common.collect.ImmutableMap;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.exec.DataRow;

/**
 * Implementation class for visiting nodes in an RSQL expressions and evaluating the result.
 * 
 * @author jtalbut
 */
public class RsqlEvaluator implements RSQLVisitor<Boolean, DataRow> {

  private static final Logger logger = LoggerFactory.getLogger(RsqlEvaluator.class);

  /**
   * Constructor.
   */
  public RsqlEvaluator() {
  }

  /**
   * Functional interface representing an operation in RSQL (or FIQL).
   * 
   * @author jtalbut
   */
  private interface RsqlOperator {

    /**
     * Carry out the operation.
     * @param <T> The type of the value being operated on.
     * @param field The field being accessed.
     * @param rsqlComparator The comparator being used.
     * @param rowValue The value from the row.
     * @param arguments Arguments to the operator.
     * @return 
     */
    <T> boolean operate(String field, RsqlComparator<T> rsqlComparator, Object rowValue, List<String> arguments);

  }
  
  abstract static class RsqlOperatorMulti implements RsqlOperator {

    abstract <T> boolean compare(RsqlComparator<T> rsqlComparator, T rowValue, Set<T> args);

    @Override
    public <T> boolean operate(String field, RsqlComparator<T> rsqlComparator, Object rowValue, List<String> arguments) {
      T typedRowValue = rsqlComparator.validateType(field, rowValue);
      Set<T> typedArguments = arguments.stream().map(v -> rsqlComparator.parseType(field, v)).collect(Collectors.toSet());
      return compare(rsqlComparator, typedRowValue, typedArguments);
    }

  }

  /**
   * Base class for performing comparisons that are always string comparisons.
   * Note that for non-String field this will use the default "toString" method of the value, it will not use any specific formatting configured for the output.
   */
  abstract static class RsqlOperatorString implements RsqlOperator {

    abstract boolean compare(String rowValue, List<String> args);

    @Override
    public <T> boolean operate(String field, RsqlComparator<T> rsqlComparator, Object rowValue, List<String> arguments) {
      if (rsqlComparator != null) {
        throw new AssertionError("rsqlComparator must be null");
      }
      if (rowValue == null) {
        return compare(null, arguments);
      } else if (rowValue instanceof String stringValue) {
        return compare(stringValue, arguments);
      } else {
        String stringValue = rowValue.toString();
        return compare(stringValue, arguments);
      }
    }

  }

  static class RsqlOperatorIn extends RsqlOperatorMulti {
    @Override
    <T> boolean compare(RsqlComparator<T> rsqlComparator, T rowValue, Set<T> args) {
      return rsqlComparator.in(rowValue, args);
    }
  }
  
  static class RsqlOperatorNotIn extends RsqlOperatorMulti {
    @Override
    <T> boolean compare(RsqlComparator<T> rsqlComparator, T rowValue, Set<T> args) {
      return rsqlComparator.notIn(rowValue, args);
    }
  }

  abstract static class RsqlOperatorSingle implements RsqlOperator {

    abstract <T> boolean compare(RsqlComparator<T> rsqlComparator, T rowValue, T args);

    @Override
    public <T> boolean operate(String field, RsqlComparator<T> rsqlComparator, Object rowValue, List<String> arguments) {
      T typedRowValue = rsqlComparator.validateType(field, rowValue);
      T typedArgument = rsqlComparator.parseType(field, arguments.get(0));
      return compare(rsqlComparator, typedRowValue, typedArgument);
    }

  }

  static class RsqlOperatorEqual extends RsqlOperatorSingle {
    @Override
    <T> boolean compare(RsqlComparator<T> rsqlComparator, T rowValue, T arg) {
      return rsqlComparator.equal(rowValue, arg);
    }
  }
  
  static class RsqlOperatorNotEqual extends RsqlOperatorSingle {
    @Override
    <T> boolean compare(RsqlComparator<T> rsqlComparator, T rowValue, T arg) {
      return rsqlComparator.notEqual(rowValue, arg);
    }
  }

  static class RsqlOperatorGreaterThan extends RsqlOperatorSingle {
    @Override
    <T> boolean compare(RsqlComparator<T> rsqlComparator, T rowValue, T arg) {
      return rsqlComparator.greaterThan(rowValue, arg);
    }
  }
  
  static class RsqlOperatorGreaterThanOrEqual extends RsqlOperatorSingle {
    @Override
    <T> boolean compare(RsqlComparator<T> rsqlComparator, T rowValue, T arg) {
      return rsqlComparator.greaterThanOrEqual(rowValue, arg);
    }
  }

  static class RsqlOperatorLessThan extends RsqlOperatorSingle {
    @Override
    <T> boolean compare(RsqlComparator<T> rsqlComparator, T rowValue, T arg) {
      return rsqlComparator.lessThan(rowValue, arg);
    }
  }
  
  static class RsqlOperatorLessThanOrEqual extends RsqlOperatorSingle {
    @Override
    <T> boolean compare(RsqlComparator<T> rsqlComparator, T rowValue, T arg) {
      return rsqlComparator.lessThanOrEqual(rowValue, arg);
    }
  }
  
  private static final ImmutableMap<String, RsqlOperator> OPERATOR_MAP = ImmutableMap.<String, RsqlOperator>builder()
          .put(RSQLOperators.EQUAL.getSymbol(), new RsqlOperatorEqual())
          .put(RSQLOperators.GREATER_THAN.getSymbol(), new RsqlOperatorGreaterThan())
          .put(RSQLOperators.GREATER_THAN_OR_EQUAL.getSymbol(), new RsqlOperatorGreaterThanOrEqual())
          .put(RSQLOperators.IN.getSymbol(), new RsqlOperatorIn())
          .put(RSQLOperators.LESS_THAN.getSymbol(), new RsqlOperatorLessThan())
          .put(RSQLOperators.LESS_THAN_OR_EQUAL.getSymbol(), new RsqlOperatorLessThanOrEqual())
          .put(RSQLOperators.NOT_EQUAL.getSymbol(), new RsqlOperatorNotEqual())
          .put(RSQLOperators.NOT_IN.getSymbol(), new RsqlOperatorNotIn())
          .put("=~", new RsqlOperatorRegex())
          .build();
  private static final ImmutableMap<DataType, RsqlComparator<?>> COMPARATOR_MAP = ImmutableMap.<DataType, RsqlComparator<?>>builder()
          .put(DataType.Boolean, new RsqlComparatorBoolean())
          .put(DataType.Date, new RsqlComparatorDate())
          .put(DataType.DateTime, new RsqlComparatorDateTime())
          .put(DataType.Double, new RsqlComparatorDouble())
          .put(DataType.Float, new RsqlComparatorFloat())
          .put(DataType.Integer, new RsqlComparatorInteger())
          .put(DataType.Long, new RsqlComparatorLong())
          .put(DataType.String, new RsqlComparatorString())
          .put(DataType.Time, new RsqlComparatorTime())
          .build();

  @Override
  public Boolean visit(AndNode node, DataRow row) {
    for (Node child : node.getChildren()) {
      if (!child.accept(this, row)) {
        return Boolean.FALSE;
      }
    }
    return Boolean.TRUE;
  }

  @Override
  public Boolean visit(OrNode node, DataRow row) {
    for (Node child : node.getChildren()) {
      if (child.accept(this, row)) {
        return Boolean.TRUE;
      }
    }
    return Boolean.FALSE;
  }

  @Override
  public Boolean visit(ComparisonNode node, DataRow row) {
    String selector = node.getSelector();
    DataType type = row.getType(selector);
    if (type == null) {
      logger.warn("The field {} is not present in the row: {}", selector, row.getMap().keySet());
      throw new IllegalArgumentException("The field specified in the RSQL expression does not exist");
    } else {
      Object rowValue = row.get(selector);
      if (rowValue == null) {
        return false;
      }
      ComparisonOperator operator = node.getOperator();

      RsqlOperator rsqlOperator = OPERATOR_MAP.get(operator.getSymbol());
      if (rsqlOperator == null) {
        logger.warn("The operator specified in the RSQL expression ({}) is not handled", operator.getSymbol());
        throw new IllegalArgumentException("The operator specified in the RSQL expression is not handled");
      }
      
      RsqlComparator<?> rsqlComparator;
      if (rsqlOperator instanceof RsqlOperatorString) {
        rsqlComparator = null;
      } else {
        rsqlComparator = COMPARATOR_MAP.get(type);
        if (rsqlComparator == null) {
          logger.warn("The data type {} is not handled", type);
          throw new IllegalStateException("The data type accessed in the RSQL expression is not handled");
        }
      }

      return rsqlOperator.operate(selector, rsqlComparator, rowValue, node.getArguments());
    }
  }
}
