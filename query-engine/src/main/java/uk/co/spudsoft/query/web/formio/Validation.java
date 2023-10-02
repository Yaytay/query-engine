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

/**
 *
 * @author njt
 */
public class Validation {
  
  private Boolean required;
  private Integer minLength;
  private Integer maxLength;
  private String pattern;
  private String custom;

  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public Integer getMinLength() {
    return minLength;
  }

  public void setMinLength(Integer minLength) {
    this.minLength = minLength;
  }

  public Integer getMaxLength() {
    return maxLength;
  }

  public void setMaxLength(Integer maxLength) {
    this.maxLength = maxLength;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public String getCustom() {
    return custom;
  }

  public void setCustom(String custom) {
    this.custom = custom;
  }

  public Validation withRequired(final Boolean value) {
    this.required = value;
    return this;
  }

  public Validation withMinLength(final Integer value) {
    this.minLength = value;
    return this;
  }

  public Validation withMaxLength(final Integer value) {
    this.maxLength = value;
    return this;
  }

  public Validation withPattern(final String value) {
    this.pattern = value;
    return this;
  }

  public Validation withCustom(final String value) {
    this.custom = value;
    return this;
  }

  
}
