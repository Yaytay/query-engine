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
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Data object purely for translating to JSON")
public class Columns extends Component<Columns> {

  public static class Column extends AbstractComponent<Column> {
    
    @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
    protected Column(JsonGenerator generator) throws IOException {
      super(generator);
    }

    public ComponentArray addComponents() throws IOException {
      generator.writeFieldName("components");
      return new ComponentArray(generator);    
    }

    public Column withWidth(final int value) throws IOException {
      return with("width", value);
    }

    public Column withOffset(final Integer value) throws IOException {
      return with("offset", value);
    }

    public Column withPush(final Integer value) throws IOException {
      return with("push", value);
    }

    public Column withPull(final Integer value) throws IOException {
      return with("pull", value);
    }

    public Column withSize(final String value) throws IOException {
      return with("size", value);
    }
    
  }
  
  @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
  public Columns(JsonGenerator generator) throws IOException {
    super(generator, "columns");
  }

  public ComponentArray addColumns() throws IOException {
    generator.writeFieldName("columns");
    return new ComponentArray(generator);    
  }

}
