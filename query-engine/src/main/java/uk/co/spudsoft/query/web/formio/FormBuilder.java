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
package uk.co.spudsoft.query.web.formio;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.defn.ArgumentGroup;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.defn.ArgumentValue;
import static uk.co.spudsoft.query.defn.DataType.Null;
import uk.co.spudsoft.query.defn.Format;
import uk.co.spudsoft.query.exec.FilterFactory;
import uk.co.spudsoft.query.exec.conditions.ConditionInstance;
import uk.co.spudsoft.query.exec.conditions.JexlEvaluator;
import uk.co.spudsoft.query.exec.conditions.RequestContext;
import uk.co.spudsoft.query.pipeline.PipelineNodesTree.PipelineFile;
import uk.co.spudsoft.query.web.formio.DateTime.DatePicker;

/**
 * Builder class to output a formio representation of a pipeline definition.
 * 
 * The output is written directly to the OutputStream as it is generated.
 * The helper classes in this package are all based on a try-with-resources model where the close() method outputs the closing HTML tag.
 * 
 * @author jtalbut
 */
@SuppressWarnings("try")
public class FormBuilder {
  
  private static final Logger logger = LoggerFactory.getLogger(FormBuilder.class);
    
  private final JsonFactory factory;
  private final RequestContext requestContext;
  private final int columns;
  private final FilterFactory filterFactory;
  
  /**
   * Constructor.
   * @param requestContext the context of the request, for evaluating expressions.
   * @param columns The number of columns to use for the arguments.
   * @param filterFactory The FilterFactory to use to get filter arguments.3
   */
  @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
  public FormBuilder(RequestContext requestContext, int columns, FilterFactory filterFactory) {
    this.factory = new JsonFactory();
    this.requestContext = requestContext;
    this.columns = columns;
    this.filterFactory = filterFactory;
  }
  
  /**
   * Return true if the passed in collection is null or empty.
   * @param collection the nullable collection that may be empty.
   * @return true if the passed in collection is null or empty.
   */
  static boolean isNullOrEmpty(Collection<?> collection) {
    return collection == null || collection.isEmpty();
  }
  
  /**
   * Build a form definition from a pipeline to an {@link OutputStream}.
   * @param pipeline the {@link PipelineFile} that contains enough of a {@link uk.co.spudsoft.query.defn.Pipeline} definition to build the input form.
   * @param stream the {@link OutputStream} to which the formio form will be written.
   * @throws IOException if anything goes wrong.
   */
  public void buildForm(PipelineFile pipeline, OutputStream stream) throws IOException {
    
    try (JsonGenerator generator = factory.createGenerator(stream, JsonEncoding.UTF8)) {
      try (Form f = new Form(generator)) {        
        f.withName(pipeline.getName())
            .withTitle(pipeline.getTitle())
            .withPath(pipeline.getPath())
            .withDisplay("form")
            ;
        try (ComponentArray a = f.addComponents()) {
          buildDescription(generator, pipeline);

          buildArguments(generator, pipeline);
          
          buildFilters(generator, pipeline);
          buildOutput(generator, pipeline);
          buildButtons(generator, pipeline);
        }
      }
    } catch (Throwable ex) {
      logger.warn("Exception in form builder: ", ex);
    }
  }
  
  void buildDescription(JsonGenerator generator, PipelineFile pipeline) throws IOException {
    try (Content description = new Content(generator)) {
      description
            .withHtml("<p><h2>" + pipeline.getTitle() + "</h2></p><p>" + pipeline.getDescription() + "</p>")
            .withCustomClass("border-bottom mb-3")
            ;
    }
  }

