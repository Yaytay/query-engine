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
public class Errors {
  
  private String required;
  private String min;
  private String max;
  private String minLength;
  private String maxLength;
  private String invalid_email;
  private String invalid_date;
  private String pattern;
  private String custom;

  public String getRequired() {
    return required;
  }

  public void setRequired(String required) {
    this.required = required;
  }

  public String getMin() {
    return min;
  }

  public void setMin(String min) {
    this.min = min;
  }

  public String getMax() {
    return max;
  }

  public void setMax(String max) {
    this.max = max;
  }

  public String getMinLength() {
    return minLength;
  }

  public void setMinLength(String minLength) {
    this.minLength = minLength;
  }

  public String getMaxLength() {
    return maxLength;
  }

  public void setMaxLength(String maxLength) {
    this.maxLength = maxLength;
  }

  public String getInvalid_email() {
    return invalid_email;
  }

  public void setInvalid_email(String invalid_email) {
    this.invalid_email = invalid_email;
  }

  public String getInvalid_date() {
    return invalid_date;
  }

  public void setInvalid_date(String invalid_date) {
    this.invalid_date = invalid_date;
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

  public Errors withRequired(final String value) {
    this.required = value;
    return this;
  }

  public Errors withMin(final String value) {
    this.min = value;
    return this;
  }

  public Errors withMax(final String value) {
    this.max = value;
    return this;
  }

  public Errors withMinLength(final String value) {
    this.minLength = value;
    return this;
  }

  public Errors withMaxLength(final String value) {
    this.maxLength = value;
    return this;
  }

  public Errors withInvalid_email(final String value) {
    this.invalid_email = value;
    return this;
  }

  public Errors withInvalid_date(final String value) {
    this.invalid_date = value;
    return this;
  }

  public Errors withPattern(final String value) {
    this.pattern = value;
    return this;
  }

  public Errors withCustom(final String value) {
    this.custom = value;
    return this;
  }

  
}
