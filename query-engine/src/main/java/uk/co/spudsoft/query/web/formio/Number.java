/*
 * Copyright (C) 2023 njt
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

/**
 *
 * @author njt
 */
public class Number extends Component<Number> {
  
  public static class NumberValidation extends Validation {
    private Double min;
    private Double max;
    private Double step;
    private Boolean integer;

    public Double getMin() {
      return min;
    }

    public void setMin(Double min) {
      this.min = min;
    }

    public Double getMax() {
      return max;
    }

    public void setMax(Double max) {
      this.max = max;
    }

    public Double getStep() {
      return step;
    }

    public void setStep(Double step) {
      this.step = step;
    }

    public Boolean getInteger() {
      return integer;
    }

    public void setInteger(Boolean integer) {
      this.integer = integer;
    }

    public Validation withMin(final Double value) {
      this.min = value;
      return this;
    }

    public Validation withMax(final Double value) {
      this.max = value;
      return this;
    }

    public Validation withStep(final Double value) {
      this.step = value;
      return this;
    }

    public Validation withInteger(final Boolean value) {
      this.integer = value;
      return this;
    }
  }

  public Number() {
    super("number");
  }

  public Number withValidate(NumberValidation value) {
    return super.withValidate(value);
  }

  public void setValidate(NumberValidation validate) {
    super.setValidate(validate);
  }

}
