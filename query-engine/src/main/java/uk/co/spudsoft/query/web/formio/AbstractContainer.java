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
 * @param <T> The concrete class the derives from this base class.
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "Data object purely for translating to JSON")
public abstract class AbstractContainer<T extends AbstractContainer<T>> extends Component<T> {
  
  private List<Component<?>> components;

  public AbstractContainer(String type) {
    super(type);
  }

  public List<Component<?>> getComponents() {
    return components;
  }

  public void setComponents(List<Component<?>> components) {
    this.components = components;
  }

  @SuppressWarnings("unchecked")
  public T withComponents(final List<Component<?>> value) {
    this.components = value;
    return (T) this;
  }
  
}
