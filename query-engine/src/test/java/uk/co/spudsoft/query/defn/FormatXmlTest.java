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

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.co.spudsoft.query.exec.FormatInstance;
import uk.co.spudsoft.query.exec.fmts.xml.FormatXmlInstance;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class FormatXmlTest {

  @Test
  void testConstructorAndGetters() {
    // Arrange & Act
    FormatXml formatXml = new FormatXml.Builder()
      .type(FormatType.XML)
      .name("TestFormat")
      .extension("xml")
      .mediaType(MediaType.APPLICATION_XML)
      .xmlDeclaration(true)
      .encoding("UTF-16")
      .indent(true)
      .fieldsAsAttributes(true)
      .docName("testDoc")
      .rowName("testRow")
      .fieldInitialLetterFix("_")
      .fieldInvalidLetterFix("-")
      .build();

    // Assert
    assertEquals(FormatType.XML, formatXml.getType());
    assertEquals("TestFormat", formatXml.getName());
    assertEquals("xml", formatXml.getExtension());
    assertEquals(com.google.common.net.MediaType.parse(MediaType.APPLICATION_XML), formatXml.getMediaType());
    assertTrue(formatXml.isXmlDeclaration());
    assertEquals("UTF-16", formatXml.getEncoding());
    assertTrue(formatXml.isIndent());
    assertTrue(formatXml.isFieldsAsAttributes());
    assertEquals("testDoc", formatXml.getDocName());
    assertEquals("testRow", formatXml.getRowName());
    assertEquals("_", formatXml.getFieldInitialLetterFix());
    assertEquals("-", formatXml.getFieldInvalidLetterFix());
  }

  @Test
  void testBuilderDefaults() {
    // Arrange & Act
    FormatXml formatXml = new FormatXml.Builder().build();

    // Assert
    assertEquals("xml", formatXml.getName());
    assertEquals("xml", formatXml.getExtension());
    assertEquals(MediaType.APPLICATION_XML + "; charset=utf-8", formatXml.getMediaType().toString());
    assertEquals(FormatType.XML, formatXml.getType());
    assertTrue(formatXml.isXmlDeclaration());
    assertNull(formatXml.getEncoding());
    assertFalse(formatXml.isIndent());
    assertFalse(formatXml.isFieldsAsAttributes());
    assertNull(formatXml.getDocName());
    assertNull(formatXml.getRowName());
    assertNull(formatXml.getFieldInitialLetterFix());
    assertNull(formatXml.getFieldInvalidLetterFix());

    formatXml = formatXml.withDefaults();
    assertEquals("utf-8", formatXml.getEncoding());
    assertEquals("data", formatXml.getDocName());
    assertEquals("row", formatXml.getRowName());
    assertEquals("", formatXml.getFieldInitialLetterFix());
    assertEquals("", formatXml.getFieldInvalidLetterFix());

    formatXml = new FormatXml.Builder()
      .encoding("utf-16")
      .docName("testDoc")
      .rowName("rowName")
      .fieldInitialLetterFix("Z")
      .fieldInvalidLetterFix("_")
      .build();
    assertEquals("utf-16", formatXml.getEncoding());
    assertEquals("testDoc", formatXml.getDocName());
    assertEquals("rowName", formatXml.getRowName());
    assertEquals("Z", formatXml.getFieldInitialLetterFix());
    assertEquals("_", formatXml.getFieldInvalidLetterFix());
  }

  @Test
  void testStaticBuilder() {
    // Act
    FormatXml.Builder builder = FormatXml.builder();

    // Assert
    assertNotNull(builder);
    assertInstanceOf(FormatXml.Builder.class, builder);
  }

  @Test
  void testWithDefaults() {
    // Arrange
    FormatXml formatWithNulls = new FormatXml.Builder()
      .type(FormatType.XML)
      .build();

    // Act
    FormatXml formatWithDefaults = formatWithNulls.withDefaults();

    // Assert
    assertNotNull(formatWithDefaults);
    assertEquals(FormatType.XML, formatWithDefaults.getType());
    assertNotNull(formatWithDefaults.getName());
    assertNotNull(formatWithDefaults.getExtension());
    assertNotNull(formatWithDefaults.getMediaType());
    assertNotNull(formatWithDefaults.getDocName());
    assertNotNull(formatWithDefaults.getRowName());
  }

  @Test
  void testCreateInstance() {
    // Arrange
    FormatXml formatXml = new FormatXml.Builder()
      .type(FormatType.XML)
      .name("TestFormat")
      .build();

    Vertx vertx = mock(Vertx.class);
    Context context = mock(Context.class);
    @SuppressWarnings("unchecked")
    WriteStream<Buffer> writeStream = mock(WriteStream.class);

    // Act
    FormatInstance instance = formatXml.createInstance(vertx, context, writeStream);

    // Assert
    assertNotNull(instance);
    assertInstanceOf(FormatXmlInstance.class, instance);
  }

  @Test
  void testValidate() {
    // Arrange
    FormatXml validFormat = new FormatXml.Builder()
      .type(FormatType.XML)
      .name("TestFormat")
      .docName("validDoc")
      .rowName("validRow")
      .build();

    // Act & Assert - should not throw exception
    assertDoesNotThrow(() -> validFormat.validate());

    // Invalid format name
    FormatXml invalidFormatName = new FormatXml.Builder()
      .name(null)
      .type(FormatType.XML)
      .build();

    // Act & Assert - should throw exception
    Exception exception = assertThrows(IllegalArgumentException.class,
      () -> invalidFormatName.validate());
    assertTrue(exception.getMessage().contains("Format has no name"));

    // Invalid encoding
    FormatXml invalidEncoding = new FormatXml.Builder()
      .encoding("123")
      .build();

    // Act & Assert - should throw exception
    exception = assertThrows(IllegalArgumentException.class,
      () -> invalidEncoding.validate());
    assertThat(exception.getMessage(), containsString("The charset '123' is not recognised by this JVM"));

    // Invalid field initial letter fix
    FormatXml invalidFieldInitialLetterFix = new FormatXml.Builder()
      .type(FormatType.XML)
      .fieldInitialLetterFix("123")
      .build();

    // Act & Assert - should throw exception
    exception = assertThrows(IllegalArgumentException.class,
      () -> invalidFieldInitialLetterFix.validate());
    assertTrue(exception.getMessage().contains("fieldInitialLetterFix"));

    // Invalid document name
    FormatXml invalidFieldInvalidLetterFix = new FormatXml.Builder()
      .type(FormatType.XML)
      .fieldInvalidLetterFix("a&b")
      .build();

    // Act & Assert - should throw exception
    exception = assertThrows(IllegalArgumentException.class,
      () -> invalidFieldInvalidLetterFix.validate());
    assertTrue(exception.getMessage().contains("fieldInvalidLetterFix"));

    // Invalid document name
    FormatXml invalidDocName = new FormatXml.Builder()
      .type(FormatType.XML)
      .docName("1invalidDoc")
      .build();

    // Act & Assert - should throw exception
    exception = assertThrows(IllegalArgumentException.class,
      () -> invalidDocName.validate());
    assertTrue(exception.getMessage().contains("docName"));

    // Invalid row name
    FormatXml invalidRowName = new FormatXml.Builder()
      .type(FormatType.XML)
      .docName("validDoc")
      .rowName("invalid&Row")
      .build();

    // Act & Assert - should throw exception
    exception = assertThrows(IllegalArgumentException.class,
      () -> invalidRowName.validate());
    assertTrue(exception.getMessage().contains("rowName"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ValidName", "valid", "X123", "_valid", "x"})
  void testValidNameStartRegex(String name) {
    assertTrue(FormatXml.NAME_START_REGEX.matcher(name).matches());
  }

  @ParameterizedTest
  @ValueSource(strings = {"1invalid", "!", "&invalid", "invalid@name", "."})
  void testInvalidNameStartRegex(String name) {
    assertFalse(FormatXml.NAME_START_REGEX.matcher(name).matches());
  }

  @ParameterizedTest
  @ValueSource(strings = {"valid-name", "valid.name", "valid123", "valid_name"})
  void testValidNameCharRegex(String name) {
    assertTrue(FormatXml.NAME_CHAR_REGEX.matcher(name).matches());
  }

  @ParameterizedTest
  @ValueSource(strings = {"invalid&name", "invalid@name", "invalid name", "invalid#name"})
  void testInvalidNameCharRegex(String name) {
    assertFalse(FormatXml.NAME_CHAR_REGEX.matcher(name).matches());
  }
}
