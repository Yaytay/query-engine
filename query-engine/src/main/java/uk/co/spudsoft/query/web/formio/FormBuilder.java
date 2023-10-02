/*
 * Copyright (C) 2023 njt
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.defn.ArgumentType;
import uk.co.spudsoft.query.defn.ArgumentValue;
import uk.co.spudsoft.query.defn.Format;
import uk.co.spudsoft.query.pipeline.PipelineNodesTree.PipelineFile;

/**
 *
 * @author njt
 */
public class FormBuilder {
  
  private final ObjectMapper objectMapper;

  public FormBuilder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }
  
  private static boolean isNullOrEmpty(Collection<?> collection) {
    return collection == null || collection.isEmpty();
  }
  
  public static Form buildForm(PipelineFile pipeline) {
    
    Form f = new Form()
            .withName(pipeline.getName())
            .withTitle(pipeline.getTitle())
            .withPath(pipeline.getPath())
            .withDisplay("form")
            .withComponents(
                    Arrays.asList(
                            buildDescription(pipeline)
                            , buildArguments(pipeline)
                            , buildOutput(pipeline)
                            , buildButtons(pipeline)
                    )
            )
            ;
    
    return f;
  }
  
  static Content buildDescription(PipelineFile pipeline) {
    Content description = new Content()
            .withHtml("<p><h2>" + pipeline.getTitle() + "</h2></p><p>" + pipeline.getDescription() + "</p>")
            .withCustomClass("border-bottom")
            ;
    return description;
  }
  
  static FieldSet buildArguments(PipelineFile pipeline) {
    List<Component<?>> args = new ArrayList<>();
    for (Argument arg : pipeline.getArguments()) {
      switch(arg.getType()) {
        case Date:
        case Time:
        case DateTime:
          args.add(buildDateTime(arg));
          break;
        case Double:
        case Integer:
        case Long:
          args.add(buildNumber(arg));
          break;
        case String:
          if (!isNullOrEmpty(arg.getPossibleValues()) || !Strings.isNullOrEmpty(arg.getPossibleValuesUrl())) {
            args.add(buildSelect(arg));
          } else {
            args.add(buildTextField(arg));
          }
          break;
      }
    }
    FieldSet argSet = new FieldSet()
            .withLegend("Arguments")
            .withCustomClass("border-bottom")
            .withComponents(args)
            ;
    return argSet;
  }
  
  static FieldSet buildOutput(PipelineFile pipeline) {
    FieldSet output = new FieldSet()
            .withLegend("Output")
            .withComponents(
                    Arrays.asList(
                            buildOutputSelect(pipeline)
                    )
            )
            ;
    return output;    
  }
  
  static Select buildOutputSelect(PipelineFile pipeline) {
    Select select = new Select()
            .withDescription(null)
            .withKey("format")
            .withClearOnHide(false)
            .withValidate(new Validation().withRequired(Boolean.TRUE))
            ;            

    List<Select.DataValue> values = new ArrayList<>();
    for (Format f : pipeline.getDestinations()) {
      values.add(new Select.DataValue().withLabel(f.getName()).withValue(f.getName()));
    }
    select.withDataSrc(Select.DataSrcType.values).withData(values);

    return select;        
  }

  static Component<?> buildButtons(PipelineFile pipeline) {
    FieldSet fs = new FieldSet()
            .withComponents(
                    Arrays.asList(
                            new Columns()
                                    .withColumns(
                                            Arrays.asList(
                                                    new Columns.Column()
                                                            .withComponents(
                                                                    Arrays.asList(
                                                                            new Button()
                                                                                    .withLabel("Submit")
                                                                                    .withKey("submit")
                                                                                    .withDisableOnInvalid(true)
                                                                                    .withAction(Button.ActionType.submit)
                                                                    )
                                                            )
                                                            .withSize("xs"),
                                                     new Columns.Column()
                                                            .withComponents(
                                                                    Arrays.asList(
                                                                            new Button()
                                                                                    .withLabel("Cancel")
                                                                                    .withKey("cancel")
                                                                                    .withDisableOnInvalid(false)
                                                                                    .withTheme("secondary")
                                                                                    .withAction(Button.ActionType.event)
                                                                    )
                                                            )
                                                            .withSize("xs")
                                            )
                                    )
                    )
            );
    return fs;
  }
  
  static DateTime buildDateTime(Argument arg) {
    DateTime dateTime = new DateTime()
            .withLabel(arg.getTitle())
            .withDescription(arg.getDescription())
            .withKey(arg.getName())
            .withMultiple(arg.isMultiValued())
            .withValidate(new Validation().withRequired(!arg.isOptional()))
            .withEnableDate(arg.getType() == ArgumentType.Date || arg.getType() == ArgumentType.DateTime)
            .withEnableTime(arg.getType() == ArgumentType.Time || arg.getType() == ArgumentType.DateTime)
            ;
    return dateTime;
  }

  static Number buildNumber(Argument arg) {
    Number number = new Number()
            .withLabel(arg.getTitle())
            .withDescription(arg.getDescription())
            .withKey(arg.getName())
            .withMultiple(arg.isMultiValued())
            .withValidate(
                    new Number.NumberValidation()
                            .withInteger(arg.getType() != ArgumentType.Double)
                            .withRequired(!arg.isOptional())
            )
            ;
    return number;    
  }

  static TextField buildTextField(Argument arg) {
    TextField textField = new TextField()
            .withLabel(arg.getTitle())
            .withDescription(arg.getDescription())
            .withKey(arg.getName())
            .withMultiple(arg.isMultiValued())
            .withValidate(
                    new Validation()
                            .withRequired(!arg.isOptional())
            )
            ;
    return textField;    
  }

  static Select buildSelect(Argument arg) {
    Select select = new Select()
            .withLabel(arg.getTitle())
            .withDescription(arg.getDescription())
            .withKey(arg.getName())
            .withMultiple(arg.isMultiValued())
            ;
    if (isNullOrEmpty(arg.getPossibleValues())) {
      Select.DataUrl url = new Select.DataUrl()
              .withUrl(arg.getPossibleValuesUrl());
      select.withDataSrc(Select.DataSrcType.url).withData(url);
    } else {
      List<Select.DataValue> values = new ArrayList<>();
      for (ArgumentValue av : arg.getPossibleValues()) {
        values.add(new Select.DataValue().withLabel(av.getLabel()).withValue(av.getValue()));
      }
      select.withDataSrc(Select.DataSrcType.values).withData(values);
    }
    return select;    
  }
}
