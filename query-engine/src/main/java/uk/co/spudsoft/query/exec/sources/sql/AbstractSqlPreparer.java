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
import java.util.ArrayList;
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
 * The main method called by {@link SourceSqlStreamingInstance} is {@link #prepareSqlStatement(java.lang.String, java.lang.Boolean, com.google.common.collect.ImmutableMap)}.
 * 
 * 
 * @author jtalbut
 */
public abstract class AbstractSqlPreparer {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(AbstractSqlPreparer.class);
  
  public static final class QueryAndArgs {
    public final String query;
    public final List<Object> args;

    public QueryAndArgs(String query, List<Object> args) {
      this.query = query;
      this.args = args;
    }
  }
  
  protected Object generateArg(ArgumentInstance instance, int idx) {
    if (instance == null || idx >= instance.getValues().size()) {
      return null;
    }
    return instance.getValues().get(idx);
  }
  
  boolean hasNumberedParameters() {
    return true;
  }
  
  /**
   * Get the string used to quote identifier characters in SQL statements.
   * @return the string used to quote identifier characters in SQL statements.
   */
  protected String getQuoteCharacter() {
    return "\"";
  }
  
  abstract void generateParameterNumber(StringBuilder builder, int number);
 
  /**
   * Prepare SQL statements for passing to the driver - primarily to permit named parameters in the SQL.
   * <P>
   * For all drivers parameters can be referenced by name in the form &quot;:&lt;argname&gt;&quot;.
   * <P>
   * SQL preparation does three things:
   * <UL>
   * <LI>Optionally, double quotes get replaced with the appropriate quote character for the driver.
   * <LI>BIND parameters are evaluated.
   * Looks for a commented section of SQL (/&#42;&#42;/) that begin "BIND" and contains a named parameter.
   * If the parameter does not have a value then the entire section is removed, if the parameter does have a value then the comment characters and everything preceding BIND are removed and the remainder is processed as for named parameters.
   * <LI>Named parameters are evaluated.
   * The parameter reference in the SQL is replaced with a positional reference appropriate for the driver and the argument is added to those returned.
   * </UL>
   * 
   * @param definitionSql The SQL statement from the pipeline definition.
   * If the definition contains a {@link uk.co.spudsoft.query.defn.SourceSql#queryTemplate} this will be the result of evaluating the template.
   * @param replaceDoubleQuotes When a SQL statement is required to use quote entities (typically tables and columns) but does not know the correct character to use this will replace double quotes with the correct character.
   * This is a blanket replacement, if double quotes are used for other purposes in the SQL statement they will also be replaced.
   * @param argSrc The arguments passed in the pipeline, after having been parsed and had default values set.
   * @return A {@link QueryAndArgs} object representing the corrected SQL and the arguments to pass to the driver.
   */ 
  public QueryAndArgs prepareSqlStatement(String definitionSql, Boolean replaceDoubleQuotes, ImmutableMap<String, ArgumentInstance> argSrc) {
    
    if (replaceDoubleQuotes != null && replaceDoubleQuotes && !"\"".equals(getQuoteCharacter()) && definitionSql.contains("\"")) {
      definitionSql = definitionSql.replaceAll("\"", getQuoteCharacter());
    }
    
    List<Object> args = new ArrayList<>();
    if (Strings.isNullOrEmpty(definitionSql)) {
      return new QueryAndArgs(definitionSql, args);
    }
    Map<String, Integer> baseNumberedArgs = hasNumberedParameters() ? new HashMap<>() : null;

    StringBuilder builder = new StringBuilder();
    
    Pattern pattern = Pattern.compile(":(" + Argument.VALID_NAME.pattern() + ")|/\\*\\s*BIND([^:]*):(" + Argument.VALID_NAME.pattern() + ")([^:]*)\\s*\\*/");
    Matcher matcher = pattern.matcher(definitionSql);
    while (matcher.find()) {
      String varName = matcher.group(1);
      if (varName == null) {
        // If the second part of the regex is matched the varName is in group(3)
        varName = matcher.group(3);
        processBind(argSrc, matcher, baseNumberedArgs, varName, args, builder);
      } else {
        processParameter(argSrc, matcher, baseNumberedArgs, varName, args, builder);
      }
    }
    matcher.appendTail(builder);
    
    String sql = builder.toString();
    logger.debug("Running SQL {} with args {}", sql, args);
    return new QueryAndArgs(sql, args);
  }

  protected void appendSingleValuedParameter(ArgumentInstance argInstance, List<Object> args, Map<String, Integer> baseNumberedArgs, int inParameterIdx, StringBuilder inClause) {
    Integer outParameterIdx = baseNumberedArgs == null || argInstance == null ? null : baseNumberedArgs.get(argInstance.getName());
    logger.trace("appendSingleValuedParameter({}, {} ({}))", argInstance == null ? null : argInstance.getName(), outParameterIdx, baseNumberedArgs);
    if (outParameterIdx == null && baseNumberedArgs != null && argInstance != null) {
      baseNumberedArgs.put(argInstance.getName(), 1 + args.size());
    }
    appendSingleValuedParameter(argInstance, args, outParameterIdx, inParameterIdx, inClause);
  }

  protected void appendSingleValuedParameter(ArgumentInstance argInstance, List<Object> args, Integer outParameterIdx, int inParameterIdx, StringBuilder inClause) {
    if (outParameterIdx == null) {
      Object argValue = generateArg(argInstance, inParameterIdx);
      args.add(argValue);
      generateParameterNumber(inClause, args.size());
    } else {
      generateParameterNumber(inClause, outParameterIdx + inParameterIdx);
    }
  }

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
    matcher.appendReplacement(builder, inClause.toString());
  }

  protected void processBind(ImmutableMap<String, ArgumentInstance> argSrc, Matcher matcher, Map<String, Integer> baseNumberedArgs, String varName, List<Object> args, StringBuilder builder) {
    ArgumentInstance argInstance = argSrc.get(varName);
    if (argInstance == null || argInstance.getValues().isEmpty()) {
      matcher.appendReplacement(builder, "");
    } else {
      StringBuilder boundClause = new StringBuilder();
      boundClause.append(matcher.group(2));
      if (argInstance.getDefinition().isMultiValued()) {
        appendMultivaluedParameter(argInstance, args, baseNumberedArgs, boundClause);
      } else {
        appendSingleValuedParameter(argInstance, args, baseNumberedArgs, 0, boundClause);
      }
      boundClause.append(matcher.group(4));
      matcher.appendReplacement(builder, boundClause.toString());
    }
  }
  
}
