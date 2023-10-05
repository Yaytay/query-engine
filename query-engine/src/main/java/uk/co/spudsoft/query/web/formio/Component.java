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
 * @param <T> The concrete class the derives from this base class.
 */
@SuppressWarnings("unchecked")
public abstract class Component<T extends Component<T>> extends AbstractComponent<T> {

  protected Component(JsonGenerator generator, String type) throws IOException {
    super(generator);
    generator.writeStringField("type", type);
  }

  public T withLabel(final String value) throws IOException {
    return with("label", value);
  }

  public T withKey(final String value) throws IOException {
    return with("key", value);
  }

  public T withDescription(final String value) throws IOException {
    return with("description", value);
  }

  public T withPlaceholder(final String value) throws IOException {
    return with("placeholder", value);
  }

  public T withInput(final Boolean value) throws IOException {
    return with("input", value);
  }

  public T withTableView(final Boolean value) throws IOException {
    return with("tableView", value);
  }

  public T withMultiple(final Boolean value) throws IOException {
    return with("multiple", value);
  }

  public T withProtect(final Boolean value) throws IOException {
    return with("protect", value);
  }

  public T withCustomClass(final String value) throws IOException {
    return with("customClass", value);
  }

  public T withPrefix(final String value) throws IOException {
    return with("prefix", value);
  }

  public T withSuffix(final String value) throws IOException {
    return with("suffix", value);
  }

  public T withDefaultValue(final String value) throws IOException {
    return with("defaultValue", value);
  }

  public T withClearOnHide(final Boolean value) throws IOException {
    return with("clearOnHide", value);
  }

  public T withUnique(final Boolean value) throws IOException {
    return with("unique", value);
  }

  public T withPersistent(final Boolean value) throws IOException {
    return with("persistent", value);
  }

  public T withHidden(final Boolean value) throws IOException {
    return with("hidden", value);
  }

  public Validation addValidate() throws IOException {
    generator.writeFieldName("validate");
    return new Validation(generator);
  }

  public Conditional addConditional() throws IOException {
    generator.writeFieldName("conditional");
    return new Conditional(generator);
  }

  public Errors addErrors() throws IOException {
    generator.writeFieldName("errors");
    return new Errors(generator);
  }

}
