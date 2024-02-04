/*
 * Copyright (C) 2024 jtalbut
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
package uk.co.spudsoft.query.exec.procs.sort;

import java.io.Serializable;
import java.util.Comparator;
import uk.co.spudsoft.query.exec.DataRow;

/**
 *
 * @param <T> The type of the field extracted from the DataRow
 * @author jtalbut
 */
public abstract class AbstractDataRowFieldComparator<T extends Comparable<?>> implements Comparator<DataRow>, Serializable {

  private static final long serialVersionUID = 1L;
  
  protected final String field;
  protected int greaterResult;

  public AbstractDataRowFieldComparator(String field, boolean descending) {
    this.field = field;
    this.greaterResult = descending ? -1 : 1;
  }

  @Override
  @SuppressWarnings("unchecked")
  public int compare(DataRow o1, DataRow o2) {
    T v1 = (T) o1.get(field);
    T v2 = (T) o2.get(field);
    if (v1 == null) {
      return (v2 == null) ? 0 : 0 - greaterResult;       
    } else if (v2 == null) {
      return greaterResult;
    }
    return internalCompare(v1, v2) * greaterResult;
  }
  
  protected abstract int internalCompare(T v1, T v2);
  
}
