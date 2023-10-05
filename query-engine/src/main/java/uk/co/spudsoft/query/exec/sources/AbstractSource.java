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

import io.vertx.core.Context;
import uk.co.spudsoft.query.exec.SourceInstance;
import uk.co.spudsoft.query.exec.SourceNameTracker;

/**
 *
 * @author jtalbut
 */
public abstract class AbstractSource implements SourceInstance, SourceNameTracker {
  
  private final String name;

  public AbstractSource(String name) {
    this.name = name;
  }

  @Override
  public final String getName() {
    return name;
  }

  @Override
  public void addNameToContextLocalData(Context context) {
    if (context != null) {
      context.putLocal(SourceInstance.SOURCE_CONTEXT_KEY, name);
    }
  }
}
