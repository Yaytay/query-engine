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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Helper class of methods to help with handling Immutable collections from the <a href="https://github.com/google/guava">Guava</a> library.
 * @author jtalbut
 */
public class ImmutableCollectionTools {

  private ImmutableCollectionTools() {
  }
  
  /**
   * Null-safe method for copying a {@link java.util.Map} to a {@link com.google.common.collect.ImmutableMap}.
   * @param <K> The type of the key in the input and output map.
   * @param <V> The type of item in the input and output map.
   * @param src The input {@link java.util.Map}.
   * @return An ImmutableMap containing the same items as src.
   * 
   */
  public static <K, V> ImmutableMap<K, V> copy(Map<K, V> src) {
    if (src == null) {
      return ImmutableMap.of();
    } else {
      return ImmutableMap.copyOf(src);
    }
  }
  
  /**
   * Create an {@link com.google.common.collect.ImmutableMap} based on the contents of a Collection.
   * 
   * Note that if the id function returns null for a value that value will be excluded from the Map.
   * Please ensure that validation of the Collection happens to alert the user to this issue.
   * 
   * If the src collection is null an empty ImmutableMap will be returned.
   * 
   * @param <K> The key type for the Map.
   * @param <V> The value type for the Map.
   * @param src The collection being converted.
   * @param id Function to extract the ID from a Collection entry.
   * @return A newly created ImmutableMap.
   */
  public static <K, V> ImmutableMap<K, V> listToMap(Collection<V> src, Function<V, K> id) {
    if (src == null) {
      return ImmutableMap.of();
    } else {
      var builder = ImmutableMap.<K, V>builder();
      src.forEach(v -> {
        K key = id.apply(v);
        if (key != null) {
          builder.put(key, v);
        }
      });
      return builder.build();
    }
  }
  
  /**
   * Null-safe method for copying a {@link java.util.List} to a {@link com.google.common.collect.ImmutableList}.
   * @param <V> The type of item in the input collection.
   * @param src The input {@link java.util.List}.
   * @return An ImmutableList containing the same items as src.
   */
  public static <V> ImmutableList<V> copy(List<V> src) {
    if (src == null) {
      return ImmutableList.of();
    } else {
      return ImmutableList.copyOf(src);
    }
  }
  
}
