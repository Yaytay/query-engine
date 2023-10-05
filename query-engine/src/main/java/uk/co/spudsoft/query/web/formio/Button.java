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
import java.io.IOException;

/**
 *
 * @author jtalbut
 */
public class Button extends Component<Button> {

  public enum ActionType {
    submit, reset, event, oauth
  }
  
  public Button(JsonGenerator generator) throws IOException {
    super(generator, "button");
  }

  public Button withSize(final String value) throws IOException {
    return with("size", value);
  }

  public Button withLeftIcon(final String value) throws IOException {
    return with("leftIcon", value);
  }

  public Button withRightIcon(final String value) throws IOException {
    return with("rightIcon", value);
  }

  public Button withBlock(final Boolean value) throws IOException {
    return with("block", value);
  }

  public Button withAction(final ActionType value) throws IOException {
    if (value != null) {
      return with("action", value.name());
    } else {
      return null;
    }
  }

  public Button withDisableOnInvalid(final Boolean value) throws IOException {
    return with("disableOnInvalid", value);
  }

  public Button withTheme(final String value) throws IOException {
    return with("theme", value);
  }
}