  private Map<String, List<Argument>> collateArguments(PipelineFile pipeline) {
    Map<String, List<Argument>> result = new HashMap<>();
    
    for (Argument arg : pipeline.getArguments()) {
      if (arg.isHidden()) {
        continue ;
      }
      
      boolean add = false;
      if (arg.getCondition() == null || Strings.isNullOrEmpty(arg.getCondition().getExpression())) {
        add = true;
      } else {
        ConditionInstance conditionInstance = arg.getCondition().createInstance();
        if (conditionInstance.evaluate(requestContext, null)) {
          add = true;
        }
      }
      if (add) {
        String group = Strings.isNullOrEmpty(arg.getGroup()) ? "" : arg.getGroup();
        List<Argument> args = result.get(group);
        if (args == null) {
          args = new ArrayList<>();
          result.put(group, args);
        }
        args.add(arg);
      }
    }
    
    return result;
  }
  
  
  void buildArguments(JsonGenerator generator, PipelineFile pipeline) throws IOException {
    
    Map<String, List<Argument>> groupedArguments = collateArguments(pipeline);
    
    if (groupedArguments.isEmpty()) {
      return ;
    }
    
    try (FieldSet fieldSet = new FieldSet(generator)) {
      fieldSet
            .withLegend("Arguments")
            .withCustomClass("qe-arguments border-bottom");
      try (ComponentArray components = fieldSet.addComponents()) {
        buildArguments(generator, pipeline, groupedArguments.get(""), null);
        for (ArgumentGroup group : pipeline.getArgumentGroups()) {
          buildArguments(generator, pipeline, groupedArguments.get(group.getName()), group);
        }
      }
    }    
  }

  void buildArguments(JsonGenerator generator, PipelineFile pipeline, List<Argument> args, ArgumentGroup group) throws IOException {
    
    int fieldsPerColumn = Math.max(1, (int) Math.ceil((double) args.size() / columns));
    
    if (group == null) {
      addArgumentsToCurrentContainer(generator, args, fieldsPerColumn);
    } else {
      try (AbstractContainer<?> groupContainer = buildFieldGroup(generator, group)) {
        try (ComponentArray fieldSetArray = groupContainer.addComponents()) {
          if (!Strings.isNullOrEmpty(group.getDescription())) {
            try (Content description = new Content(generator)) {
              description
                    .withHtml("<p>" + group.getDescription() + "</p>")
                    .withCustomClass("border-bottom mb-3")
                    ;
            }
          }
          addArgumentsToCurrentContainer(generator, args, fieldsPerColumn);
        }
      }
    }
  }
  
  private AbstractContainer<?> buildFieldGroup(JsonGenerator generator, ArgumentGroup group) throws IOException {
    switch (group.getType()) {
      case FIELD_SET -> {
        FieldSet fs = new FieldSet(generator);
        fs.withLegend(Strings.isNullOrEmpty(group.getTitle()) ? group.getName() : group.getTitle());
        return fs;
      }
      case COLLAPSIBLE_PANEL -> {
        Panel panel = new Panel(generator);
        panel.withTitle(Strings.isNullOrEmpty(group.getTitle()) ? group.getName() : group.getTitle());
        panel.withCollapsible(true);
        return panel;
      }
      case PANEL -> {
        Panel panel = new Panel(generator);
        panel.withTitle(Strings.isNullOrEmpty(group.getTitle()) ? group.getName() : group.getTitle());
        panel.withCollapsible(false);
        return panel;
      }
      default -> throw new IllegalArgumentException("Unhandled ArgumentGroup type " + group.getType());
    }
  }

  private void addArgumentsToCurrentContainer(JsonGenerator generator, List<Argument> args, int fieldsPerColumn) throws IllegalStateException, IOException {
    int fieldIdx = 0;
    int colIdx = 0;
    try (Columns columns = new Columns(generator)) {
      try (ComponentArray columnArray = columns.addColumns()) {
        while (fieldIdx < args.size()) {
          try (Columns.Column col = new Columns.Column(generator)) {
            col.withWidth(12 / this.columns);
            col.withSize("md");
            try (ComponentArray fieldArray = col.addComponents()) {
              for (; fieldIdx < (colIdx * fieldsPerColumn) + fieldsPerColumn && fieldIdx < args.size(); ++fieldIdx) {
                buildArgument(args.get(fieldIdx), generator);
              }
            }
          }
          ++colIdx;
        }
      }
    }
  }

