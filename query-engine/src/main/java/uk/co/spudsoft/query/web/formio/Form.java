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

import com.fasterxml.jackson.core.JsonGenerator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;

/**
 * Output a formio Form container.
 *
 * @author jtalbut
 */
public class Form extends AbstractContainer<Form> {

  /**
   * Constructor.
   * 
   * @param generator The Jackson JsonGenerator for FormIO.
   * @throws IOException if something goes wrong.
   */
  @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
  public Form(JsonGenerator generator) throws IOException {
    super(generator, "form");
  }

  /**
   * Output a name field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Form withName(final String value) throws IOException {
    return with("name", value);
  }

  /**
   * Output a title field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Form withTitle(final String value) throws IOException {
    return with("title", value);
  }

  /**
   * Output a path field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Form withPath(final String value) throws IOException {
    return with("path", value);
  }

  /**
   * Output a display field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Form withDisplay(final String value) throws IOException {
    return with("display", value);
  }

}
