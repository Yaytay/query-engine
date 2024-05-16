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
 * Output a formio Component.
 *
 * @author jtalbut
 * @param <T> The concrete class the derives from this base class.
 */
@SuppressWarnings("unchecked")
public abstract class Component<T extends Component<T>> extends AbstractComponent<T> {

  /**
   * Constructor.
   * 
   * @param generator The Jackson JsonGenerator for FormIO.
   * @param type The type of component to be written as the type field.
   * @throws IOException if something goes wrong.
   */
  @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
  protected Component(JsonGenerator generator, String type) throws IOException {
    super(generator);
    generator.writeStringField("type", type);
  }

    /**
     * Output a label field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
  public final T withLabel(final String value) throws IOException {
    return with("label", value);
  }

    /**
     * Output a key field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
  public final T withKey(final String value) throws IOException {
    return with("key", value);
  }

    /**
     * Output a description field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
  public final T withDescription(final String value) throws IOException {
    return with("description", value);
  }

    /**
     * Output a placeholder field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
  public final T withPlaceholder(final String value) throws IOException {
    return with("placeholder", value);
  }

    /**
     * Output an input field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
  public final T withInput(final Boolean value) throws IOException {
    return with("input", value);
  }

    /**
     * Output a hideLabel field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
  public final T withHideLabel(final Boolean value) throws IOException {
    return with("hideLabel", value);
  }

    /**
     * Output a tableView field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
  public final T withTableView(final Boolean value) throws IOException {
    return with("tableView", value);
  }

    /**
     * Output a multiple field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
  public final T withMultiple(final Boolean value) throws IOException {
    return with("multiple", value);
  }

    /**
     * Output a protect field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
  public final T withProtect(final Boolean value) throws IOException {
    return with("protect", value);
  }

    /**
     * Output a customClass field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
  public final T withCustomClass(final String value) throws IOException {
    return with("customClass", value);
  }

    /**
     * Output a prefix field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
  public final T withPrefix(final String value) throws IOException {
    return with("prefix", value);
  }

    /**
     * Output a suffix field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
  public final T withSuffix(final String value) throws IOException {
    return with("suffix", value);
  }

    /**
     * Output a defaultValue field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
  public final T withDefaultValue(final String value) throws IOException {
    return with("defaultValue", value);
  }

    /**
     * Output a widget field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
  public final T withWidget(final String value) throws IOException {
    return with("widget", value);
  }

    /**
     * Output a clearOnHide field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
  public final T withClearOnHide(final Boolean value) throws IOException {
    return with("clearOnHide", value);
  }

    /**
     * Output a unique field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
  public final T withUnique(final Boolean value) throws IOException {
    return with("unique", value);
  }

    /**
     * Output a persistent field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
  public final T withPersistent(final Boolean value) throws IOException {
    return with("persistent", value);
  }

    /**
     * Output a hidden field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
  public final T withHidden(final Boolean value) throws IOException {
    return with("hidden", value);
  }

    /**
     * Add a validate field.
     * @return a newly created Validation object.
     * @throws IOException if the generator fails.
     */
  public Validation addValidate() throws IOException {
    generator.writeFieldName("validate");
    return new Validation(generator);
  }

    /**
     * Add a conditional field.
     * @return a newly created Conditional object.
     * @throws IOException if the generator fails.
     */
  public Conditional addConditional() throws IOException {
    generator.writeFieldName("conditional");
    return new Conditional(generator);
  }

    /**
     * Add an errors field.
     * @return a newly created Errors object.
     * @throws IOException if the generator fails.
     */
  public Errors addErrors() throws IOException {
    generator.writeFieldName("errors");
    return new Errors(generator);
  }

}
