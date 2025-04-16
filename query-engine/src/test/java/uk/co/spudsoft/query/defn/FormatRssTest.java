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

import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormatRssTest {

  @Test
  void testConstructorAndGetters() {
    // Arrange & Act
    FormatRss formatRss = new FormatRss.Builder()
      .type(FormatType.RSS)
      .name("TestFormat")
      .extension("xml")
      .mediaType(com.google.common.net.MediaType.parse(MediaType.APPLICATION_XML))
      .fieldInitialLetterFix("_")
      .fieldInvalidLetterFix("-")
      .build();

    // Assert
    assertEquals(FormatType.RSS, formatRss.getType());
    assertEquals("TestFormat", formatRss.getName());
    assertEquals("xml", formatRss.getExtension());
    assertEquals(com.google.common.net.MediaType.parse(MediaType.APPLICATION_XML), formatRss.getMediaType());
    assertEquals("_", formatRss.getFieldInitialLetterFix());
    assertEquals("-", formatRss.getFieldInvalidLetterFix());
  }

  @Test
  void testBuilderDefaults() {
    // Arrange & Act
    FormatRss formatRss = new FormatRss.Builder().build();

    // Assert
    assertEquals("RSS", formatRss.getName());
    assertEquals("xml", formatRss.getExtension());
    assertEquals("application/rss+xml", formatRss.getMediaType().toString());
    assertEquals(FormatType.RSS, formatRss.getType());
    assertNull(formatRss.getFieldInitialLetterFix());
    assertNull(formatRss.getFieldInvalidLetterFix());

    formatRss = formatRss.withDefaults();
    assertEquals("F", formatRss.getFieldInitialLetterFix());
    assertEquals("_", formatRss.getFieldInvalidLetterFix());

    formatRss = new FormatRss.Builder()
      .fieldInitialLetterFix("Z")
      .fieldInvalidLetterFix("X")
      .build();
    assertEquals("Z", formatRss.getFieldInitialLetterFix());
    assertEquals("X", formatRss.getFieldInvalidLetterFix());

    formatRss = formatRss.withDefaults();
    assertEquals("Z", formatRss.getFieldInitialLetterFix());
    assertEquals("X", formatRss.getFieldInvalidLetterFix());

  }

  @Test
  void testStaticBuilder() {
    // Act
    FormatRss.Builder builder = FormatRss.builder();

    // Assert
    assertNotNull(builder);
    assertInstanceOf(FormatRss.Builder.class, builder);
  }

  @Test
  void testWithDefaults() {
    // Arrange
    FormatRss formatWithNulls = new FormatRss.Builder()
      .type(FormatType.RSS)
      .build();

    // Act
    FormatRss formatWithDefaults = formatWithNulls.withDefaults();

    // Assert
    assertNotNull(formatWithDefaults);
    assertEquals(FormatType.RSS, formatWithDefaults.getType());
    assertNotNull(formatWithDefaults.getName());
    assertNotNull(formatWithDefaults.getExtension());
    assertNotNull(formatWithDefaults.getMediaType());
  }

  @Test
  void testValidate() {
    // Arrange
    FormatRss validFormat = new FormatRss.Builder()
      .name("TestFormat")
      .build();

    // Act & Assert - should not throw exception
    assertDoesNotThrow(() -> validFormat.validate());

    // Arrange
    FormatRss validFormat2 = new FormatRss.Builder()
      .name("TestFormat")
      .fieldInitialLetterFix(null)
      .fieldInvalidLetterFix(null)
      .build();

    // Act & Assert - should not throw exception
    assertDoesNotThrow(() -> validFormat2.validate());

    // Invalid format name
    FormatRss invalidFormatName = new FormatRss.Builder()
      .name(null)
      .build();

    // Act & Assert - should throw exception
    Exception exception = assertThrows(IllegalArgumentException.class,
      () -> invalidFormatName.validate());
    assertTrue(exception.getMessage().contains("Format has no name"));

    // Invalid field initial letter fix
    FormatRss invalidFieldInitialLetterFix = new FormatRss.Builder()
      .fieldInitialLetterFix("123")
      .build();

    // Act & Assert - should throw exception
    exception = assertThrows(IllegalArgumentException.class,
      () -> invalidFieldInitialLetterFix.validate());
    assertTrue(exception.getMessage().contains("fieldInitialLetterFix"));

    // Invalid document name
    FormatRss invalidFieldInvalidLetterFix = new FormatRss.Builder()
      .fieldInvalidLetterFix("a&b")
      .build();

    // Act & Assert - should throw exception
    exception = assertThrows(IllegalArgumentException.class,
      () -> invalidFieldInvalidLetterFix.validate());
    assertTrue(exception.getMessage().contains("fieldInvalidLetterFix"));
  }

}