  private void buildArgument(Argument arg, JsonGenerator generator) throws IOException, IllegalStateException {
    switch (arg.getType()) {
      case Date, Time, DateTime -> buildDateTime(generator, arg);
      case Double, Integer, Long, Float -> buildNumber(generator, arg);
      case String -> {
        if (!isNullOrEmpty(arg.getPossibleValues()) || !Strings.isNullOrEmpty(arg.getPossibleValuesUrl())) {
          buildSelect(generator, arg);
        } else {
          buildTextField(generator, arg);
        }
      }
      case Boolean -> buildCheckBox(generator, arg);
      case Null -> {
        logger.warn("Argument {} is of null type", arg.getName());
      }
      default -> {
        logger.warn("Argument {} is of unknown type ({})", arg.getName(), arg.getType());
      }
    }
  }
  
  void buildFilters(JsonGenerator generator, PipelineFile pipeline) throws IOException {
    if (filterFactory != null && !filterFactory.getSortedKeys().isEmpty()) {
      try (FieldSet output = new FieldSet(generator)) {
        output
                .withCustomClass("qe-filters")
                .withLegend("Filters");
        try (ComponentArray a = output.addComponents()) {
          buildFiltersDataGrid(generator, pipeline);
        }
      }
    }
  }
  
  void buildFiltersDataGrid(JsonGenerator generator, PipelineFile pipeline) throws IOException {
    try (DataGrid output = new DataGrid(generator)) {
      output.withCustomClass("qe-filters-datagrid")
              .withKey("_filters")
              .withHideLabel(Boolean.TRUE)
              .withInitEmpty(Boolean.TRUE)
              .withAddAnother("Add Filter")
              ;
      try (ComponentArray a = output.addComponents()) {
        buildFiltersFilterSelect(generator, pipeline);
        buildFiltersValue(generator, pipeline);
      }
    }
  }
  
  void buildFiltersFilterSelect(JsonGenerator generator, PipelineFile pipeline) throws IOException {
    try (Select select = new Select(generator)) {
      select 
            .withDescription(null)
            .withKey("filter")
            .withCustomClass("qe-filter-select")
            .withHideLabel(Boolean.TRUE)
            .withClearOnHide(false)
            .withDefaultValue(filterFactory.getSortedKeys().get(0))
            .withSearchEnabled(Boolean.FALSE)
      ;
      
      try (Select.SelectValidation v = select.addValidate()) {
        v.withOnlyAvailableItems(Boolean.TRUE);
      }

      try (Select.DataValues dv = select.addDataValues()) {        
        try (ComponentArray a = dv.addValues()) {
          for (String filter : filterFactory.getSortedKeys()) {
            try (Select.DataValue value = new Select.DataValue(generator)) {
              value
                      .withLabel(filter)
                      .withValue(filter)
                       ;
            }
          }
        }
      }
    }
  }
  
  void buildFiltersValue(JsonGenerator generator, PipelineFile pipeline) throws IOException {
    try (TextField textField = new TextField(generator)) {
      textField
              .withHideLabel(Boolean.TRUE)
              .withKey("value")
              ;
    }
  }
  
  void buildOutput(JsonGenerator generator, PipelineFile pipeline) throws IOException {
    try (FieldSet output = new FieldSet(generator)) {
      output.withCustomClass("qe-output");
      output.withLegend("Output");
      try (ComponentArray a = output.addComponents()) {
        buildOutputSelect(generator, pipeline);
      }
    }
  }
  
