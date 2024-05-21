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
 * Output a formio Number component.
 *
 * @author jtalbut
 */
public class Number extends Component<Number> {
  
  /**
   * Output a formio Validation component for Number components.
   */
  public static class NumberValidation extends AbstractComponent<NumberValidation> {

    /**
     * Constructor.
     * @param generator The Jackson {@link com.fasterxml.jackson.core.JsonGenerator} for FormIO.
     * @throws IOException if something goes wrong.
     */
    @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
    protected NumberValidation(JsonGenerator generator) throws IOException {
      super(generator);
    }

    /**
     * Output a min field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public NumberValidation withMin(final java.lang.Number value) throws IOException {
      return with("min", value);
    }

    /**
     * Output a max field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public NumberValidation withMax(final java.lang.Number value) throws IOException {
      return with("max", value);
    }

    /**
     * Output a step field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public NumberValidation withStep(final java.lang.Number value) throws IOException {
      return with("step", value);
    }

    /**
     * Output an integer field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public NumberValidation withInteger(final Boolean value) throws IOException {
      return with("integer", value);
    }
    
    /**
     * Output a required field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public NumberValidation withRequired(final Boolean value) throws IOException {
      return with("required", value);
    }

    /**
     * Output a pattern field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public NumberValidation withPattern(final String value) throws IOException {
      return with("pattern", value);
    }

    /**
     * Output a custom field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public NumberValidation withCustom(final String value) throws IOException {
      return with("custom", value);
    }
  }
  
  /**
   * Constructor.
   * 
   * @param generator The Jackson JsonGenerator for FormIO.
   * @throws IOException if something goes wrong.
   */
  @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
  public Number(JsonGenerator generator) throws IOException {
    super(generator, "number");
  }

  /**
   * Output a validate field.
   * @return a newly created NumberValidation object.
   * @throws IOException if the generator fails.
   */
  public NumberValidation addNumberValidate() throws IOException {
    generator.writeFieldName("validate");
    return new NumberValidation(generator);
  }

}
