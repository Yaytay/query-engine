/*
 * Copyright (C) 2025 jtalbut
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
package uk.co.spudsoft.query.exec.conditions;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.jexl3.JexlArithmetic;
import static org.apache.commons.jexl3.JexlEngine.TRY_FAILED;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlOperator;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import uk.co.spudsoft.query.exec.DataRow;

/**
 * Uberspector to allow JEXL to correctly work with DataRow objects.
 * @author jtalbut
 */
// Java
public final class DataRowUberspect implements JexlUberspect {

  private final JexlUberspect delegate;

  /**
   * Constructor.
   * @param delegate The standard (presumably) Uberspector to delegate most calls to.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Most calls to this class are passed on to the delegate")
  public DataRowUberspect(JexlUberspect delegate) {
    this.delegate = delegate;
  }

  @Override
  public JexlArithmetic.Uberspect getArithmetic(JexlArithmetic arithmetic) {
    return delegate.getArithmetic(arithmetic);
  }

  @Override
  public ClassLoader getClassLoader() {
    return delegate.getClassLoader();
  }

  @Override
  public JexlMethod getConstructor(Object ctorHandle, Object... args) {
    return delegate.getConstructor(ctorHandle, args);
  }

  @Override
  public Iterator<?> getIterator(Object obj) {
    return delegate.getIterator(obj);
  }

  @Override
  public JexlMethod getMethod(Object obj, String method, Object... args) {
    return delegate.getMethod(obj, method, args);
  }

  @Override
  public JexlPropertyGet getPropertyGet(List<PropertyResolver> resolvers, Object obj, Object identifier) {
    if (obj instanceof DataRow row && identifier instanceof String name) {
      return new JexlPropertyGet() {
        @Override
        public Object invoke(Object o) {
          return row.get(name);
        }

        @Override
        public Object tryInvoke(Object o, Object key) {
          return (o instanceof DataRow row && name.equals(key)) ? row.get(name) : TRY_FAILED;
        }

        @Override
        public boolean isCacheable() {
          return true;
        }

        @Override
        public boolean tryFailed(Object rval) {
          return rval == TRY_FAILED;
        }

      };
    } else {
      return delegate.getPropertyGet(resolvers, obj, identifier);
    }
  }

  @Override
  public JexlPropertyGet getPropertyGet(Object obj, Object identifier) {
    return getPropertyGet(null, obj, identifier);
  }

  @Override
  public JexlPropertySet getPropertySet(List<PropertyResolver> resolvers, Object obj, Object identifier, Object arg) {
    if (obj instanceof DataRow row && identifier instanceof String name && arg instanceof Comparable<?> comp) {
      return new JexlPropertySet() {
        @Override
        public Object invoke(Object obj, Object arg) {
          row.put(name, comp);
          return row;
        }

        @Override
        public Object tryInvoke(Object obj, Object key, Object value) throws JexlException.TryFailed {
          if (obj instanceof DataRow row && name.equals(key) && value instanceof Comparable<?> comp) {
            row.put(name, comp);
            return obj;
          } else {
            return TRY_FAILED;
          }
        }

        @Override
        public boolean isCacheable() {
          return true;
        }

        @Override
        public boolean tryFailed(Object rval) {
          return rval == TRY_FAILED;
        }

      };
    } else {
      return delegate.getPropertySet(resolvers, obj, identifier, arg);
    }
  }

  @Override
  public JexlPropertySet getPropertySet(Object obj, Object identifier, Object arg) {
    return getPropertySet(null, obj, identifier, arg);
  }

  @Override
  public List<PropertyResolver> getResolvers(JexlOperator op, Object obj) {
    return delegate.getResolvers(op, obj);
  }

  @Override
  public int getVersion() {
    return delegate.getVersion();
  }

  @Override
  public void setClassLoader(ClassLoader loader) {
    delegate.setClassLoader(loader);
  }


}

