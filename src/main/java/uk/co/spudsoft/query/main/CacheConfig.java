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

/**
 *
 * @author jtalbut
 */
public class CacheConfig {
  
  private int maxItems = 100;
  private int maxDurationMs = -1;
  private int purgePeriodMs = 3600 * 1000; // Hourly

  public int getMaxItems() {
    return maxItems;
  }

  public int getMaxDurationMs() {
    return maxDurationMs;
  }

  public int getPurgePeriodMs() {
    return purgePeriodMs;
  }
  
  public CacheConfig setMaxItems(int maxItems) {
    this.maxItems = maxItems;
    return this;
  }

  public CacheConfig setMaxDurationMs(int maxDurationMs) {
    this.maxDurationMs = maxDurationMs;
    return this;
  }

  public CacheConfig setPurgePeriodMs(final int value) {
    this.purgePeriodMs = value;
    return this;
  }
  
  
    
}
