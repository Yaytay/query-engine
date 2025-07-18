/*
 * Copyright (C) 2022 jtalbut
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
package uk.co.spudsoft.query.exec.sources.sql;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.exec.ArgumentInstance;

/**
 * Abstract class to prepare SQL statements before being passed to Vert.x.
 * <P>
 * The main method called by {@link SourceSqlStreamingInstance} is
 * {@link #prepareSqlStatement(java.lang.String, java.lang.Boolean, com.google.common.collect.ImmutableMap)}.
 *
 * @author jtalbut
 */
public abstract class AbstractSqlPreparer {

  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(AbstractSqlPreparer.class);

  /**
   * Constructor.
   */
  public AbstractSqlPreparer() {
  }

  /**
   * Grouping of a prepared SQL statement and its associated arguments.
   *
   * @param query The modified SQL statement.
   * @param args The arguments for the SQL statement.
   */
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Do not modify args obtained from a QueryAndArgs")
  public record QueryAndArgs(String query, List<Object> args) {
  }
  ;
  
  /**
   * For a multi-valued parameter, return the selected parameter.
   * @param instance The ArgumentInstance that encapsulates the past in values.
   * @param idx The index of the required argument.
   * @return The selected parameter value, or null.
   */
  protected Object generateArg(ArgumentInstance instance, int idx) {
    if (instance == null || idx >= instance.getValues().size()) {
      return null;
    }
    return instance.getValues().get(idx);
  }

  /**
   * If true the SQL driver uses numbers to identify parameters, rather than position.
   * <p>
   * This is more efficient if parameters are repeated, but it is just a question of the protocol that the DBMS uses.
   *
   * @return true if the SQL driver uses numbers to identify parameters, rather than position.
   */
  protected boolean hasNumberedParameters() {
    return true;
  }

  /**
   * Get the string used to quote identifier characters in SQL statements.
   *
   * @return the string used to quote identifier characters in SQL statements.
   */
  protected String getQuoteCharacter() {
    return "\"";
  }

  /**
   * Insert the number into the builder in the appropriate way for the driver.
   *
   * @param builder The StringBuilder container the resulting SQL statement.
   * @param number The number of the parameter to be appended.
   */
  protected abstract void generateParameterNumber(StringBuilder builder, int number);

  /**
   * Prepare SQL statements for passing to the driver - primarily to permit named parameters in the SQL.
   * <P>
   * For all drivers parameters can be referenced by name in the form &quot;:&lt;argname&gt;&quot;.
   * <P>
   * SQL preparation does three things:
   * <UL>
   * <LI>Optionally, double quotes get replaced with the appropriate quote character for the driver.
   * <LI>BIND parameters are evaluated. Looks for a commented section of SQL (/&#42;&#42;/) that begin "BIND" and contains a named
   * parameter. If the parameter does not have a value then the entire section is removed, if the parameter does have a value then
   * the comment characters and everything preceding BIND are removed and the remainder is processed as for named parameters.
   * <LI>Named parameters are evaluated. The parameter reference in the SQL is replaced with a positional reference appropriate
   * for the driver and the argument is added to those returned.
   * </UL>
   * The processing is largely based around a regular expression match and replace into a StringBuilder.
   *
   * @param definitionSql The SQL statement from the pipeline definition. If the definition contains a
   * {@link uk.co.spudsoft.query.defn.SourceSql#queryTemplate} this will be the result of evaluating the template.
   * @param replaceDoubleQuotes When a SQL statement is required to use quote entities (typically tables and columns) but does not
   * know the correct character to use this will replace double quotes with the correct character. This is a blanket replacement,
   * if double quotes are used for other purposes in the SQL statement they will also be replaced.
   * @param argSrc The arguments passed in the pipeline, after having been parsed and had default values set.
   * @return A {@link QueryAndArgs} object representing the corrected SQL and the arguments to pass to the driver.
   */
  public QueryAndArgs prepareSqlStatement(String definitionSql, Boolean replaceDoubleQuotes, ImmutableMap<String, ArgumentInstance> argSrc) {

    if (replaceDoubleQuotes != null && replaceDoubleQuotes && !"\"".equals(getQuoteCharacter()) && definitionSql.contains("\"")) {
      definitionSql = definitionSql.replaceAll("\"", getQuoteCharacter());
    }

    List<Object> args = new ArrayList<>();
    if (Strings.isNullOrEmpty(definitionSql)) {
      return new QueryAndArgs(definitionSql, Collections.<Object>emptyList());
    }
    Map<String, Integer> baseNumberedArgs = hasNumberedParameters() ? new HashMap<>() : null;

    StringBuilder builder = new StringBuilder();

    // Group 1 is naked argument reference
    // Group 2 is a complete BIND expression
    Pattern pattern = Pattern.compile(":(" + Argument.VALID_NAME.pattern() + "+)|/\\*\\s*BIND(([^:]*:" + Argument.VALID_NAME.pattern() + "+)+?[^:]*\\s*)\\*/");
    Matcher matcher = pattern.matcher(definitionSql);
    while (matcher.find()) {
      String varName = matcher.group(1);
      if (varName == null) {
        varName = matcher.group(2);
        processBind(argSrc, matcher, baseNumberedArgs, varName, args, builder);
      } else {
        processParameter(argSrc, matcher, baseNumberedArgs, varName, args, builder);
      }
    }
    matcher.appendTail(builder);

    String sql = builder.toString();
    logger.debug("Running SQL {} with args {}", sql, args);
    return new QueryAndArgs(sql, Collections.unmodifiableList(args));
  }

