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

  public enum DataSrcType {
    values, json, url, resource, customer
  }
  
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

    public DataValue withLabel(final String value) throws IOException {
      return with("label", value);
    }

    public DataValue withValue(final String value) throws IOException {
      return with("value", value);
    }
  }
  
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

    public ComponentArray addValues() throws IOException {
      generator.writeFieldName("values");
      return new ComponentArray(generator);    
    }
  }
  
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

    public DataUrlHeader withKey(final String value) throws IOException {
      return with("key", value);
    }

    public DataUrlHeader withValue(final String value) throws IOException {
      return with("value", value);
    }

  }
  
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

    public DataUrl withUrl(final String value) throws IOException {
      return with("url", value);
    }

    public ComponentArray addHeaders() throws IOException {
      generator.writeFieldName("headers");
      return new ComponentArray(generator);    
    }
  }

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

  public Select withDataSrc(final DataSrcType value) throws IOException {
    if (value != null) {
      return with("dataSrc", value.toString());
    }
    return this;
  }
  
  public void addCompleteDataValue(String label, String value) throws IOException {
    try (DataValue dv = new DataValue(generator)) {
      dv.withLabel(label);
      dv.withValue(value);
    }
  }

  public void addCompleteDataUrlHeader(String key, String value) throws IOException {
    try (DataUrlHeader header = new DataUrlHeader(generator)) {
      header.withKey(key);
      header.withValue(value);
    }
  }

  public DataValues addDataValues() throws IOException {
    withDataSrc(DataSrcType.values);
    generator.writeFieldName("data");
    return new DataValues(generator);
  }

  public DataUrl addDataUrl() throws IOException {
    withDataSrc(DataSrcType.url);
    generator.writeFieldName("data");
    return new DataUrl(generator);    
  }

  public Select withValueProperty(final String value) throws IOException {
    return with("valueProperty", value);
  }

  public Select withRefreshOn(final String value) throws IOException {
    return with("refreshOn", value);
  }

  public Select withFilter(final String value) throws IOException {
    return with("filter", value);
  }

  public Select withAuthenticate(final Boolean value) throws IOException {
    return with("authenticate", value);
  }

  public Select withTemplate(final String value) throws IOException {
    return with("template", value);
  }
  
  public Select withSearchEnabled(final Boolean value) throws IOException {
    return with("searchEnabled", value);
  }

  @Override
  public SelectValidation addValidate() throws IOException {
    generator.writeFieldName("validate");
    return new SelectValidation(generator);
  }  
  
}
