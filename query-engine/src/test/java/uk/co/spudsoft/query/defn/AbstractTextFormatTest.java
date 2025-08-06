/*
 * Copyright (C) 2025 jtalbut
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

import com.google.common.net.MediaType;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.co.spudsoft.query.exec.FormatInstance;
import uk.co.spudsoft.query.exec.context.RequestContext;

/**
 * Test class for AbstractTextFormat.
 *
 * @author jtalbut
 */
public class AbstractTextFormatTest {

  /**
   * Test implementation of AbstractTextFormat for testing purposes.
   */
  public static class AbstractTextFormatImpl extends AbstractTextFormat {

    public AbstractTextFormatImpl(Builder builder) {
      super(builder);
    }

    @Override
    public void validate() {
      validate(FormatType.Delimited, null, null);
    }

    /**
     * Builder implementation for AbstractTextFormatImpl.
     */
    public static class Builder extends AbstractTextFormat.Builder<Builder> {

      public Builder() {
        super(FormatType.Delimited, null, null, null, null, MediaType.BMP, false
                , null, null, null, null, null);
      }

      public AbstractTextFormatImpl build() {
        return new AbstractTextFormatImpl(this);
      }
    }

    public static Builder builder() {
      return new Builder();
    }

    @Override
    public FormatInstance createInstance(Vertx vertx, RequestContext requestContext, WriteStream<Buffer> writeStream) {
      throw new UnsupportedOperationException("Not supported yet.");
    }
    
  }

  /**
   * Create a builder with sample values for testing.
   */
  private AbstractTextFormatImpl.Builder createBuilderWithValues() {
    AbstractTextFormatImpl.Builder builder = AbstractTextFormatImpl.builder();
    builder.type(FormatType.Delimited)
      .name("TestFormat")
      .description("Test description")
      .extension(".test")
      .filename("test-file")
      .mediaType(MediaType.PLAIN_TEXT_UTF_8.toString())
      .hidden(false)
      .dateFormat("yyyy/MM/dd")
      .dateTimeFormat("yyyy/MM/dd HH:mm:ss")
      .timeFormat("HH:mm:ss")
      .decimalFormat("#,##0.00")
      .booleanFormat("['\"Yes\"', '\"No\"']");
    return builder;
  }

  @Test
  public void testConstructorAndGetters() {
    AbstractTextFormatImpl.Builder builder = createBuilderWithValues();
    List<ColumnTextFormats> columnFormats = Arrays.asList(
      ColumnTextFormats.builder().column("col1").booleanFormat("T|F").build(),
      ColumnTextFormats.builder().column("col2").dateFormat("dd-MM-yyyy").build()
    );
    builder.columnSpecificTextFormats(columnFormats);

    AbstractTextFormatImpl format = builder.build();

    // Test all getters
    assertEquals(FormatType.Delimited, format.getType());
    assertEquals("TestFormat", format.getName());
    assertEquals("Test description", format.getDescription());
    assertEquals(".test", format.getExtension());
    assertEquals("test-file", format.getFilename());
    assertEquals(MediaType.PLAIN_TEXT_UTF_8, format.getMediaType());
    assertFalse(format.isHidden());
    assertEquals("yyyy/MM/dd", format.getDateFormat());
    assertEquals("yyyy/MM/dd HH:mm:ss", format.getDateTimeFormat());
    assertEquals("HH:mm:ss", format.getTimeFormat());
    assertEquals("#,##0.00", format.getDecimalFormat());
    assertEquals("['\"Yes\"', '\"No\"']", format.getBooleanFormat());
    assertEquals(2, format.getColumnSpecificTextFormats().size());
  }

  @Test
  public void testValidate() {
    // Test normal validation
    AbstractTextFormatImpl format = createBuilderWithValues().build();
    format.validate();

    // Test validation with wrong format type
    AbstractTextFormatImpl formatWithWrongType = createBuilderWithValues()
      .type(FormatType.JSON)
      .build();

    IllegalArgumentException ex = assertThrows(
      IllegalArgumentException.class,
      formatWithWrongType::validate);
    assertThat(ex.getMessage(), containsString("Format of type Delimited configured with type JSON"));
    
    // Test validation with wrong format type
    AbstractTextFormatImpl formatWithDuplicateCustomColumns = createBuilderWithValues()
      .columnSpecificTextFormats(
              Arrays.asList(
                            ColumnTextFormats.builder().column("first").decimalFormat("0.000").build()
                            , ColumnTextFormats.builder().column("second").decimalFormat("0.000").build()
                            , ColumnTextFormats.builder().column("second").decimalFormat("0.000").build()
              )
      )
      .build();

    ex = assertThrows(
      IllegalArgumentException.class,
      formatWithDuplicateCustomColumns::validate);
    assertThat(ex.getMessage(), containsString("At least two column-specific text formats (2 and 1) have the same name (\"second\")"));
    
    // Test validation with bad CTF
    AbstractTextFormatImpl formatWithBadCustomColumn = createBuilderWithValues()
      .columnSpecificTextFormats(
              Arrays.asList(
                            ColumnTextFormats.builder().column("first").decimalFormat("0.000").build()
                            , ColumnTextFormats.builder().column("second").build()
                            , ColumnTextFormats.builder().column("third").decimalFormat("0.000").build()
              )
      )
      .build();

    ex = assertThrows(
      IllegalArgumentException.class,
      formatWithBadCustomColumn::validate);
    assertThat(ex.getMessage(), containsString("No formats specified for column \"second\""));
    
  }

  @Test
  public void testGetDateFormat() {
    AbstractTextFormatImpl format = createBuilderWithValues().dateFormat("MM-dd-yyyy").build();
    assertEquals("MM-dd-yyyy", format.getDateFormat());

    // Test with null value
    format = createBuilderWithValues().dateFormat(null).build();
    assertNull(format.getDateFormat());
  }