  void buildOutputSelect(JsonGenerator generator, PipelineFile pipeline) throws IOException {
    try (Select select = new Select(generator)) {
      select 
            .withDescription(null)
            .withKey("_fmt")
            .withClearOnHide(false)
            .withDefaultValue(pipeline.getDestinations().get(0).getName())
      ;
      try (Select.SelectValidation v = select.addValidate()) {
        v.withOnlyAvailableItems(Boolean.TRUE);
        v.withRequired(Boolean.TRUE);
      }

      try (Select.DataValues dv = select.addDataValues()) {
        try (ComponentArray a = dv.addValues()) {
          for (Format f : pipeline.getDestinations()) {
            try (Select.DataValue value = new Select.DataValue(generator)) {
              value
                      .withLabel(f.getName())
                      .withValue(f.getName())
                       ;
            }
          }
        }
      }
    }
  }

  void buildButtons(JsonGenerator generator, PipelineFile pipeline) throws IOException {
    try (FieldSet fs = new FieldSet(generator)) {
      try (ComponentArray a1 = fs.addComponents()) {
        try (Columns columns = new Columns(generator)) {
          try (ComponentArray a2 = columns.addColumns()) {
            try (Columns.Column col = new Columns.Column(generator)) {
              col.withSize("md");
              col.withWidth(12);
              try (ComponentArray a3 = col.addComponents()) {
                try (Button b = new Button(generator)) {
                  b
                          .withCustomClass("float-left qe_formio_submit_button")
                          .withLabel("Submit")
                          .withKey("_submit")
                          .withDisableOnInvalid(true)
                          .withAction(Button.ActionType.submit)
                          ;
                }
                try (Button b = new Button(generator)) {
                  b
                          .withCustomClass("float-left qe_formio_cancel_button")
                          .withLabel("Cancel")
                          .withKey("_cancel")
                          .withDisableOnInvalid(false)
                          .withTheme("secondary")
                          .withAction(Button.ActionType.event)
                        ;
                }                      
              }
            }
          }
        }
      }
    }
  }
  
  static LocalDateTime parseToLocalDateTime(String value) {
    if (Strings.isNullOrEmpty(value)) {
      return null;
    }
    try {
      return LocalDateTime.parse(value);
    } catch (DateTimeParseException ex) {
      try {
        LocalDate ld = LocalDate.parse(value);
        return ld.atTime(0, 0);
      } catch (DateTimeParseException ex1) {
        try {
          LocalTime lt = LocalTime.parse(value);
          return lt.atDate(LocalDate.ofEpochDay(0));
        } catch (DateTimeParseException ex2) {
          logger.warn("Cannot parse \"{}\" as LocalDateTime, LocalDate or LocalTime", value);
          return null;
        }
      }
    }
  }
  
  void buildCheckBox(JsonGenerator generator, Argument arg) throws IOException {
    try (CheckBox checkbox = new CheckBox(generator)) {
      checkbox
            .withLabel(arg.getTitle())
            .withPlaceholder(arg.getPrompt())
            .withDescription(arg.getDescription())
            .withKey(arg.getName())
            .withMultiple(arg.isMultiValued())
            ;
      if (!Strings.isNullOrEmpty(arg.getDefaultValueExpression())) {
        JexlEvaluator evaluator = new JexlEvaluator(arg.getDefaultValueExpression());
        Object result = evaluator.evaluateAsObject(requestContext, null);
        if (result != null) {
          try {
            Boolean defaultBoolean = (Boolean) DataType.Boolean.cast(result);
            if (defaultBoolean != null) {
              checkbox.withDefaultValue(defaultBoolean.toString());
            }
          } catch (Throwable ex) {
            checkbox.withDefaultValue(result.toString());
          }
        }        
      }
      
      try (Validation v = checkbox.addValidate()) {
        v.withRequired(!arg.isOptional());
      }
    }    
  }
  
