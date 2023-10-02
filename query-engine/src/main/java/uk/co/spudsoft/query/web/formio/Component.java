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

import com.oracle.js.parser.ir.ObjectNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 *
 * @author njt
 * @param <T> The concrete class the derives from this base class.
 */
@SuppressWarnings("unchecked")
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Data object purely for translating to JSON")
public abstract class Component<T extends Component<T>> {

  protected ObjectNode node;
  
  private String type;
  private String label;
  private String key;
  private String description;
  private String placeholder;
  private boolean input;
  private boolean tableView;
  private boolean multiple;
  private boolean protect;
  private String customClass;
  private String prefix;
  private String suffix;
  private String defaultValue;
  private boolean clearOnHide;
  private boolean unique;
  private boolean persistent;
  private boolean hidden;
  private Validation validate;
  private Conditional conditional;
  private Errors errors;

  protected Component(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  
  public String getPlaceholder() {
    return placeholder;
  }

  public void setPlaceholder(String placeholder) {
    this.placeholder = placeholder;
  }

  public boolean isInput() {
    return input;
  }

  public void setInput(boolean input) {
    this.input = input;
  }

  public boolean isTableView() {
    return tableView;
  }

  public void setTableView(boolean tableView) {
    this.tableView = tableView;
  }

  public boolean isMultiple() {
    return multiple;
  }

  public void setMultiple(boolean multiple) {
    this.multiple = multiple;
  }

  public boolean isProtect() {
    return protect;
  }

  public void setProtect(boolean protect) {
    this.protect = protect;
  }

  public String getCustomClass() {
    return customClass;
  }

  public void setCustomClass(String customClass) {
    this.customClass = customClass;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public String getSuffix() {
    return suffix;
  }

  public void setSuffix(String suffix) {
    this.suffix = suffix;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public boolean isClearOnHide() {
    return clearOnHide;
  }

  public void setClearOnHide(boolean clearOnHide) {
    this.clearOnHide = clearOnHide;
  }

  public boolean isUnique() {
    return unique;
  }

  public void setUnique(boolean unique) {
    this.unique = unique;
  }

  public boolean isPersistent() {
    return persistent;
  }

  public void setPersistent(boolean persistent) {
    this.persistent = persistent;
  }

  public boolean isHidden() {
    return hidden;
  }

  public void setHidden(boolean hidden) {
    this.hidden = hidden;
  }

  public Validation getValidate() {
    return validate;
  }

  public void setValidate(Validation validate) {
    this.validate = validate;
  }

  public Conditional getConditional() {
    return conditional;
  }

  public void setConditional(Conditional conditional) {
    this.conditional = conditional;
  }

  public Errors getErrors() {
    return errors;
  }

  public void setErrors(Errors errors) {
    this.errors = errors;
  }

  public T withType(final String value) {
    this.type = value;
    return (T) this;
  }

  public T withLabel(final String value) {
    this.label = value;
    return (T) this;
  }

  public T withKey(final String value) {
    this.key = value;
    return (T) this;
  }

  public T withDescription(final String value) {
    this.description = value;
    return (T) this;
  }

  public T withPlaceholder(final String value) {
    this.placeholder = value;
    return (T) this;
  }

  public T withInput(final boolean value) {
    this.input = value;
    return (T) this;
  }

  public T withTableView(final boolean value) {
    this.tableView = value;
    return (T) this;
  }

  public T withMultiple(final boolean value) {
    this.multiple = value;
    return (T) this;
  }

  public T withProtect(final boolean value) {
    this.protect = value;
    return (T) this;
  }

  public T withCustomClass(final String value) {
    this.customClass = value;
    return (T) this;
  }

  public T withPrefix(final String value) {
    this.prefix = value;
    return (T) this;
  }

  public T withSuffix(final String value) {
    this.suffix = value;
    return (T) this;
  }

  public T withDefaultValue(final String value) {
    this.defaultValue = value;
    return (T) this;
  }

  public T withClearOnHide(final boolean value) {
    this.clearOnHide = value;
    return (T) this;
  }

  public T withUnique(final boolean value) {
    this.unique = value;
    return (T) this;
  }

  public T withPersistent(final boolean value) {
    this.persistent = value;
    return (T) this;
  }

  public T withHidden(final boolean value) {
    this.hidden = value;
    return (T) this;
  }

  public T withValidate(final Validation value) {
    this.validate = value;
    return (T) this;
  }

  public T withConditional(final Conditional value) {
    this.conditional = value;
    return (T) this;
  }

  public T withErrors(final Errors value) {
    this.errors = value;
    return (T) this;
  }
  
  

}
