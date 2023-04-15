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
package uk.co.spudsoft.query.main;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jtalbut
 */
public class ImmutableCollectionTools {
  
  public static <K, V> ImmutableMap<K, V> copy(Map<K, V> src) {
    if (src == null) {
      return ImmutableMap.of();
    } else {
      return ImmutableMap.copyOf(src);
    }
  }
  
  public static <V> ImmutableList<V> copy(List<V> src) {
    if (src == null) {
      return ImmutableList.of();
    } else {
      return ImmutableList.copyOf(src);
    }
  }
  
}