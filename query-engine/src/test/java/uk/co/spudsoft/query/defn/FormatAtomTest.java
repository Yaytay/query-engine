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

class FormatAtomTest {

  @Test
  void testConstructorAndGetters() {
    // Arrange & Act
    FormatAtom formatAtom = new FormatAtom.Builder()
      .type(FormatType.Atom)
      .name("TestFormat")
      .extension("xml")
      .mediaType(MediaType.APPLICATION_XML)
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
    assertEquals(MediaType.APPLICATION_ATOM_XML + "; charset=utf-8", formatAtom.getMediaType().toString());
    assertEquals(FormatType.Atom, formatAtom.getType());
    assertEquals("F", formatAtom.getFieldInitialLetterFix());
    assertEquals("_", formatAtom.getFieldInvalidLetterFix());

    formatAtom = new FormatAtom.Builder()
      .fieldInitialLetterFix("Z")
      .fieldInvalidLetterFix("X")
      .build();
    assertEquals("Z", formatAtom.getFieldInitialLetterFix());
    assertEquals("X", formatAtom.getFieldInvalidLetterFix());
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
  void testValidate() {
    // Arrange
    FormatAtom validFormat = new FormatAtom.Builder()
      .name("TestFormat")
      .build();

    // Act & Assert - should not throw exception
    assertDoesNotThrow(() -> validFormat.validate());

    // Arrange
    FormatAtom validFormat2 = new FormatAtom.Builder()
      .name("TestFormat")
      .fieldInitialLetterFix(null)
      .fieldInvalidLetterFix(null)
      .build();

    // Act & Assert - should not throw exception
    assertDoesNotThrow(() -> validFormat2.validate());

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
