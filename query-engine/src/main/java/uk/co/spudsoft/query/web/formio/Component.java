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
 * @param <T> The concrete class the derives from this base class.
 */
@SuppressWarnings("unchecked")
public abstract class Component<T extends Component<T>> extends AbstractComponent<T> {

  @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
  protected Component(JsonGenerator generator, String type) throws IOException {
    super(generator);
    generator.writeStringField("type", type);
  }

  public final T withLabel(final String value) throws IOException {
    return with("label", value);
  }

  public final T withKey(final String value) throws IOException {
    return with("key", value);
  }

  public final T withDescription(final String value) throws IOException {
    return with("description", value);
  }

  public final T withPlaceholder(final String value) throws IOException {
    return with("placeholder", value);
  }

  public final T withInput(final Boolean value) throws IOException {
    return with("input", value);
  }

  public final T withHideLabel(final Boolean value) throws IOException {
    return with("hideLabel", value);
  }

  public final T withTableView(final Boolean value) throws IOException {
    return with("tableView", value);
  }

  public final T withMultiple(final Boolean value) throws IOException {
    return with("multiple", value);
  }

  public final T withProtect(final Boolean value) throws IOException {
    return with("protect", value);
  }

  public final T withCustomClass(final String value) throws IOException {
    return with("customClass", value);
  }

  public final T withPrefix(final String value) throws IOException {
    return with("prefix", value);
  }

  public final T withSuffix(final String value) throws IOException {
    return with("suffix", value);
  }

  public final T withDefaultValue(final String value) throws IOException {
    return with("defaultValue", value);
  }

  public final T withWidget(final String value) throws IOException {
    return with("widget", value);
  }

  public final T withClearOnHide(final Boolean value) throws IOException {
    return with("clearOnHide", value);
  }

  public final T withUnique(final Boolean value) throws IOException {
    return with("unique", value);
  }

  public final T withPersistent(final Boolean value) throws IOException {
    return with("persistent", value);
  }

  public final T withHidden(final Boolean value) throws IOException {
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