  void buildDateTime(JsonGenerator generator, Argument arg) throws IOException {    
    try (DateTime dateTime = new DateTime(generator)) {
      dateTime
              .withLabel(arg.getTitle())
              .withDescription(arg.getDescription())
              .withPlaceholder(arg.getPrompt())
              .withKey(arg.getName())
              .withMultiple(arg.isMultiValued())
              .withEnableDate(arg.getType() == DataType.Date || arg.getType() == DataType.DateTime)
              .withEnableTime(arg.getType() == DataType.Time || arg.getType() == DataType.DateTime)              
            ;
      if (!Strings.isNullOrEmpty(arg.getDefaultValueExpression())) {
        JexlEvaluator evaluator = new JexlEvaluator(arg.getDefaultValueExpression());
        Object result = evaluator.evaluateAsObject(requestContext, null);
        if (result != null) {
          if (arg.getType() == DataType.Date) {
            try {
              LocalDate defaultDate = (LocalDate) DataType.Date.cast(result);
              dateTime.withDefaultDate(defaultDate);
            } catch (Throwable ex) {
              dateTime.withDefaultValue(result.toString());
            }
          } else if (arg.getType() == DataType.DateTime) {
            try {
              LocalDateTime defaultDateTime = (LocalDateTime) DataType.DateTime.cast(result);
              dateTime.withDefaultDate(defaultDateTime);
            } catch (Throwable ex) {
              dateTime.withDefaultValue(result.toString());
            }
          } else if (arg.getType() == DataType.Time) {
            try {
              LocalTime defaultTime = (LocalTime) DataType.Time.cast(result);
              dateTime.withDefaultDate(defaultTime);
            } catch (Throwable ex) {
              dateTime.withDefaultValue(result.toString());
            }
          }
        }
      }
      
      try (Validation v = dateTime.addValidate()) {
        v.withRequired(!arg.isOptional());
      }
      
      try (DatePicker dp = dateTime.addDatePicker()) {
        dp.withMaxDate(parseToLocalDateTime(arg.getMaximumValue()));
        dp.withMinDate(parseToLocalDateTime(arg.getMinimumValue()));
      }
    }
  }

  void buildNumber(JsonGenerator generator, Argument arg) throws IOException {
    try (Number number = new Number(generator)) {
      number
            .withLabel(arg.getTitle())
            .withDescription(arg.getDescription())
            .withPlaceholder(arg.getPrompt())
            .withKey(arg.getName())
            .withMultiple(arg.isMultiValued())
            ;
      if (!Strings.isNullOrEmpty(arg.getDefaultValueExpression())) {
        JexlEvaluator evaluator = new JexlEvaluator(arg.getDefaultValueExpression());
        Object result = evaluator.evaluateAsObject(requestContext, null);
        if (result != null) {
          if (arg.getType() == DataType.Double) {
            try {
              Double defaultNum = (Double) DataType.Double.cast(result);
              number.withDefaultValue(defaultNum.toString());
            } catch (Throwable ex) {
              number.withDefaultValue(result.toString());
            }
          } else if (arg.getType() == DataType.Float) {
            try {
              Float defaultNum = (Float) DataType.Float.cast(result);
              number.withDefaultValue(defaultNum.toString());
            } catch (Throwable ex) {
              number.withDefaultValue(result.toString());
            }
          } else if (arg.getType() == DataType.Integer) {
            try {
              Integer defaultNum = (Integer) DataType.Integer.cast(result);
              number.withDefaultValue(defaultNum.toString());
            } catch (Throwable ex) {
              number.withDefaultValue(result.toString());
            }
          } else if (arg.getType() == DataType.Long) {
            try {
              Long defaultNum = (Long) DataType.Long.cast(result);
              number.withDefaultValue(defaultNum.toString());
            } catch (Throwable ex) {
              number.withDefaultValue(result.toString());
            }
          }
        }        
      }
      
      try (Number.NumberValidation v = number.addNumberValidate()) {
        v
                .withInteger(arg.getType() != DataType.Double && arg.getType() != DataType.Float)
                .withRequired(!arg.isOptional())
                ;
        if (arg.getMaximumValue() != null) {
          try {
            java.lang.Number maxValue = parseNumber(arg.getType(), arg.getMaximumValue());
            if (maxValue != null) {
              v.withMax(maxValue);
            }
          } catch (NumberFormatException ex) {
            logger.warn("Unable to parse {} as a {}: ", arg.getMaximumValue(), arg.getType(), ex);
          }
        }
        if (arg.getMinimumValue() != null) {
          try {
            java.lang.Number minValue = parseNumber(arg.getType(), arg.getMaximumValue());
            if (minValue != null) {
              v.withMin(minValue);
            }
          } catch (NumberFormatException ex) {
            logger.warn("Unable to parse {} as a {}: ", arg.getMaximumValue(), arg.getType(), ex);
          }
        }
      }
    }
  }
  
