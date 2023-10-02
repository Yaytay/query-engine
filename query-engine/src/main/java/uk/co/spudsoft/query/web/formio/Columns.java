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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 *
 * @author njt
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Data object purely for translating to JSON")
public class Columns extends Component<Columns> {

  public static class Column {
    private List<Component<?>> components;
    private int width = 6;
    private Integer offset;
    private Integer push;
    private Integer pull;
    private String size;

    public List<Component<?>> getComponents() {
      return components;
    }

    public void setComponents(List<Component<?>> components) {
      this.components = components;
    }

    public int getWidth() {
      return width;
    }

    public void setWidth(int width) {
      this.width = width;
    }

    public Integer getOffset() {
      return offset;
    }

    public void setOffset(Integer offset) {
      this.offset = offset;
    }

    public Integer getPush() {
      return push;
    }

    public void setPush(Integer push) {
      this.push = push;
    }

    public Integer getPull() {
      return pull;
    }

    public void setPull(Integer pull) {
      this.pull = pull;
    }

    public String getSize() {
      return size;
    }

    public void setSize(String size) {
      this.size = size;
    }
    
    public Column withComponents(final List<Component<?>> value) {
      this.components = value;
      return this;
    }

    public Column withWidth(final int value) {
      this.width = value;
      return this;
    }

    public Column withOffset(final Integer value) {
      this.offset = value;
      return this;
    }

    public Column withPush(final Integer value) {
      this.push = value;
      return this;
    }

    public Column withPull(final Integer value) {
      this.pull = value;
      return this;
    }

    public Column withSize(final String value) {
      this.size = value;
      return this;
    }
    
  }
  
  private List<Column> columns;
  
  public Columns() {
    super("columns");
  }

  public List<Column> getColumns() {
    return columns;
  }

  public void setColumns(List<Column> columns) {
    this.columns = columns;
  }

  public Columns withColumns(final List<Column> value) {
    this.columns = value;
    return this;
  }

}