  @Test
  public void testGetDateTimeFormat() {
    AbstractTextFormatImpl format = createBuilderWithValues().dateTimeFormat("MM-dd-yyyy HH:mm").build();
    assertEquals("MM-dd-yyyy HH:mm", format.getDateTimeFormat());

    // Test with null value
    format = createBuilderWithValues().dateTimeFormat(null).build();
    assertNull(format.getDateTimeFormat());
  }

  @Test
  public void testGetTimeFormat() {
    AbstractTextFormatImpl format = createBuilderWithValues().timeFormat("HH.mm.ss").build();
    assertEquals("HH.mm.ss", format.getTimeFormat());

    // Test with null value
    format = createBuilderWithValues().timeFormat(null).build();
    assertNull(format.getTimeFormat());
  }

  @Test
  public void testGetDecimalFormat() {
    AbstractTextFormatImpl format = createBuilderWithValues().decimalFormat("0.000").build();
    assertEquals("0.000", format.getDecimalFormat());

    // Test with null value
    format = createBuilderWithValues().decimalFormat(null).build();
    assertNull(format.getDecimalFormat());
  }

  @Test
  public void testGetBooleanFormat() {
    AbstractTextFormatImpl format = createBuilderWithValues().booleanFormat("TRUE|FALSE").build();
    assertEquals("TRUE|FALSE", format.getBooleanFormat());

    // Test with null value
    format = createBuilderWithValues().booleanFormat(null).build();
    assertNull(format.getBooleanFormat());
  }

  @Test
  public void testGetColumnSpecificTextFormats() {
    List<ColumnTextFormats> columnFormats = Arrays.asList(
      ColumnTextFormats.builder().column("col1").build(),
      ColumnTextFormats.builder().column("col2").build()
    );

    AbstractTextFormatImpl format = createBuilderWithValues()
      .columnSpecificTextFormats(columnFormats)
      .build();

    assertEquals(columnFormats, format.getColumnSpecificTextFormats());

    // Test with null value
    format = createBuilderWithValues().columnSpecificTextFormats(null).build();
    assertNotNull(format.getColumnSpecificTextFormats());
    assertEquals(0, format.getColumnSpecificTextFormats().size());

    // Test with empty list
    format = createBuilderWithValues().columnSpecificTextFormats(Collections.emptyList()).build();
    assertTrue(format.getColumnSpecificTextFormats().isEmpty());
  }

  @Test
  public void testValidateWithBooleanQuotes() {
    // Create a custom implementation that uses quotes for boolean validation
    AbstractTextFormat customFormat = new AbstractTextFormat(createBuilderWithValues()) {
      @Override
      public void validate() {
        validate(FormatType.Delimited, "\"", "\"");
      }
      @Override
      public FormatInstance createInstance(Vertx vertx, RequestContext requestContext, WriteStream<Buffer> writeStream) {
        throw new UnsupportedOperationException("Not supported yet.");
      }
    };

    // This should pass with no exception
    customFormat.validate();

    // Test invalid boolean format with quotes
    AbstractTextFormat invalidFormat = new AbstractTextFormat(createBuilderWithValues().booleanFormat("\"Yes\"|No")) {
      @Override
      public void validate() {
        validate(FormatType.Delimited, "\"", "\"");
      }
      @Override
      public FormatInstance createInstance(Vertx vertx, RequestContext requestContext, WriteStream<Buffer> writeStream) {
        throw new UnsupportedOperationException("Not supported yet.");
      }
    };

    assertThat(
            assertThrows(IllegalArgumentException.class
                    , invalidFormat::validate
            ).getMessage()
            , containsString("Invalid booleanFormat")
    );
  }

  @ParameterizedTest
  @CsvSource({
    "true, Delimited, true",
    "true, Delimited, false",
    "false, JSON, false"
  })
  public void testValidateWithTypeAndHidden(boolean expectedToPass, FormatType type, boolean hidden) {
    AbstractTextFormat format = new AbstractTextFormat(createBuilderWithValues().type(type).hidden(hidden)) {
      @Override
      public void validate() {
        validate(FormatType.Delimited, null, null);
      }
      @Override
      public FormatInstance createInstance(Vertx vertx, RequestContext requestContext, WriteStream<Buffer> writeStream) {
        throw new UnsupportedOperationException("Not supported yet.");
      }
    };

    if (expectedToPass) {
      format.validate(); // Should not throw exception
    } else {
      assertThrows(IllegalArgumentException.class, format::validate);
    }
  }

  @Test
  public void testBuilderFluentInterface() {
    // Test that all builder methods return the builder instance for fluent chaining
    AbstractTextFormatImpl.Builder builder = AbstractTextFormatImpl.builder();

    // Test all methods return the same builder
    assertSame(builder, builder.dateFormat("test"));
    assertSame(builder, builder.dateTimeFormat("test"));
    assertSame(builder, builder.timeFormat("test"));
    assertSame(builder, builder.decimalFormat("test"));
    assertSame(builder, builder.booleanFormat("test"));
    assertSame(builder, builder.columnSpecificTextFormats(Collections.emptyList()));

    // Test inherited methods from AbstractFormat.Builder
    assertSame(builder, builder.type(FormatType.Delimited));
    assertSame(builder, builder.name("test"));
    assertSame(builder, builder.description("test"));
    assertSame(builder, builder.extension("test"));
    assertSame(builder, builder.filename("test"));
    assertSame(builder, builder.mediaType(MediaType.PLAIN_TEXT_UTF_8.toString()));
    assertSame(builder, builder.hidden(false));
  }
}
