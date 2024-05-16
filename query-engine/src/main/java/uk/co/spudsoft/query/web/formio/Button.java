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
 * Output a formio Button.
 * 
 * @author jtalbut
 */
public class Button extends Component<Button> {

  public enum ActionType {
    submit, reset, event, oauth
  }
  
  /**
   * Constructor.
   * 
   * @param generator The Jackson JsonGenerator for FormIO.
   * @throws IOException if something goes wrong.
   */  
  @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
  public Button(JsonGenerator generator) throws IOException {
    super(generator, "button");
  }

  /**
   * Output a size field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Button withSize(final String value) throws IOException {
    return with("size", value);
  }

  /**
   * Output a leftIcon field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Button withLeftIcon(final String value) throws IOException {
    return with("leftIcon", value);
  }

  /**
   * Output a rightIcon field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Button withRightIcon(final String value) throws IOException {
    return with("rightIcon", value);
  }

  /**
   * Output a block field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Button withBlock(final Boolean value) throws IOException {
    return with("block", value);
  }

  /**
   * Output an action field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Button withAction(final ActionType value) throws IOException {
    if (value != null) {
      return with("action", value.name());
    } else {
      return null;
    }
  }

  /**
   * Output a disableOnInvalid field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Button withDisableOnInvalid(final Boolean value) throws IOException {
    return with("disableOnInvalid", value);
  }

  /**
   * Output a theme field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Button withTheme(final String value) throws IOException {
    return with("theme", value);
  }
}
