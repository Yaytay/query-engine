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
 * Base class for outputting a formio container.
 * 
 * @author jtalbut
 * @param <T> The concrete class the derives from this base class.
 */
public abstract class AbstractContainer<T extends AbstractContainer<T>> extends Component<T> {
    
  /**
   * Constructor.
   * 
   * @param generator The Jackson JsonGenerator for FormIO.
   * @param type The type of container component.
   * @throws IOException if something goes wrong.
   */
  @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
  public AbstractContainer(JsonGenerator generator, String type) throws IOException {
    super(generator, type);
  }
  
  /**
   * Write a "components" field as a {@link ComponentArray}.
   * @return this, so that the object can be used in a fluent manner.
   * @throws IOException if the generator fails.
   */
  public ComponentArray addComponents() throws IOException {
    generator.writeFieldName("components");
    return new ComponentArray(generator);    
  }
  
}
