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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Vertx;
import java.util.List;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.procs.subquery.ProcessorDynamicFieldInstance;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * Processor that takes in multiple streams and uses them to dynamically add fields to the primary stream.
 * 
 * This is aimed at converting tables of key/value pairs into fields.
 * 
 * Two child pipelines must be defined:
 * <ul>
 * <li>The definition  pipeline, that is queried in its entirety at the beginning and that defines the columns that will be found.
 * <li>The values pipeline, that is queried in parallel with the main stream and the supplies the data for the dynamic columns.
 * </ul>
 * 
 * The definition pipeline must provide four fields:
 * <ul>
 * <li>The ID for the dynamic column to be added - this value must correspond to the ID from the values pipeline.
 * <li>The name for the dynamic column - this will be the name of the newly created field.
 * <li>The type for the dynamic column - one of the{@link uk.co.spudsoft.query.defn.DataType} values.
 * <li>The name of the column in the values stream that will contain the actual value.
 * </ul>
 * The names of these four fields can be controlled using the field*Column properties on this processor (though they have sensible defaults).
 * 
 * The values pipeline must provide at least three fields:
 * <ul>
 * <li>The parent ID, that matches the ID of the data row in the main pipeline.
 * <li>The field ID, that matches one of the rows returned from the definition pipeline.
 * <li>The value field, whose name must match that defined for the selected field.
 * </ul>
 * 
 * As a streaming processor this processor requires the main pipeline and the values pipeline to be sorted by the same ID (the parent ID from the point of view of this processor).
 * 
 * The processor works by:
 * <ol>
 * <li>If the parent ID is greater than the values ID, skip through values until it isn't.
 * <li>If the values ID is greater than the parent ID, skip through parent rows until it isn't.
 * <li>While the two IDs match:
 * <ol>
 * <li>Find the definition for the current value.
 * <li>Get the name of the value field from the field definition.
 * <li>Add a new field to the parent data row with the name from the field definition and the value from the value field of the value row.
 * </ol>
 * </ol>
 * 
 * 
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorDynamicField.Builder.class)
@Schema(description = 
        """
        Processor that takes in multiple streams and uses them to dynamically add fields to the primary stream.

        Two child pipelines must be defined:
        <ul>
        <li>The definition  pipeline, that is queried in its entirety at the beginning and that defines the columns that will be found.
        <li>The values pipeline, that is queried in parallel with the main stream and the supplies the data for the dynamic columns.
        </il>
        The definition pipeline must provide four fields:
        <ul>
        <li>The ID for the dynamic column to be added - this value must correspond to the ID from the values pipeline.
        <li>The name for the dynamic column - this will be the name of the newly created field.
        <li>The type for the dynamic column - one of the{@link uk.co.spudsoft.query.defn.DataType} values.
        <li>The name of the column in the values stream that will contain the actual value.
        </ul>
        The names of these four fields can be controlled using the field*Column properties on this processor (though they have sensible defaults).
        
        The values pipeline must provide at least three fields:
        <ul>
        <li>The parent ID, that matches the ID of the data row in the main pipeline.
        <li>The field ID, that matches one of the rows returned from the definition pipeline.
        <li>The value field, whose name must match that defined for the selected field.
        </ul>
        
        As a streaming processor this processor requires the main pipeline and the values pipeline to be sorted by the same ID (the parent ID from the point of view of this processor).
        
        The processor works by:
        <ol>
        <li>If the parent ID is greater than the values ID, skip through values until it isn't.
        <li>If the values ID is greater than the parent ID, skip through parent rows until it isn't.
        <li>While the two IDs match:
        <ol>
        <li>Find the definition for the current value.
        <li>Get the name of the value field from the field definition.
        <li>Add a new field to the parent data row with the name from the field definition and the value from the value field of the value row.
        </ol>
        </ol>
        """
)
public class ProcessorDynamicField implements Processor {

  private final ProcessorType type;
  private final Condition condition;
  private final String name;
  
  private final boolean innerJoin;
  private final boolean useCaseInsensitiveFieldNames;
  
  private final String fieldIdColumn;
  private final String fieldNameColumn;
  private final String fieldTypeColumn;
  private final String fieldColumnColumn;
  
