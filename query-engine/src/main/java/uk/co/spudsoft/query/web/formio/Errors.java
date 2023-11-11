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
 *
 * @author jtalbut
 */
public class Errors extends AbstractComponent<Errors> {

  @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
  public Errors(JsonGenerator generator) throws IOException {
    super(generator);
  }
  
  public Errors withRequired(final String value) throws IOException {
    return with("required", value);
  }

  public Errors withMin(final String value) throws IOException {
    return with("min", value);
  }

  public Errors withMax(final String value) throws IOException {
    return with("max", value);
  }

  public Errors withMinLength(final String value) throws IOException {
    return with("minLength", value);
  }

  public Errors withMaxLength(final String value) throws IOException {
    return with("maxLength", value);
  }

  public Errors withInvalidEmail(final String value) throws IOException {
    return with("invalid_email", value);
  }

  public Errors withInvalidDate(final String value) throws IOException {
    return with("invalid_date", value);
  }

  public Errors withPattern(final String value) throws IOException {
    return with("pattern", value);
  }

  public Errors withCustom(final String value) throws IOException {
    return with("custom", value);
  }
  
}
