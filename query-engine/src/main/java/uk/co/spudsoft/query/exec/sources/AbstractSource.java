/*
 * Copyright (C) 2022 jtalbut
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
package uk.co.spudsoft.query.exec.sources;

import io.reactiverse.contextual.logging.ContextualData;
import uk.co.spudsoft.query.exec.SourceInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;

/**
 * Abstract class to aid the implementation of {@link uk.co.spudsoft.query.exec.SourceInstance} classes.
 * 
 * @author jtalbut
 */
public abstract class AbstractSource implements SourceInstance, SourceNameTracker {
  
  private final String name;

  /**
   * Constructor.
   * @param name the name of the data source, as used in logs and tracking.   
   */
  public AbstractSource(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void addNameToContextLocalData() {
    ContextualData.put(SourceInstance.SOURCE_CONTEXT_KEY, name);
  }
}
