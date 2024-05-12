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

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Configuration data for caches.
 * <P>
 * May specify either or both or maxItems and maxDuration.
 * <P>
 * If the maxDuration is specified as zero caching will be effectively disabled (regardless of the value of maxItems).
 * If maxItems is less than 1 the configuration is invalid.
 * If maxDuration is negative the configuration is invalid.
 * If purgePeriod is not positive the configuration is invalid.
 * 
 * @author jtalbut
 */
public class CacheConfig {
  
  private int maxItems = 100;
  private Duration maxDuration = null;
  private Duration purgePeriod = Duration.of(1, ChronoUnit.HOURS);

  /**
   * Get the maximum number of items that should be stored in the cache.
   * <P>
   * When more items than this are in the cache they are purged eldest first.
   * <P>
   * Must be >= 1.
   * @return the maximum number of items that should be stored in the cache.
   */
  public int getMaxItems() {
    return maxItems;
  }

  /**
   * Set the maximum number of items that should be stored in the cache.
   * <P>
   * When more items than this are in the cache they are purged eldest first.
   * <P>
   * Must be >= 1.
   * @param value the maximum number of items that should be stored in the cache.
   */
  public void setMaxItems(final int value) {
    this.maxItems = value;
  }

  /**
   * Get the maximum age of items in the cache.
   * <P>
   * Items older than this are purged from the cache.
   * <P>
   * If set to zero the cache will only ever contain a single item.
   * Must not be negative.
   * <P>
   * Configuration files should specify this using <a href="https://en.wikipedia.org/wiki/ISO_8601#Durations">ISO860 Duration</a> format, i.e. PT10S.
   * @return the maximum age of items in the cache.
   */
  public Duration getMaxDuration() {
    return maxDuration;
  }

  /**
   * Set the maximum age of items in the cache.
   * <P>
   * Items older than this are purged from the cache.
   * <P>
   * If set to zero the cache will only ever contain a single item.
   * If set, must not be negative.
   * <P>
   * Configuration files should specify this using <a href="https://en.wikipedia.org/wiki/ISO_8601#Durations">ISO860 Duration</a> format, i.e. PT10S.
   * @param value the maximum age of items in the cache.
   */
  public void setMaxDuration(final Duration value) {
    this.maxDuration = value;
  }

  /**
   * Get the purge period for the cache.
   * <P>
   * If the purge period is null items will only be removed from the cache when they expire and space is required.
   * When purge period is set a scheduled task runs that explicitly removes from the cache any invalid items.
   * <P>
   * If set, must be positive.
   * <P>
   * Configuration files should specify this using <a href="https://en.wikipedia.org/wiki/ISO_8601#Durations">ISO860 Duration</a> format, i.e. PT10S.
   * @return the purge period for the cache.
   */
  public Duration getPurgePeriod() {
    return purgePeriod;
  }

  /**
   * Set the purge period for the cache.
   * <P>
   * If the purge period is null items will only be removed from the cache when they expire and space is required.
   * When purge period is set a scheduled task runs that explicitly removes from the cache any invalid items.
   * <P>
   * If set, must be positive.
   * <P>
   * Configuration files should specify this using <a href="https://en.wikipedia.org/wiki/ISO_8601#Durations">ISO860 Duration</a> format, i.e. PT10S.
   * @param value the purge period for the cache.
   */
  public void setPurgePeriod(final Duration value) {
    this.purgePeriod = value;
  }

  /**
   * Validate the provided values.
   * @param name
   * @throws IllegalArgumentException 
   */
  public void validate(String name) throws IllegalArgumentException {
    if (maxItems < 1) {
      throw new IllegalArgumentException(name + " configured with negative maxItems (" + maxItems + ")");
    }
    if (maxDuration != null && maxDuration.isNegative()) {
      throw new IllegalArgumentException(name + " configured with negative maxDuration (" + maxDuration + ")");
    }
    if (purgePeriod == null) {
      throw new IllegalArgumentException(name + " configured with no purgePeriod");
    } else if (!purgePeriod.isPositive()) {
      throw new IllegalArgumentException(name + " configured with purgePeriod that is not positive (" + purgePeriod + ")");
    }
  }
    
}
