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
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;
import uk.co.spudsoft.query.exec.procs.query.ProcessorDynamicFieldInstance;

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
  private final boolean innerJoin;
  
  private final String fieldIdColumn;
  private final String fieldNameColumn;
  private final String fieldTypeColumn;
  private final String fieldColumnColumn;
  
  private final String parentIdColumn;

  private final String valuesParentIdColumn;
  private final String valuesFieldIdColumn;
  
  private final SourcePipeline fieldDefns;
  private final SourcePipeline fieldValues;
  
  @Override
  public ProcessorInstance createInstance(Vertx vertx, SourceNameTracker sourceNameTracker, Context context) {
    return new ProcessorDynamicFieldInstance(vertx, sourceNameTracker, context, this);
  }

  @Override
  public void validate() {
    validateType(ProcessorType.DYNAMIC_FIELD, type);
    if (fieldDefns == null) {
      throw new IllegalArgumentException("Field definitions (fieldDefns) pipeline not provided");
    }
    fieldDefns.validate();
    if (fieldValues == null) {
      throw new IllegalArgumentException("Field values (fieldValues) pipeline not provided");
    }
    fieldValues.validate();
  }
  
  @Override
  public ProcessorType getType() {
    return type;
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
   * Get the parent ID column.
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
  )
  public String getParentIdColumn() {
    return parentIdColumn;
  }

  /**
   * Get the name of the column in the field defns feed that is used to identify the extra column.
   * @return the name of the column in the field defns feed that is used to identify the extra column.
   */
  @Schema(description = """
                        The name of the column in the field defns feed that is used to identify the extra column.
                        """
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
  )
  public String getFieldColumnColumn() {
    return fieldColumnColumn;
  }

  /**
   * Get the name of the column in the values feed that contains the ID to match to the parent feed.
   * The values feed must be sorted by this column.
   * @return the name of the column in the values feed that contains the ID to match to the parent feed.
   */
  @Schema(description = """
                        The name of the column in the values feed that contains the ID to match to the parent feed.
                        <P>
                        The values feed must be sorted by this column.
                        """
  )
  public String getValuesParentIdColumn() {
    return valuesParentIdColumn;
  }

  /**
   * Get the name of the column in the values feed that contains the ID of the field represented by that row.
   * @return the name of the column in the values feed that contains the ID of the field represented by that row.
   */
  @Schema(description = """
                        The name of the column in the values feed that contains the ID of the field represented by that row.
                        """
  )
  public String getValuesFieldIdColumn() {
    return valuesFieldIdColumn;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private ProcessorType type = ProcessorType.DYNAMIC_FIELD;
    private boolean innerJoin;
    private String fieldIdColumn = "id";
    private String fieldNameColumn = "name";
    private String fieldTypeColumn = "type";
    private String fieldColumnColumn = "column";
    private String parentIdColumn;
    private String valuesParentIdColumn;
    private String valuesFieldIdColumn;
    private SourcePipeline fieldDefns;
    private SourcePipeline fieldValues;

    private Builder() {
    }
    
    public Builder type(final ProcessorType value) {
      this.type = value;
      return this;
    }

    public Builder innerJoin(final boolean value) {
      this.innerJoin = value;
      return this;
    }

    public Builder fieldIdColumn(final String value) {
      this.fieldIdColumn = value;
      return this;
    }

    public Builder fieldNameColumn(final String value) {
      this.fieldNameColumn = value;
      return this;
    }

    public Builder fieldTypeColumn(final String value) {
      this.fieldTypeColumn = value;
      return this;
    }

    public Builder fieldColumnColumn(final String value) {
      this.fieldColumnColumn = value;
      return this;
    }

    public Builder parentIdColumn(final String value) {
      this.parentIdColumn = value;
      return this;
    }

    public Builder valuesParentIdColumn(final String value) {
      this.valuesParentIdColumn = value;
      return this;
    }

    public Builder valuesFieldIdColumn(final String value) {
      this.valuesFieldIdColumn = value;
      return this;
    }

    public Builder fieldDefns(final SourcePipeline value) {
      this.fieldDefns = value;
      return this;
    }

    public Builder fieldValues(final SourcePipeline value) {
      this.fieldValues = value;
      return this;
    }

    public ProcessorDynamicField build() {
      return new uk.co.spudsoft.query.defn.ProcessorDynamicField(type, innerJoin, fieldIdColumn, fieldNameColumn, fieldTypeColumn, fieldColumnColumn, parentIdColumn, valuesParentIdColumn, valuesFieldIdColumn, fieldDefns, fieldValues);
    }
  }

  public static ProcessorDynamicField.Builder builder() {
    return new ProcessorDynamicField.Builder();
  }

  private ProcessorDynamicField(final ProcessorType type, final boolean innerJoin, final String fieldIdColumn, final String fieldNameColumn, final String fieldTypeColumn, final String fieldColumnColumn, final String parentIdColumn, final String valuesParentIdColumn, final String valuesFieldIdColumn, final SourcePipeline fieldDefns, final SourcePipeline fieldValues) {
    validateType(ProcessorType.DYNAMIC_FIELD, type);
    this.type = type;
    this.innerJoin = innerJoin;
    this.fieldIdColumn = fieldIdColumn;
    this.fieldNameColumn = fieldNameColumn;
    this.fieldTypeColumn = fieldTypeColumn;
    this.fieldColumnColumn = fieldColumnColumn;
    this.parentIdColumn = parentIdColumn;
    this.valuesParentIdColumn = valuesParentIdColumn;
    this.valuesFieldIdColumn = valuesFieldIdColumn;
    this.fieldDefns = fieldDefns;
    this.fieldValues = fieldValues;
  }
    
  
  
  
}
