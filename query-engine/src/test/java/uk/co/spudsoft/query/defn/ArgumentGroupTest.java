/*
 * Copyright (C) 2024 njt
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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author njt
 */
public class ArgumentGroupTest {
  
  @Test
  public void testValidate() {
    ArgumentGroup group1 = ArgumentGroup.builder().build();
    assertEquals("An argument group has a null name."
            , assertThrows(IllegalArgumentException.class, () -> {
              group1.validate();
            }).getMessage()
    );
    ArgumentGroup group2 = ArgumentGroup.builder().name("").build();
    assertEquals("An argument group has a blank name."
            , assertThrows(IllegalArgumentException.class, () -> {
              group2.validate();
            }).getMessage()
    );
    ArgumentGroup group3 = ArgumentGroup.builder().name("!£$%£$%").build();
    assertEquals("The argument group \"!£$%£$%\" does not have a valid name."
            , assertThrows(IllegalArgumentException.class, () -> {
              group3.validate();
            }).getMessage()
    );
  }

  @Test
  public void testGetName() {
    ArgumentGroup group = ArgumentGroup.builder().build();
    assertNull(group.getName());
    group = ArgumentGroup.builder().name("name").build();
    assertEquals("name", group.getName());
  }

  @Test
  public void testGetTitle() {
    ArgumentGroup group = ArgumentGroup.builder().build();
    assertNull(group.getTitle());
    group = ArgumentGroup.builder().title("title").build();
    assertEquals("title", group.getTitle());
  }

  @Test
  public void testGetDescription() {
    ArgumentGroup group = ArgumentGroup.builder().build();
    assertNull(group.getDescription());
    group = ArgumentGroup.builder().description("description").build();
    assertEquals("description", group.getDescription());
  }

  @Test
  public void testType() {
    ArgumentGroup group = ArgumentGroup.builder().build();
    assertNull(group.getType());
    group = ArgumentGroup.builder().type(ArgumentGroupType.COLLAPSIBLE_PANEL).build();
    assertEquals(ArgumentGroupType.COLLAPSIBLE_PANEL, group.getType());
  }

  @Test
  public void testGetTheme() {
    ArgumentGroup group = ArgumentGroup.builder().build();
    assertNull(group.getTheme());
    group = ArgumentGroup.builder().theme("theme").build();
    assertEquals("theme", group.getTheme());
  }

}
