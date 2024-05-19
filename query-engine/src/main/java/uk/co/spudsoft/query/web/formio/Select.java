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
 * Output a formio Select component.
 *
 * @author jtalbut
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Data object purely for translating to JSON")
public class Select extends Component<Select> {

  /**
   * The type of data source for the Select.
   * <p>
   * See <a href="https://help.form.io/userguide/form-building/form-components#select">https://help.form.io/userguide/form-building/form-components#select</a>
   */
  public enum DataSrcType {
    /**
     * Create your own list of static values for the select dropdown.
     */
    values, 
    /**
     * Provide raw JSON data as the select field value. 
     */
    json, 
    /**
     * Designate a URL that will provide the option values for the Select Dropdown
     */
    url, 
    /**
     * Source of data is mapped to a Resource within your project to populate the entire Resource Object or underlying field data within the selected Resource as the Select component options. 
     */
    resource, 
    /**
     * Write custom code to provide value options for the Select component.
     */
    custom
  }
  
  /**
   * A DataValue used for {@link DataSrcType#values}.
   */
  public static class DataValue extends AbstractComponent<DataValue> {

    /**
     * Constructor.
     * 
     * @param generator The Jackson JsonGenerator for FormIO.
     * @throws IOException if something goes wrong.
     */
    @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
    public DataValue(JsonGenerator generator) throws IOException {
      super(generator);
    }

    /**
     * Output a label field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public DataValue withLabel(final String value) throws IOException {
      return with("label", value);
    }

    /**
     * Output a value field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public DataValue withValue(final String value) throws IOException {
      return with("value", value);
    }
  }
  
  /**
   * A set of {@link DataValue} objects.
   */
  public static class DataValues extends AbstractComponent<DataValues> {

    /**
     * Constructor.
     * 
     * @param generator The Jackson JsonGenerator for FormIO.
     * @throws IOException if something goes wrong.
     */
    @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
    public DataValues(JsonGenerator generator) throws IOException {
      super(generator);
    }

    /**
     * Output a values field.
     * @return a newly created {@link ComponentArray} to which {@link DataValue} instances should be added.
     * @throws IOException if the generator fails.
     */
    public ComponentArray addValues() throws IOException {
      generator.writeFieldName("values");
      return new ComponentArray(generator);    
    }
  }
  
  /**
   * An HTTP header to be added to URLs used for requesting select values.
   */
  public static class DataUrlHeader extends AbstractComponent<DataUrlHeader> {

    /**
     * Constructor.
     * 
     * @param generator The Jackson JsonGenerator for FormIO.
     * @throws IOException if something goes wrong.
     */
    @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
    public DataUrlHeader(JsonGenerator generator) throws IOException {
      super(generator);
    }

    /**
     * Output a key field - the header name.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public DataUrlHeader withKey(final String value) throws IOException {
      return with("key", value);
    }

    /**
     * Output a value field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public DataUrlHeader withValue(final String value) throws IOException {
      return with("value", value);
    }

  }
  
  /**
   * A DataValue used for {@link DataSrcType#url}.
   */
  public static class DataUrl extends AbstractComponent<DataUrl> {

    /**
     * Constructor.
     * 
     * @param generator The Jackson JsonGenerator for FormIO.
     * @throws IOException if something goes wrong.
     */
    @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
    public DataUrl(JsonGenerator generator) throws IOException {
      super(generator);
    }

    /**
     * Output a url field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public DataUrl withUrl(final String value) throws IOException {
      return with("url", value);
    }

    /**
     * Output a headers field.
     * @return a newly created {@link ComponentArray} to which {@link DataUrlHeader} instances should be added.
     * @throws IOException if the generator fails.
     */
    public ComponentArray addHeaders() throws IOException {
      generator.writeFieldName("headers");
      return new ComponentArray(generator);    
    }
  }

  /**
   * Details of select component validation.
   */
  public static class SelectValidation extends Validation {

    /**
     * Constructor.
     * 
     * @param generator The Jackson JsonGenerator for FormIO.
     * @throws IOException if something goes wrong.
     */
    @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
    public SelectValidation(JsonGenerator generator) throws IOException {
      super(generator);
    }
    
    /**
     * Output a onlyAvailableItems field.
     * @param value The value of the JSON field.
     * @return this, so that the object can be used in a fluent manner.
     * @throws IOException if the generator fails.
     */
    public SelectValidation withOnlyAvailableItems(final Boolean value) throws IOException {
      with("onlyAvailableItems", value);
      return this;
    }

  }
  
  /**
   * Constructor.
   * 
   * @param generator The Jackson JsonGenerator for FormIO.
   * @throws IOException if something goes wrong.
   */
  @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
  public Select(JsonGenerator generator) throws IOException {
    super(generator, "select");
  }

  /**
   * Output a dataSrc field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Select withDataSrc(final DataSrcType value) throws IOException {
    if (value != null) {
      return with("dataSrc", value.toString());
    }
    return this;
  }
  
  /**
   * Helper method to add a {@link DataValue} object.
   * @param label The {@link DataValue} label.
   * @param value The {@link DataValue} value.
   * @throws IOException if the generator fails.
   */
  public void addCompleteDataValue(String label, String value) throws IOException {
    try (DataValue dv = new DataValue(generator)) {
      dv.withLabel(label);
      dv.withValue(value);
    }
  }

  /**
   * Helper method to add a {@link DataUrlHeader} object.
   * @param key The {@link DataUrlHeader} key.
   * @param value The {@link DataUrlHeader} value.
   * @throws IOException if the generator fails.
   */
  public void addCompleteDataUrlHeader(String key, String value) throws IOException {
    try (DataUrlHeader header = new DataUrlHeader(generator)) {
      header.withKey(key);
      header.withValue(value);
    }
  }

  /**
   * Output a data field for {@link DataValues}.
   * @return a newly created {@link DataValues} object to which {@link DataValue} instances should be added.
   * @throws IOException if the generator fails.
   */
  public DataValues addDataValues() throws IOException {
    withDataSrc(DataSrcType.values);
    generator.writeFieldName("data");
    return new DataValues(generator);
  }

  /**
   * Output a data field for {@link DataUrl}.
   * @return a newly created {@link DataUrl} object which must be configured.
   * @throws IOException if the generator fails.
   */
  public DataUrl addDataUrl() throws IOException {
    withDataSrc(DataSrcType.url);
    generator.writeFieldName("data");
    return new DataUrl(generator);    
  }

  /**
   * Output a valueProperty field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Select withValueProperty(final String value) throws IOException {
    return with("valueProperty", value);
  }

  /**
   * Output a refreshOn field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Select withRefreshOn(final String value) throws IOException {
    return with("refreshOn", value);
  }

  /**
   * Output a filter field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Select withFilter(final String value) throws IOException {
    return with("filter", value);
  }

  /**
   * Output an authenticate field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Select withAuthenticate(final Boolean value) throws IOException {
    return with("authenticate", value);
  }

  /**
   * Output a template field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Select withTemplate(final String value) throws IOException {
    return with("template", value);
  }
  
  /**
   * Output a searchEnabled field.
   * @param value The value of the JSON field.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public Select withSearchEnabled(final Boolean value) throws IOException {
    return with("searchEnabled", value);
  }

  @Override
  public SelectValidation addValidate() throws IOException {
    generator.writeFieldName("validate");
    return new SelectValidation(generator);
  }  
  
}
