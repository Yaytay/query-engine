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

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 *
 * @author njt
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Data object purely for translating to JSON")
public class Select extends Component<Select> {

  public static enum DataSrcType {
    values, json, url, resource, customer
  }
  
  public static class DataValue {
    private String label;
    private String value;

    public String getLabel() {
      return label;
    }

    public void setLabel(String label) {
      this.label = label;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public DataValue withLabel(final String value) {
      this.label = value;
      return this;
    }

    public DataValue withValue(final String value) {
      this.value = value;
      return this;
    }
  }
  
  public static class DataUrlHeader {
    private String key;
    private String value;

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public DataUrlHeader withKey(final String value) {
      this.key = value;
      return this;
    }

    public DataUrlHeader withValue(final String value) {
      this.value = value;
      return this;
    }

  }
  
  public static class DataUrl {
    private String url;
    private DataUrlHeader headers;

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public DataUrlHeader getHeaders() {
      return headers;
    }

    public void setHeaders(DataUrlHeader headers) {
      this.headers = headers;
    }

    public DataUrl withUrl(final String value) {
      this.url = value;
      return this;
    }

    public DataUrl withHeaders(final DataUrlHeader value) {
      this.headers = value;
      return this;
    }
    
    
  }
  
  private DataSrcType dataSrc;
  private List<DataValue> dataValues;
  private DataUrl dataUrl;
  private String valueProperty;
  private String refreshOn;
  private String filter;
  private Boolean authenticate;
  private String template;
  
  public Select() {
    super("select");
  }

  public DataSrcType getDataSrc() {
    return dataSrc;
  }

  public void setDataSrc(DataSrcType dataSrc) {
    this.dataSrc = dataSrc;
  }

  @JsonProperty("data")
  public Object getDataValues() {
    switch(dataSrc) {
    case values:
      return dataValues;
    case url:
      return dataUrl;
    default:
      return null;
    }
  }

  public void setData(List<DataValue> dataValues) {
    this.dataSrc = DataSrcType.values;
    this.dataValues = dataValues;
  }

  public void setData(DataUrl dataUrl) {
    this.dataSrc = DataSrcType.url;
    this.dataUrl = dataUrl;
  }

  public String getValueProperty() {
    return valueProperty;
  }

  public void setValueProperty(String valueProperty) {
    this.valueProperty = valueProperty;
  }

  public String getRefreshOn() {
    return refreshOn;
  }

  public void setRefreshOn(String refreshOn) {
    this.refreshOn = refreshOn;
  }

  public String getFilter() {
    return filter;
  }

  public void setFilter(String filter) {
    this.filter = filter;
  }

  public Boolean getAuthenticate() {
    return authenticate;
  }

  public void setAuthenticate(Boolean authenticate) {
    this.authenticate = authenticate;
  }

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }

  public Select withDataSrc(final DataSrcType value) {
    this.dataSrc = value;
    return this;
  }

  public Select withData(final List<DataValue> value) {
    this.dataValues = value;
    return this;
  }

  public Select withData(final DataUrl value) {
    this.dataUrl = value;
    return this;
  }

  public Select withValueProperty(final String value) {
    this.valueProperty = value;
    return this;
  }

  public Select withRefreshOn(final String value) {
    this.refreshOn = value;
    return this;
  }

  public Select withFilter(final String value) {
    this.filter = value;
    return this;
  }

  public Select withAuthenticate(final Boolean value) {
    this.authenticate = value;
    return this;
  }

  public Select withTemplate(final String value) {
    this.template = value;
    return this;
  }

  
  
}
