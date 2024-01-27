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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.defn.ArgumentType;
import uk.co.spudsoft.query.defn.ArgumentValue;
import uk.co.spudsoft.query.defn.FormatJson;
import uk.co.spudsoft.query.defn.FormatXlsx;
import uk.co.spudsoft.query.exec.FilterFactory;
import uk.co.spudsoft.query.pipeline.PipelineNodesTree;
import uk.co.spudsoft.query.pipeline.PipelineNodesTree.PipelineFile;

/**
 *
 * @author njt
 */
public class FormBuilderTest {
  
  private static final Logger logger = LoggerFactory.getLogger(FormBuilderTest.class);
  
  
  @Test
  public void testIsNullOrEmpty() {
    assertTrue(FormBuilder.isNullOrEmpty(null));
    assertTrue(FormBuilder.isNullOrEmpty(Arrays.asList()));
    assertFalse(FormBuilder.isNullOrEmpty(Arrays.asList(1)));
  }  
  
  @Test
  public void testParseToLocalDateTime() {
    assertNull(FormBuilder.parseToLocalDateTime(null));
    assertEquals("2023-10-02T23:45", FormBuilder.parseToLocalDateTime("2023-10-02T23:45").toString());
    assertEquals("2023-10-01T00:00", FormBuilder.parseToLocalDateTime("2023-10-01").toString());
    assertEquals("2023-10-01T00:00", FormBuilder.parseToLocalDateTime("2023-10-01").toString());
    assertEquals("1970-01-01T12:34", FormBuilder.parseToLocalDateTime("12:34").toString());
    assertEquals("1970-01-01T12:34:56", FormBuilder.parseToLocalDateTime("12:34:56").toString());
  }
  
  @Test
  public void testParseNumber() {
    assertEquals(1.2, FormBuilder.parseNumber(ArgumentType.Double, "1.2"));
    assertEquals(1L, FormBuilder.parseNumber(ArgumentType.Long, "1"));
    assertEquals(1, FormBuilder.parseNumber(ArgumentType.Integer, "1"));
    assertNull(FormBuilder.parseNumber(ArgumentType.String, "1.2"));
    assertNull(FormBuilder.parseNumber(ArgumentType.Integer, null));
    assertNull(FormBuilder.parseNumber(ArgumentType.Integer, ""));
  }
  
  @Test
  public void testBuildForm() throws Exception {
    PipelineFile file = new PipelineNodesTree.PipelineFile("path", "Title", "description"
            , Arrays.asList(
                    Argument.builder()
                            .name("arg1")
                            .title("First Arg")
                            .description("The first argument")
                            .permittedValuesRegex("A.*Z")
                            .maximumValue("x")
                            .minimumValue("z")
                            .type(ArgumentType.String)
                            .build()
                    ,
                    Argument.builder()
                            .name("arg1")
                            .title("First Arg")
                            .description("The first argument")
                            .maximumValue("1")
                            .minimumValue("12")
                            .type(ArgumentType.Double)
                            .build()
                    ,
                    Argument.builder()
                            .name("arg1")
                            .title("First Arg")
                            .description("The first argument")
                            .permittedValuesRegex("A.*Z")
                            .maximumValue("1")
                            .minimumValue("12")
                            .type(ArgumentType.Integer)
                            .build()
                    ,
                    Argument.builder()
                            .name("arg1")
                            .title("First Arg")
                            .description("The first argument")
                            .permittedValuesRegex("A.*Z")
                            .maximumValue("1")
                            .minimumValue("12")
                            .type(ArgumentType.Long)
                            .build()
                    ,
                    Argument.builder()
                            .name("arg2")
                            .title("Second Arg")
                            .description("The second argument")
                            .minimumValue("2023-10-01")
                            .type(ArgumentType.Date)
                            .build()
                    ,
                    Argument.builder()
                            .name("arg2")
                            .title("Second Arg")
                            .description("The second argument")
                            .minimumValue("2023-10-01")
                            .type(ArgumentType.DateTime)
                            .build()
                    ,
                    Argument.builder()
                            .name("arg2")
                            .title("Second Arg")
                            .description("The second argument")
                            .maximumValue("13:56")
                            .type(ArgumentType.Time)
                            .optional(true)
                            .build()
                    ,
                    Argument.builder()
                            .name("arg2")
                            .title("Second Arg")
                            .description("The second argument")
                            .type(ArgumentType.String)
                            .possibleValues(
                                    Arrays.asList(
                                            ArgumentValue.builder().label("one").value("One").build()
                                            , ArgumentValue.builder().label("two").value("Two").build()
                                            , ArgumentValue.builder().label("three").value("Three").build()
                                    )
                            )
                            .multiValued(true)
                            .optional(false)
                            .build()
            )
            , Arrays.asList(
                    FormatXlsx.builder().build()
                    , FormatJson.builder().build()
            )
    );
    
    FormBuilder fb = new FormBuilder(3, new FilterFactory(Collections.emptyList()));
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    fb.buildForm(file, baos);
    baos.close();
    byte[] output = baos.toByteArray();

    String result = new String(output, StandardCharsets.UTF_8);
    logger.debug("Result: {}", result);
    
    assertThat(result.length(), greaterThan(100));
    assertThat(result, containsString("<h2>Title</h2>"));
    assertThat(result, containsString("<p>description</p>"));
  }

}