  /**
   * Add a single valued parameter to the list of arguments.
   *
   * @param argInstance The ArgumentInstance representing the value of the parameter.
   * @param args The list of arguments that will be passed to the SQL driver.
   * @param baseNumberedArgs The map of argument names to argument numbers for drivers that support numbered parameters.
   * @param inParameterIdx The index of the parameter within the passed in arguments.
   * @param inClause If the parameter is multivalued this should be an in-clause in the format of the driver that is to be built
   * up.
   */
  protected void appendSingleValuedParameter(ArgumentInstance argInstance, List<Object> args, Map<String, Integer> baseNumberedArgs, int inParameterIdx, StringBuilder inClause) {
    Integer outParameterIdx = baseNumberedArgs == null || argInstance == null ? null : baseNumberedArgs.get(argInstance.getName());
    logger.trace("appendSingleValuedParameter({}, {} ({}))", argInstance == null ? null : argInstance.getName(), outParameterIdx, baseNumberedArgs);
    if (outParameterIdx == null && baseNumberedArgs != null && argInstance != null) {
      baseNumberedArgs.put(argInstance.getName(), 1 + args.size());
    }
    appendSingleValuedParameter(argInstance, args, outParameterIdx, inParameterIdx, inClause);
  }

  /**
   * Add a single valued parameter to the list of arguments.
   *
   * @param argInstance The ArgumentInstance representing the value of the parameter.
   * @param args The list of arguments that will be passed to the SQL driver.
   * @param outParameterIdx The index of the parameter in the arglist if (and only if) the driver
   * {@link #hasNumberedParameters()}.
   * @param inParameterIdx The index of the parameter within the passed in arguments.
   * @param inClause If the parameter is multivalued this should be an in-clause in the format of the driver that is to be built
   * up.
   */
  protected void appendSingleValuedParameter(ArgumentInstance argInstance, List<Object> args, Integer outParameterIdx, int inParameterIdx, StringBuilder inClause) {
    if (outParameterIdx == null) {
      Object argValue = generateArg(argInstance, inParameterIdx);
      args.add(argValue);
      generateParameterNumber(inClause, args.size());
    } else {
      generateParameterNumber(inClause, outParameterIdx + inParameterIdx);
    }
  }

  /**
   * Add a multi valued parameter to the list of arguments.
   *
   * @param argInstance The ArgumentInstance representing the value of the parameter.
   * @param args The list of arguments that will be passed to the SQL driver.
   * @param baseNumberedArgs The map of argument names to argument numbers for drivers that support numbered parameters.
   * @param inClause If the parameter is multivalued this should be an in-clause in the format of the driver that is to be built
   * up.
   */
  protected void appendMultivaluedParameter(ArgumentInstance argInstance, List<Object> args, Map<String, Integer> baseNumberedArgs, StringBuilder inClause) {
    Integer outParameterIdx = baseNumberedArgs == null ? null : baseNumberedArgs.get(argInstance.getName());
    if (outParameterIdx == null && baseNumberedArgs != null) {
      baseNumberedArgs.put(argInstance.getName(), 1 + args.size());
    }
    boolean started = false;
    for (int i = 0; i < argInstance.getValues().size(); ++i) {
      if (started) {
        inClause.append(", ");
      }
      started = true;

      appendSingleValuedParameter(argInstance, args, outParameterIdx, i, inClause);
    }
  }

