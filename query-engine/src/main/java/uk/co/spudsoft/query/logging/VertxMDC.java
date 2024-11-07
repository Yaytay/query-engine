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
package uk.co.spudsoft.query.logging;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.spi.MDCAdapter;

/**
 * Instance of {@link org.slf4j.spi.MDCAdapter} that stores data in the Vertx context.
 * 
 * Logback can be configured to use this MDCAdapter by using {@link ch.qos.logback.classic.LoggerContext#setMDCAdapter(org.slf4j.spi.MDCAdapter)} 
 * but it is not currently possible to replace the MDCAdapter used by {@link org.slf4j.MDC}
 * (because slf4j gets it from {@link ch.qos.logback.classic.spi.LogbackServiceProvider} very early in the process lifecycle - the first time a static LoggerFactory is seen).
 *
 * To work around this just explicitly write to VertxMDC instead of MDC.
 * 
 * @author jtalbut
 */
public class VertxMDC implements MDCAdapter {

  private static final String VERTX_KEY = VertxMDC.class.getCanonicalName();
  
  /**
   * The singleton instance of VertxMDC that clients should use.
   */
  public static final VertxMDC INSTANCE = new VertxMDC();

  /**
   * Constructor.
   */
  public VertxMDC() {
  }
  
  private static class MdcData {
    private final Map<String, String> mdcReadWriteMap = new HashMap<>();
    private final Map<String, Deque<String>> mdcMapOfStacks = new HashMap<>();    
    private Map<String, String> mdcReadOnlyMap;
  }

  private MdcData getMdcData() {
    Context context = Vertx.currentContext();
    if (context != null) {
      return context.get(VERTX_KEY);      
    }
    return null;
  }
  
  private static MdcData getOrCreateMdcData() {
    Context context = Vertx.currentContext();
    if (context != null) {
      MdcData data = context.get(VERTX_KEY);
      if (data == null) {
        data = new MdcData();
        context.put(VERTX_KEY, data);
      }
      return data;
    }
     return null;
  }
  
  @Override
  public void put(String key, String value) {
    MdcData data = getOrCreateMdcData();
    if (data != null) {
      data.mdcReadWriteMap.put(key, value);
      data.mdcReadOnlyMap = null;
    }
  }

  @Override
  public String get(String key) {
    MdcData data = getMdcData();
    if (data != null) {
      return data.mdcReadWriteMap.get(key);
    }
    return null;
  }

  @Override
  public void remove(String key) {
    MdcData data = getMdcData();
    if (data != null) {
      data.mdcReadWriteMap.remove(key);
      data.mdcReadOnlyMap = null;
    }
  }

  @Override
  public void clear() {
    MdcData data = getMdcData();
    if (data != null) {
      data.mdcReadWriteMap.clear();
      data.mdcReadOnlyMap = null;
    }
  }

  @Override
  public Map<String, String> getCopyOfContextMap() {
    MdcData data = getMdcData();
    if (data == null) {
      return Collections.emptyMap();
    } 
    if (data.mdcReadOnlyMap == null) {
      data.mdcReadOnlyMap = ImmutableMap.copyOf(data.mdcReadWriteMap);
    }
    return data.mdcReadOnlyMap;
  }

  @Override
  public void setContextMap(Map<String, String> map) {
    MdcData data = getOrCreateMdcData();
    data.mdcReadWriteMap.clear();
    data.mdcReadWriteMap.putAll(map);
    data.mdcReadOnlyMap = null;
  }

  @Override
  public void pushByKey(String key, String value) {
    MdcData data = getOrCreateMdcData();
    if (data != null) {
      Deque<String> stack = data.mdcMapOfStacks.get(key);
      if (stack == null) {
        stack = new ArrayDeque<>();
        data.mdcMapOfStacks.put(key, stack);
      }
      stack.push(value);
    }
  }

  @Override
  public String popByKey(String key) {
    MdcData data = getOrCreateMdcData();
    if (data != null) {
      Deque<String> stack = data.mdcMapOfStacks.get(key);
      if (stack == null) {
        stack = new ArrayDeque<>();
        data.mdcMapOfStacks.put(key, stack);
      }
      if (stack.isEmpty()) {
        return null;
      } 
      return stack.pop();
    }
    return null;
  }

  @Override
  public Deque<String> getCopyOfDequeByKey(String key) {
    MdcData data = getOrCreateMdcData();
    if (data != null) {
      Deque<String> stack = data.mdcMapOfStacks.get(key);
      if (stack != null) {
        return new ArrayDeque<>(stack);
      }
    }
    return new ArrayDeque<>();
  }

  @Override
  public void clearDequeByKey(String key) {
    MdcData data = getOrCreateMdcData();
    if (data != null) {
      Deque<String> stack = data.mdcMapOfStacks.get(key);
      if (stack != null) {
        stack.clear();
      }
    }
  }
  
}
