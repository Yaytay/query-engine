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
package uk.co.spudsoft.query.defn;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import java.util.List;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.procs.subquery.ProcessorLookupInstance;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * Processor that runs an initial query to generate a map of key/value pairs, and then updates fields based on that map.
 * 
 * In most cases this processor is a bad idea, but in a few specific situations it can be a lot quicker to 
 * find lookup values in memory rather than in SQL.
 * Consider using this processor with large datasets that access the same lookup data in multiple fields.
 * 
 * A child pipeline must be defined to generate the map.
 * This pipeline must result in two fields (additional fields will be ignored).
 * 
 * 
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorLookup.Builder.class)
@Schema(description = 
        """
        <p>
        Processor that runs an initial query to generate a map of key/value pairs, and then updates fields based on that map.
        </p><p>
        In most cases this processor is a bad idea, but in a few specific situations it can be a lot quicker to
        find lookup values in memory rather than in SQL.
        Consider using this processor with large datasets that access the same lookup data in multiple fields.
        </p><p>
        A child pipeline must be defined to generate the map.
        This pipeline must result in two fields (additional fields will be ignored).
        </p><p>
        As there can only be a single value field in the initial query, all the field generated by this processor will be of the same type.
        </p><p>
        It is possible to specify an existing field to be the destination for the looked up value, but this cannot result in a change of the
        field's type.
        It is thus not usually possible to replace the key with the value and it us usually necessary to add an additional field.
        If this is undesireable use the Map processor to remove the key field.
        </p>
        """
)
public class ProcessorLookup implements Processor {

  private final ProcessorType type;
  private final Condition condition;
  private final String id;
    
  private final String lookupKeyField;
  private final String lookupValueField;
  
  private final ImmutableList<ProcessorLookupField> lookupFields;
  
  private final SourcePipeline map;
  