  /**
   * Replace a named parameter with the correct driver-specific SQL for that parameter.
   * <p>
   * The parameter may be single valued or multi-valued.
   *
   * @param argSrc Details of the arguments defined by, and passed in to, the pipeline.
   * @param matcher Regex Matcher that contains the parameter reference from the original SQL.
   * @param baseNumberedArgs If the driver uses numbered references to arguments (see {@link #hasNumberedParameters()}) QE can
   * avoid providing the same argument twice. This tracks the numbers assigned to each argument in that case.
   * @param varName The name of the variable found in matcher (matcher covers BIND as well as simple named parameters so the group
   * that contains the variable name is not always the same.
   * @param args The List of arguments being built up to pass in to the driver.
   * @param builder The builder containing the resultant SQL statement.
   */
  protected void processParameter(ImmutableMap<String, ArgumentInstance> argSrc, Matcher matcher, Map<String, Integer> baseNumberedArgs, String varName, List<Object> args, StringBuilder builder) {
    ArgumentInstance argInstance = argSrc.get(varName);
    if (argInstance == null || argInstance.getValues().isEmpty()) {
      logger.warn("Argument \"{}\" not provided", varName);
    }
    StringBuilder inClause = new StringBuilder();
    if (argInstance != null && argInstance.getDefinition().isMultiValued()) {
      appendMultivaluedParameter(argInstance, args, baseNumberedArgs, inClause);
    } else {
      appendSingleValuedParameter(argInstance, args, baseNumberedArgs, 0, inClause);
    }
    matcher.appendReplacement(builder,  Matcher.quoteReplacement(inClause.toString()));
  }

  /**
   * Replace a BIND parameter with the correct driver-specific SQL for that parameter (if the argument is provided).
   * <p>
   * The parameter may be single valued or multi-valued.
   *
   * @param argSrc Details of the arguments defined by, and passed in to, the pipeline.
   * @param sqlMatcher Regex Matcher that contains the parameter reference from the original SQL.
   * @param baseNumberedArgs If the driver uses numbered references to arguments (see {@link #hasNumberedParameters()}) QE can
   * avoid providing the same argument twice. This tracks the numbers assigned to each argument in that case.
   * @param bindExpression The BIND expression from the SQL statement, that must be further parsed
   * @param args The List of arguments being built up to pass in to the driver.
   * @param sqlBuilder The builder containing the resultant SQL statement.
   */
  protected void processBind(ImmutableMap<String, ArgumentInstance> argSrc, Matcher sqlMatcher, Map<String, Integer> baseNumberedArgs, String bindExpression, List<Object> args, StringBuilder sqlBuilder) {
    // Find all the variable names in the bind
    Pattern argPattern = Pattern.compile(":(" + Argument.VALID_NAME.pattern() + ")");
    Matcher argMatcher = argPattern.matcher(bindExpression);

    StringBuilder boundClause = new StringBuilder();

    while (argMatcher.find()) {
      String varName = argMatcher.group(1);

      ArgumentInstance argInstance = argSrc.get(varName);
      if (argInstance == null || argInstance.getValues().isEmpty()) {
        sqlMatcher.appendReplacement(sqlBuilder, "");
        return;
      }

      StringBuilder parameterReplacement = new StringBuilder();
      if (argInstance.getDefinition().isMultiValued()) {
        appendMultivaluedParameter(argInstance, args, baseNumberedArgs, parameterReplacement);
      } else {
        appendSingleValuedParameter(argInstance, args, baseNumberedArgs, 0, parameterReplacement);
      }
      argMatcher.appendReplacement(boundClause, Matcher.quoteReplacement(parameterReplacement.toString()));
    }
    argMatcher.appendTail(boundClause);

    sqlMatcher.appendReplacement(sqlBuilder, Matcher.quoteReplacement(boundClause.toString()));
  }
}