  static java.lang.Number parseNumber(DataType type, String value) {
    if (Strings.isNullOrEmpty(value)) {
      return null;
    }
    switch (type) {
      case Double:
        return Double.valueOf(value);
      case Integer:
        return Integer.valueOf(value);
      case Long: 
        return Long.valueOf(value);
      default:
        return null;
    }
  }

  void buildTextField(JsonGenerator generator, Argument arg) throws IOException {
    try (TextField textField = new TextField(generator)) {
      textField
            .withLabel(arg.getTitle())
            .withPlaceholder(arg.getPrompt())
            .withDescription(arg.getDescription())
            .withKey(arg.getName())
            .withMultiple(arg.isMultiValued())
            ;
      if (!Strings.isNullOrEmpty(arg.getDefaultValueExpression())) {
        JexlEvaluator evaluator = new JexlEvaluator(arg.getDefaultValueExpression());
        Object result = evaluator.evaluateAsObject(requestContext, null);
        if (result != null) {
          try {
            String defaultString = (String) DataType.String.cast(result);
            textField.withDefaultValue(defaultString);
          } catch (Throwable ex) {
            textField.withDefaultValue(result.toString());
          }
        }        
      }
      
      try (Validation v = textField.addValidate()) {
        v.withRequired(!arg.isOptional());
      }
    }
  }

  void buildSelect(JsonGenerator generator, Argument arg) throws IOException {
    try (Select select = new Select(generator)) {
      select 
            .withLabel(arg.getTitle())
            .withDescription(arg.getDescription())
            .withPlaceholder(arg.getPrompt())
            .withKey(arg.getName())
            .withMultiple(arg.isMultiValued())
            .withValueProperty("value")
            ;
      try (Validation v = select.addValidate()) {
        v.withRequired(!arg.isOptional());
      }
      if (!Strings.isNullOrEmpty(arg.getDefaultValueExpression())) {
        JexlEvaluator evaluator = new JexlEvaluator(arg.getDefaultValueExpression());
        Object result = evaluator.evaluateAsObject(requestContext, null);
        if (result != null) {
          try {
            String defaultString = (String) DataType.String.cast(result);
            select.withDefaultValue(defaultString);
          } catch (Throwable ex) {
            select.withDefaultValue(result.toString());
          }
        }        
      }
      if (isNullOrEmpty(arg.getPossibleValues())) {
        try (Select.DataUrl url = select.addDataUrl()) {
          url.withUrl(arg.getPossibleValuesUrl());
        }
      } else {
        try (Select.DataValues dv = select.addDataValues()) {
          try (ComponentArray a = dv.addValues()) {
            for (ArgumentValue av : arg.getPossibleValues()) {
              try (Select.DataValue value = new Select.DataValue(generator)) {
                if (Strings.isNullOrEmpty(av.getLabel())) {
                  value.withLabel(av.getValue());
                } else {
                  value.withLabel(av.getLabel());
                }
                value.withValue(av.getValue());
              }
            }
          }
        }
      }
    }
  }
}
