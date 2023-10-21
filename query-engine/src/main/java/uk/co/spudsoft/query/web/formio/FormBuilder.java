/*
 * Copyright (C) 2023 jtalbut
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
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.defn.ArgumentType;
import uk.co.spudsoft.query.defn.ArgumentValue;
import uk.co.spudsoft.query.defn.Format;
import uk.co.spudsoft.query.pipeline.PipelineNodesTree.PipelineFile;
import uk.co.spudsoft.query.web.formio.DateTime.DatePicker;

/**
 *
 * @author jtalbut
 */
@SuppressWarnings("try")
public class FormBuilder {
  
  private static final Logger logger = LoggerFactory.getLogger(FormBuilder.class);
  
  private final JsonFactory factory;
  private final int columns;
  
  public FormBuilder(int columns) {
    this.factory = new JsonFactory();
    this.columns = columns;
  }
  
  static boolean isNullOrEmpty(Collection<?> collection) {
    return collection == null || collection.isEmpty();
  }
  
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
          buildOutput(generator, pipeline);
          buildButtons(generator, pipeline);
        }
      }
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
  
  void buildArguments(JsonGenerator generator, PipelineFile pipeline) throws IOException {
    List<Argument> args = pipeline.getArguments();
    if (args.isEmpty()) {
      return ;
    }
    
    int fieldsPerColumn = Math.max(1, (int) Math.ceil((double)args.size() / columns));
    
    try (FieldSet fieldSet = new FieldSet(generator)) {
      fieldSet
            .withLegend("Arguments")
            .withCustomClass("qe-arguments border-bottom");

      try (ComponentArray fieldSetArray = fieldSet.addComponents()) {
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
    }
  }

  private void buildArgument(Argument arg, JsonGenerator generator) throws IOException, IllegalStateException {
    switch (arg.getType()) {
      case Date:
      case Time:
      case DateTime:
        buildDateTime(generator, arg);
        break;
      case Double:
      case Integer:
      case Long:
        buildNumber(generator, arg);
        break;
      case String:
        if (!isNullOrEmpty(arg.getPossibleValues()) || !Strings.isNullOrEmpty(arg.getPossibleValuesUrl())) {
          buildSelect(generator, arg);
        } else {
          buildTextField(generator, arg);
        }
        break;
      default:
        throw new IllegalStateException("New types added to ArgumentType and not implemented here");
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
            .withKey("format")
            .withClearOnHide(false)
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
              col.withSize("xs");
              try (ComponentArray a3 = col.addComponents()) {
                try (Button b = new Button(generator)) {
                  b
                        .withLabel("Submit")
                        .withKey("submit")
                        .withDisableOnInvalid(true)
                        .withAction(Button.ActionType.submit)
                        ;
                }                      
              }
            }
            try (Columns.Column col = new Columns.Column(generator)) {
              col.withSize("xs");
              try (ComponentArray a3 = col.addComponents()) {
                try (Button b = new Button(generator)) {
                  b
                        .withLabel("Cancel")
                        .withKey("cancel")
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
  
  void buildDateTime(JsonGenerator generator, Argument arg) throws IOException {    
    try (DateTime dateTime = new DateTime(generator)) {
      dateTime
              .withLabel(arg.getTitle())
              .withDescription(arg.getDescription())
              .withPlaceholder(arg.getPrompt())
              .withKey(arg.getName())
              .withMultiple(arg.isMultiValued())
              .withEnableDate(arg.getType() == ArgumentType.Date || arg.getType() == ArgumentType.DateTime)
              .withEnableTime(arg.getType() == ArgumentType.Time || arg.getType() == ArgumentType.DateTime)
            ;
      
      
      
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
      try (Number.NumberValidation v = number.addNumberValidate()) {
        v
                .withInteger(arg.getType() != ArgumentType.Double)
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
  
  static java.lang.Number parseNumber(ArgumentType type, String value) {
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
            ;
      try (Validation v = select.addValidate()) {
        v.withRequired(!arg.isOptional());
      }
      if (isNullOrEmpty(arg.getPossibleValues())) {
        try (Select.DataUrl url = select.addDataUrl()) {
          url.withUrl(arg.getPossibleValuesUrl());
        }
      } else {
        try (Select.DataValues dv = select.addDataValues()) {
          try (ComponentArray a = dv.addValues()) {
            for (ArgumentValue av : arg.getPossibleValues()) {
              select.addCompleteDataValue(av.getLabel(), av.getValue());
            }
          }
        }
      }
    }
  }
}
