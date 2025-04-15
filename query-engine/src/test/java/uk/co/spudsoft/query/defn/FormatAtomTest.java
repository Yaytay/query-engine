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
import uk.co.spudsoft.query.exec.FormatInstance;
import uk.co.spudsoft.query.exec.fmts.xml.FormatAtomInstance;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class FormatAtomTest {

  @Test
  void testConstructorAndGetters() {
    // Arrange & Act
    FormatAtom formatAtom = new FormatAtom.Builder()
      .type(FormatType.Atom)
      .name("TestFormat")
      .extension("xml")
      .mediaType(com.google.common.net.MediaType.parse(MediaType.APPLICATION_XML))
      .fieldInitialLetterFix("_")
      .fieldInvalidLetterFix("-")
      .build();

    // Assert
    assertEquals(FormatType.Atom, formatAtom.getType());
    assertEquals("TestFormat", formatAtom.getName());
    assertEquals("xml", formatAtom.getExtension());
    assertEquals(com.google.common.net.MediaType.parse(MediaType.APPLICATION_XML), formatAtom.getMediaType());
    assertEquals("_", formatAtom.getFieldInitialLetterFix());
    assertEquals("-", formatAtom.getFieldInvalidLetterFix());
  }

  @Test
  void testBuilderDefaults() {
    // Arrange & Act
    FormatAtom formatAtom = new FormatAtom.Builder().build();

    // Assert
    assertEquals("Atom", formatAtom.getName());
    assertEquals("xml", formatAtom.getExtension());
    assertEquals(MediaType.APPLICATION_ATOM_XML, formatAtom.getMediaType().toString());
    assertEquals(FormatType.Atom, formatAtom.getType());
    assertNull(formatAtom.getFieldInitialLetterFix());
    assertNull(formatAtom.getFieldInvalidLetterFix());

    formatAtom = formatAtom.withDefaults();
    assertEquals("", formatAtom.getFieldInitialLetterFix());
    assertEquals("", formatAtom.getFieldInvalidLetterFix());

    formatAtom = new FormatAtom.Builder()
      .fieldInitialLetterFix("Z")
      .fieldInvalidLetterFix("_")
      .build();
    assertEquals("Z", formatAtom.getFieldInitialLetterFix());
    assertEquals("_", formatAtom.getFieldInvalidLetterFix());
  }

  @Test
  void testStaticBuilder() {
    // Act
    FormatAtom.Builder builder = FormatAtom.builder();

    // Assert
    assertNotNull(builder);
    assertInstanceOf(FormatAtom.Builder.class, builder);
  }

  @Test
  void testWithDefaults() {
    // Arrange
    FormatAtom formatWithNulls = new FormatAtom.Builder()
      .type(FormatType.Atom)
      .build();

    // Act
    FormatAtom formatWithDefaults = formatWithNulls.withDefaults();

    // Assert
    assertNotNull(formatWithDefaults);
    assertEquals(FormatType.Atom, formatWithDefaults.getType());
    assertNotNull(formatWithDefaults.getName());
    assertNotNull(formatWithDefaults.getExtension());
    assertNotNull(formatWithDefaults.getMediaType());
  }

  @Test
  void testValidate() {
    // Arrange
    FormatAtom validFormat = new FormatAtom.Builder()
      .name("TestFormat")
      .build();

    // Act & Assert - should not throw exception
    assertDoesNotThrow(() -> validFormat.validate());

    // Invalid format name
    FormatAtom invalidFormatName = new FormatAtom.Builder()
      .name(null)
      .build();

    // Act & Assert - should throw exception
    Exception exception = assertThrows(IllegalArgumentException.class,
      () -> invalidFormatName.validate());
    assertTrue(exception.getMessage().contains("Format has no name"));

    // Invalid field initial letter fix
    FormatAtom invalidFieldInitialLetterFix = new FormatAtom.Builder()
      .fieldInitialLetterFix("123")
      .build();

    // Act & Assert - should throw exception
    exception = assertThrows(IllegalArgumentException.class,
      () -> invalidFieldInitialLetterFix.validate());
    assertTrue(exception.getMessage().contains("fieldInitialLetterFix"));

    // Invalid document name
    FormatAtom invalidFieldInvalidLetterFix = new FormatAtom.Builder()
      .fieldInvalidLetterFix("a&b")
      .build();

    // Act & Assert - should throw exception
    exception = assertThrows(IllegalArgumentException.class,
      () -> invalidFieldInvalidLetterFix.validate());
    assertTrue(exception.getMessage().contains("fieldInvalidLetterFix"));
  }

}