  @Override
  public ProcessorLookupInstance createInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context) {
    return new ProcessorLookupInstance(vertx, sourceNameTracker, context, this);
  }

  @Override
  public void validate() {
    validateType(ProcessorType.LOOKUP, type);
    if (map == null) {
      throw new IllegalArgumentException("Lookup source (lookupSource) pipeline not provided");
    }
    map.validate();
    if (lookupFields == null || lookupFields.isEmpty()) {
      throw new IllegalArgumentException("No fields provided to lookup (lookupFields)");
    }
    for (ProcessorLookupField lookupField : lookupFields) {
      lookupField.validate();
    }
    if (lookupKeyField == null || lookupKeyField.isEmpty()) {
      throw new IllegalArgumentException("Lookup key field (lookupKeyField) not specified for lookup stream");
    }
    if (lookupValueField == null || lookupValueField.isEmpty()) {
      throw new IllegalArgumentException("Lookup value field (lookupValueField) not specified for lookup stream");      
    }
    if (condition != null) {
      condition.validate();
    }
  }
  
  @Override
  public ProcessorType getType() {
    return type;
  }
  
  @Override
  public Condition getCondition() {
    return condition;
  }  

  @Override
  public String getId() {
    return id;
  }
  
  /**
   * Get the feed for the lookup key/value pairs.
   * 
   * This data feed should result in two columns:
   * <ul>
   * <li>lookupKeyField - The key that will be used to find values in the map.
   * <li>lookupValueField - The value that will be take from the map and put into the main feed.
   * </ul>
   * 
   * @return a SourcePipeline defining a feed that defines a map for lookup key/value pairs.
   */
  @Schema(description = """
                        Get the feed for the lookup key/value pairs.
                        
                        This data feed should result in two columns:
                        <ul>
                        <li>lookupKeyField - The key that will be used to find values in the map.
                        <li>lookupValueField - The value that will be take from the map and put into the main feed.
                        </ul>
                        """)
  public SourcePipeline getMap() {
    return map;
  }
    
  /**
   * Get the name of the field in the lookupSource that provides the keys for the map.
   * 
   * @return the name of the field in the lookupSource that provides the keys for the map.
   */
  @Schema(description = """
                        The name of the field in the lookupSource that provides the keys for the map.
                        """
          , maxLength = 100
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public String getLookupKeyField() {
    return lookupKeyField;
  }

  /**
   * Get the name of the field in the lookupSource the provides the values for the map.
   * 
   * @return the name of the field in the lookupSource the provides the values for the map.
   */
  @Schema(description = """
                        The name of the field in the lookupSource that provides the values for the map.
                        """
            , maxLength = 100
            , requiredMode = Schema.RequiredMode.REQUIRED
    )
    public String getLookupValueField() {
    return lookupValueField;
  }

  /**
   * Get the fields in the main stream that are to be looked up and the fields that are to be created in the main stream for the values found.
   * @return the fields in the main stream that are to be looked up and the fields that are to be created in the main stream for the values found.
   */
  @Schema(description = """
                        The fields in the main stream that are to be looked up and the fields that are to be created in the main stream for the values found.
                        """)
  public List<ProcessorLookupField> getLookupFields() {
    return lookupFields;
  }
  
  /**
   * Builder class for ProcessorLookup.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  public static class Builder {

    private ProcessorType type = ProcessorType.LOOKUP;
    private Condition condition;
    private String id;
    private String lookupKeyField;
    private String lookupValueField;
    private List<ProcessorLookupField> lookupFields;
    private SourcePipeline map;

    private Builder() {
    }

    /**
     * Set the {@link ProcessorLookup#type} value in the builder.
     * @param value The value for the {@link ProcessorLookup#type}, must be {@link ProcessorType#DYNAMIC_FIELD}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder type(final ProcessorType value) {
      this.type = value;
      return this;
    }

    /**
     * Set the {@link ProcessorLookup#condition} value in the builder.
     * @param value The value for the {@link ProcessorLookup#condition}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder condition(final Condition value) {
      this.condition = value;
      return this;
    }

    /**
     * Set the {@link ProcessorLookup#id} value in the builder.
     * @param value The value for the {@link ProcessorLookup#id}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder id(final String value) {
      this.id = value;
      return this;
    }

    /**
     * Set the {@link ProcessorLookup#lookupKeyField} value in the builder.
     * @param value The value for the {@link ProcessorLookup#lookupKeyField}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder lookupKeyField(final String value) {
      this.lookupKeyField = value;
      return this;
    }

    /**
     * Set the {@link ProcessorLookup#lookupValueField} value in the builder.
     * @param value The value for the {@link ProcessorLookup#lookupValueField}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder lookupValueField(final String value) {
      this.lookupValueField = value;
      return this;
    }

    /**
     * Set the {@link ProcessorLookup#lookupFields} value in the builder.
     * @param value The value for the {@link ProcessorLookup#lookupFields}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder lookupFields(final List<ProcessorLookupField> value) {
      this.lookupFields = value;
      return this;
    }

    /**
     * Set the {@link ProcessorLookup#map} value in the builder.
     * @param value The value for the {@link ProcessorLookup#map}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder map(final SourcePipeline value) {
      this.map = value;
      return this;
    }

    /**
     * Construct a new instance of the ProcessorLookup class.
     * @return a new instance of the ProcessorLookup class.
     */
    public ProcessorLookup build() {
      ProcessorLookup result = new ProcessorLookup(type, condition, id, lookupKeyField, lookupValueField, lookupFields, map);
      result.validateType(ProcessorType.LOOKUP, type);
      return result;
    }
  }

  /**
   * Construct a new instance of the ProcessorLookup.Builder class.
   * @return a new instance of the ProcessorLookup.Builder class.
   */
  public static ProcessorLookup.Builder builder() {
    return new ProcessorLookup.Builder();
  }

  private ProcessorLookup(final ProcessorType type, final Condition condition, final String id, final String lookupKeyField, final String lookupValueField, final List<ProcessorLookupField> lookupFields, final SourcePipeline map) {
    this.type = type;
    this.condition = condition;
    this.id = id;
    this.lookupKeyField = lookupKeyField;
    this.lookupValueField = lookupValueField;
    this.lookupFields = ImmutableCollectionTools.copy(lookupFields);
    this.map = map;
  }
  
}
