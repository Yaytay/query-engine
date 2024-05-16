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
 * Output a formio Errors component.
 *
 * @author jtalbut
 */
public class Errors extends AbstractComponent<Errors> {

  /**
   * Constructor.
   * 
   * @param generator The Jackson JsonGenerator for FormIO.
   * @throws IOException if something goes wrong.
   */
  @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
  public Errors(JsonGenerator generator) throws IOException {
    super(generator);
  }
  
  /**
   * Output a required field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Errors withRequired(final String value) throws IOException {
    return with("required", value);
  }

  /**
   * Output a min field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Errors withMin(final String value) throws IOException {
    return with("min", value);
  }

  /**
   * Output a max field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Errors withMax(final String value) throws IOException {
    return with("max", value);
  }

  /**
   * Output a minLength field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Errors withMinLength(final String value) throws IOException {
    return with("minLength", value);
  }

  /**
   * Output a maxLength field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Errors withMaxLength(final String value) throws IOException {
    return with("maxLength", value);
  }

  /**
   * Output an invalid_email field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Errors withInvalidEmail(final String value) throws IOException {
    return with("invalid_email", value);
  }

  /**
   * Output a invalid_date field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Errors withInvalidDate(final String value) throws IOException {
    return with("invalid_date", value);
  }

  /**
   * Output a pattern field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Errors withPattern(final String value) throws IOException {
    return with("pattern", value);
  }

  /**
   * Output a custom field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Errors withCustom(final String value) throws IOException {
    return with("custom", value);
  }
  
}