  private final ImmutableList<String> parentIdColumns;
  private final ImmutableList<String> valuesParentIdColumns;
  private final String valuesFieldIdColumn;
  
  private final String fieldValueColumnName;
  
  private final SourcePipeline fieldDefns;
  private final SourcePipeline fieldValues;
  
  @Override
  public ProcessorDynamicFieldInstance createInstance(Vertx vertx, MeterRegistry meterRegistry, Auditor auditor, PipelineContext pipelineContext, String name) {
    return new ProcessorDynamicFieldInstance(vertx, meterRegistry, auditor, pipelineContext, this, name);
  }

  @Override
  public void validate() {
    validateType(ProcessorType.DYNAMIC_FIELD, type);
    if (fieldDefns == null) {
      throw new IllegalArgumentException("Field definitions (fieldDefns) pipeline not provided");
    }
    fieldDefns.validate((Strings.isNullOrEmpty(name) ? "DynamicField" : "DynamicField " + name) + " fieldDefns");
    if (fieldValues == null) {
      throw new IllegalArgumentException("Field values (fieldValues) pipeline not provided");
    }
    fieldValues.validate((Strings.isNullOrEmpty(name) ? "DynamicField" : "DynamicField " + name) + " fieldValues");
    if (parentIdColumns == null || parentIdColumns.isEmpty()) {
      throw new IllegalArgumentException("ID column(s) not specified for parent stream");      
    }
    if (valuesParentIdColumns == null || valuesParentIdColumns.isEmpty()) {
      throw new IllegalArgumentException("ID column(s) not specified for values stream");      
    }
    if (parentIdColumns.size() != valuesParentIdColumns.size()) {
      throw new IllegalArgumentException("ID column(s) specified for parent stream does not have the same number of fields as those specified for values stream");
    }
    if (Strings.isNullOrEmpty(fieldTypeColumn)) {
      throw new IllegalArgumentException("Type column not set (fieldTypeColumn)");
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
  public String getName() {
    return name;
  }
  
  /**
   * Get the feed for the field definitions.
   * 
   * This data feed should result in four columns:
   * <ul>
   * <li>fieldIdColumn - The ID value that will be used to refer to the field from the values feed.
   * <li>fieldNameColumn - The name that the resultant field will be given.
   * <li>fieldTypeColumn - The type of the resultant field (will be processed using {@link uk.co.spudsoft.query.defn.DataType#valueOf(java.lang.String)}/
   * <li>fieldColumnColumn - The column in the field values feed that contains the actual value for this field.
   * </ul>
   * The fields will be added to the parent feed in the order of the rows returned by this query (regardless of the ordering in the fieldValues feed).
   * 
   * @return a SourcePipeline defining a feed that defines additional fields to add to the parent feed.
   */
  @Schema(description = """
                        Get the feed for the field definitions.
                        
                        This data feed should result in four columns:
                        <ul>
                        <li>fieldIdColumn - The ID value that will be used to refer to the field from the values feed.
                        <li>fieldNameColumn - The name that the resultant field will be given.
                        <li>fieldTypeColumn - The type of the resultant field (will be processed using {@link uk.co.spudsoft.query.defn.DataType#valueOf(java.lang.String)}/
                        <li>fieldColumnColumn - The column in the field values feed that contains the actual value for this field.
                        </ul>
                        The fields will be added to the parent feed in the order of the rows returned by this query (regardless of the ordering in the fieldValues feed).
                        """)
  public SourcePipeline getFieldDefns() {
    return fieldDefns;
  }

  /**
   * Get the feed for the field values.
   * 
   * This data feed should result in at least three columns:
   * <ul>
   * <li>valuesParentIdColumn - ID of the parent row that is gaining a field value.
   * <li>valuesFieldIdColumn - ID of the field that this row relates to (used to define the type and name of the resulting field).
   * <li>Values - One or more fields that contain values, identified from the Column value in the FieldDefns feed.
   * </ul>
   * 
   * 
   * @return a SourcePipeline defining a feed that defines values for additional fields to add to the parent feed.
   */
  @Schema(description = """
                        The feed for the field values.
                        <P>
                        This data feed should result in at least three columns:
                        <ul>
                        <li>valuesParentIdColumn - ID of the parent row that is gaining a field value.
                        <li>valuesFieldIdColumn - ID of the field that this row relates to (used to define the type and name of the resulting field).
                        <li>Values - One or more fields that contain values, identified from the Column value in the FieldDefns feed.
                        </ul>
                        """)
  public SourcePipeline getFieldValues() {
    return fieldValues;
  }
    
  /**
   * Get the inner join flag.
   * If set to true the parent row will only be output if the child feed has at least one matching row.
   * @return the inner join flag.
   */
  @Schema(description = """
                        The inner join flag.
                        <P>
                        If set to true the parent row will only be output if the child feed has at least one matching row.
                        """)
  public boolean isInnerJoin() {
    return innerJoin;
  }

  /**
   * Get the case insensitivity flag.
   * If set to true two dynamic field names that differ only in case will be considered to be the same field.
   * For the sake of clarity this should usually be left false.
   * @return the case insensitivity flag.
   */
  @Schema(description = """
                        The inner join flag.
                        <P>
                        If set to true the parent row will only be output if the child feed has at least one matching row.
                        <P>
                        For the sake of clarity this should usually be left as false.
                        """
          , defaultValue = "false"
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public boolean isUseCaseInsensitiveFieldNames() {
    return useCaseInsensitiveFieldNames;
  }
  
  /**
   * Get the parent ID columns.
   * 
   * This is the name of the field in the main stream that is to be used to match against child rows.
   * The main stream must be sorted by this field.
   * 
   * @return the parent ID column.
   */
  @Schema(description = """
                        The parent ID column.
                        <P>
                        This is the name of the field in the main stream that is to be used to match against child rows.
                        The main stream must be sorted by this field.
                        """
          , maxLength = 100
  )
  public List<String> getParentIdColumns() {
    return parentIdColumns;
  }

  /**
   * Get the name of the column in the field defns feed that is used to identify the extra column.
   * @return the name of the column in the field defns feed that is used to identify the extra column.
   */
  @Schema(description = """
                        The name of the column in the field defns feed that is used to identify the extra column.
                        """
          , maxLength = 100
  )
  public String getFieldIdColumn() {
    return fieldIdColumn;
  }

  /**
   * Get the name of the column in the field defns feed that is used to name the extra column.
   * @return the name of the column in the field defns feed that is used to name the extra column.
   */
  @Schema(description = """
                        The name of the column in the field defns feed that is used to name the extra column.
                        """
          , maxLength = 100
  )
  public String getFieldNameColumn() {
    return fieldNameColumn;
  }

  /**
   * Get the name of the column in the field defns feed that is used to determine the type of the extra column.
   * @return the name of the column in the field defns feed that is used to determine the type of the extra column.
   */
  @Schema(description = """
                        The name of the column in the field defns feed that is used to determine the type of the extra column.
                        """
          , maxLength = 100
  )
  public String getFieldTypeColumn() {
    return fieldTypeColumn;
  }

  /**
   * Get the name of the column in the field defns feed that is used to find the name of the field in the values feed that contains the actual value.
   * @return the name of the column in the field defns feed that is used to find the name of the field in the values feed that contains the actual value.
   */
  @Schema(description = """
                        The name of the column in the field defns feed that is used to find the name of the field in the values feed that contains the actual value.
                        """
          , maxLength = 100
  )
  public String getFieldColumnColumn() {
    return fieldColumnColumn;
  }

  /**
   * Get the names of the columns in the values feed that contains the ID to match to the parent feed.
   * The values feed must be sorted by these columns.
   * @return the name of the column in the values feed that contains the ID to match to the parent feed.
   */
  @Schema(description = """
                        The name of the column in the values feed that contains the ID to match to the parent feed.
                        <P>
                        The values feed must be sorted by this column.
                        """
          , maxLength = 100
  )
  public List<String> getValuesParentIdColumns() {
    return valuesParentIdColumns;
  }

  /**
   * Get the name of the column in the values feed that contains the ID of the field represented by that row.
   * @return the name of the column in the values feed that contains the ID of the field represented by that row.
   */
  @Schema(description = """
                        The name of the column in the values feed that contains the ID of the field represented by that row.
                        """
          , maxLength = 100
  )
  public String getValuesFieldIdColumn() {
    return valuesFieldIdColumn;
  }

  /**
   * Get the list of fields to look in for the field value.
   * This should not be used, the correct approach is to identify the field value column in the field definition query - this approach only exists for backwards compatibility.
   * 
   * When set, this should be a comma separate list of field names from the values stream. 
   * Even if this value is set, it will only be used if the field value column in the field definition query is not set.
   * At runtime the named columns in the values stream will be checked in order and the first one that is not null be be taken.
   * 
   * @return the list of fields to look in for the field value.
   */
  @Schema(description = """
                        The list of fields to look in for the field value.
                        <P>
                        This should not be used, the correct approach is to identify the field value column in the field definition query - this approach only exists for backwards compatibility.
                        <P>
                        When set, this should be a comma separate list of field names from the values stream.
                        Even if this value is set, it will only be used if the field value column in the field definition query is not set.
                        At runtime the named columns in the values stream will be checked in order and the first one that is not null be be taken.
                        """
          , maxLength = 200
  )
  public String getFieldValueColumnName() {
    return fieldValueColumnName;
  }

  /**
   * Builder class for ProcessorDynamicField.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  public static class Builder {

    private ProcessorType type = ProcessorType.DYNAMIC_FIELD;
    private Condition condition;
    private String name;
    private boolean innerJoin;
    private boolean useCaseInsensitiveFieldNames;
    private String fieldIdColumn = "id";
    private String fieldNameColumn = "name";
    private String fieldTypeColumn = "type";
    private String fieldColumnColumn = "column";
    private List<String> parentIdColumns;
    private List<String> valuesParentIdColumns;
    private String valuesFieldIdColumn;
    private String fieldValueColumnName;
    private SourcePipeline fieldDefns;
    private SourcePipeline fieldValues;

    private Builder() {
    }
    
    /**
     * Set the {@link ProcessorDynamicField#type} value in the builder.
     * @param value The value for the {@link ProcessorDynamicField#type}, must be {@link ProcessorType#DYNAMIC_FIELD}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder type(final ProcessorType value) {
      this.type = value;
      return this;
    }

    /**
     * Set the {@link ProcessorDynamicField#condition} value in the builder.
     * @param value The value for the {@link ProcessorDynamicField#condition}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder condition(final Condition value) {
      this.condition = value;
      return this;
    }
    
    /**
     * Set the {@link ProcessorDynamicField#name} value in the builder.
     * @param value The value for the {@link ProcessorDynamicField#name}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder name(final String value) {
      this.name = value;
      return this;
    }

    /**
     * Set the {@link ProcessorDynamicField#innerJoin} value in the builder.
     * @param value The value for the {@link ProcessorDynamicField#innerJoin}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder innerJoin(final boolean value) {
      this.innerJoin = value;
      return this;
    }

    /**
     * Set the {@link ProcessorDynamicField#useCaseInsensitiveFieldNames} value in the builder.
     * @param value The value for the {@link ProcessorDynamicField#useCaseInsensitiveFieldNames}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder useCaseInsensitiveFieldNames(final boolean value) {
      this.useCaseInsensitiveFieldNames = value;
      return this;
    }
    
    /**
     * Set the {@link ProcessorDynamicField#fieldIdColumn} value in the builder.
     * @param value The value for the {@link ProcessorDynamicField#fieldIdColumn}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder fieldIdColumn(final String value) {
      this.fieldIdColumn = value;
      return this;
    }

    /**
     * Set the {@link ProcessorDynamicField#fieldNameColumn} value in the builder.
     * @param value The value for the {@link ProcessorDynamicField#fieldNameColumn}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder fieldNameColumn(final String value) {
      this.fieldNameColumn = value;
      return this;
    }

    /**
     * Set the {@link ProcessorDynamicField#fieldTypeColumn} value in the builder.
     * @param value The value for the {@link ProcessorDynamicField#fieldTypeColumn}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder fieldTypeColumn(final String value) {
      this.fieldTypeColumn = value;
      return this;
    }

    /**
     * Set the {@link ProcessorDynamicField#fieldColumnColumn} value in the builder.
     * @param value The value for the {@link ProcessorDynamicField#fieldColumnColumn}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder fieldColumnColumn(final String value) {
      this.fieldColumnColumn = value;
      return this;
    }

    /**
     * Set the {@link ProcessorDynamicField#parentIdColumns} value in the builder.
     * @param value The value for the {@link ProcessorDynamicField#parentIdColumns}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder parentIdColumns(final List<String> value) {
      this.parentIdColumns = value;
      return this;
    }

    /**
     * Set the {@link ProcessorDynamicField#valuesParentIdColumns} value in the builder.
     * @param value The value for the {@link ProcessorDynamicField#valuesParentIdColumns}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder valuesParentIdColumns(final List<String> value) {
      this.valuesParentIdColumns = value;
      return this;
    }

    /**
     * Set the {@link ProcessorDynamicField#valuesFieldIdColumn} value in the builder.
     * @param value The value for the {@link ProcessorDynamicField#valuesFieldIdColumn}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder valuesFieldIdColumn(final String value) {
      this.valuesFieldIdColumn = value;
      return this;
    }

    /**
     * Set the {@link ProcessorDynamicField#fieldValueColumnName} value in the builder.
     * @param value The value for the {@link ProcessorDynamicField#fieldValueColumnName}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder fieldValueColumnName(final String value) {
      this.fieldValueColumnName = value;
      return this;
    }

    /**
     * Set the {@link ProcessorDynamicField#fieldDefns} value in the builder.
     * @param value The value for the {@link ProcessorDynamicField#fieldDefns}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder fieldDefns(final SourcePipeline value) {
      this.fieldDefns = value;
      return this;
    }

    /**
     * Set the {@link ProcessorDynamicField#fieldValues} value in the builder.
     * @param value The value for the {@link ProcessorDynamicField#fieldValues}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder fieldValues(final SourcePipeline value) {
      this.fieldValues = value;
      return this;
    }

    /**
     * Construct a new instance of the ProcessorDynamicField class.
     * @return a new instance of the ProcessorDynamicField class.
     */
    public ProcessorDynamicField build() {
      ProcessorDynamicField result = new ProcessorDynamicField(type, condition, name, innerJoin, useCaseInsensitiveFieldNames, fieldIdColumn, fieldNameColumn, fieldTypeColumn, fieldColumnColumn, parentIdColumns, valuesParentIdColumns, valuesFieldIdColumn, fieldValueColumnName, fieldDefns, fieldValues);
      result.validateType(ProcessorType.DYNAMIC_FIELD, type);
      return result;
    }
  }

  /**
   * Construct a new instance of the ProcessorDynamicField.Builder class.
   * @return a new instance of the ProcessorDynamicField.Builder class.
   */
  public static ProcessorDynamicField.Builder builder() {
    return new ProcessorDynamicField.Builder();
  }

  private ProcessorDynamicField(final ProcessorType type, final Condition condition, final String name
          , final boolean innerJoin, final boolean useCaseInsensitiveFieldNames
          , final String fieldIdColumn, final String fieldNameColumn, final String fieldTypeColumn, final String fieldColumnColumn
          , final List<String> parentIdColumns, final List<String> valuesParentIdColumns
          , final String valuesFieldIdColumn, final String fieldValueColumnName, final SourcePipeline fieldDefns, final SourcePipeline fieldValues) {
    this.type = type;
    this.condition = condition;
    this.name = name;
    this.innerJoin = innerJoin;
    this.useCaseInsensitiveFieldNames = useCaseInsensitiveFieldNames;
    this.fieldIdColumn = fieldIdColumn;
    this.fieldNameColumn = fieldNameColumn;
    this.fieldTypeColumn = fieldTypeColumn;
    this.fieldColumnColumn = fieldColumnColumn;
    this.parentIdColumns = ImmutableCollectionTools.copy(parentIdColumns);
    this.valuesParentIdColumns = ImmutableCollectionTools.copy(valuesParentIdColumns);
    this.valuesFieldIdColumn = valuesFieldIdColumn;
    this.fieldValueColumnName = fieldValueColumnName;
    this.fieldDefns = fieldDefns;
    this.fieldValues = fieldValues;
  }
}
