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
public class Number extends Component<Number> {
  
  public static class NumberValidation extends AbstractComponent<NumberValidation> {

    @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
    protected NumberValidation(JsonGenerator generator) throws IOException {
      super(generator);
    }

    public NumberValidation withMin(final java.lang.Number value) throws IOException {
      return with("min", value);
    }

    public NumberValidation withMax(final java.lang.Number value) throws IOException {
      return with("max", value);
    }

    public NumberValidation withStep(final java.lang.Number value) throws IOException {
      return with("step", value);
    }

    public NumberValidation withInteger(final Boolean value) throws IOException {
      return with("integer", value);
    }
    
    public NumberValidation withRequired(final Boolean value) throws IOException {
      return with("required", value);
    }

    public NumberValidation withPattern(final String value) throws IOException {
      return with("pattern", value);
    }

    public NumberValidation withCustom(final String value) throws IOException {
      return with("custom", value);
    }
  }
  
  @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
  public Number(JsonGenerator generator) throws IOException {
    super(generator, "number");
  }

  public NumberValidation addNumberValidate() throws IOException {
    generator.writeFieldName("validate");
    return new NumberValidation(generator);
  }

}
