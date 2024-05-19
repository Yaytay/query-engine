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
package uk.co.spudsoft.query.exec.procs.script;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import uk.co.spudsoft.query.exec.DataRow;

/**
 * A <a href="https://www.graalvm.org/">GraalVM</a>  {@link org.graalvm.polyglot.proxy.ProxyObject} to represent a 
 * {@link uk.co.spudsoft.query.exec.DataRow} in <a href="https://www.graalvm.org/latest/reference-manual/polyglot-programming/">polyglot</a> scripts. 
 * <P>
 * The DataRow wrapped by this object can be updated by the {@link #putMember(java.lang.String, org.graalvm.polyglot.Value)} method.
 * 
 * @author jtalbut
 */
public class ProxyDataRow implements ProxyObject {
  
  private final DataRow dataRow;

  /**
   * Constructor.
   * @param dataRow The {@link DataRow} being wrapped.
   */
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "DataRow should only be modified on context")
  public ProxyDataRow(DataRow dataRow) {
    this.dataRow = dataRow;
  }
  
  @Override
  public Object getMember(String key) {
    return dataRow.get(key);
  }

  @Override
  public Object getMemberKeys() {
    return dataRow.keySet();
  }

  @Override
  public boolean hasMember(String key) {
    return dataRow.containsKey(key);
  }

  @Override
  public void putMember(String key, Value value) {
    dataRow.put(key, ProcessorScriptInstance.mapToNativeObject(value));
  }
  
}
