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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import inet.ipaddr.IPAddressString;
import io.vertx.core.http.HttpServerRequest;
import java.awt.Point;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import static org.apache.commons.jexl3.JexlEngine.TRY_FAILED;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import uk.co.spudsoft.query.exec.DataRow;
import uk.co.spudsoft.query.exec.Types;
import uk.co.spudsoft.query.exec.context.RequestContext;

/**
 * Unit tests for DataRowUberspect to verify dot-notation get/set on DataRow in JEXL.
 */
public class DataRowUberspectTest {

  private JexlEngine buildEngineWithDataRowUberspect() {
    // Build a default engine to obtain its default uberspect as delegate
    JexlEngine base = new JexlBuilder().create();
    JexlUberspect delegate = base.getUberspect();
    // Wrap with our DataRowUberspect
    return new JexlBuilder()
        .uberspect(new DataRowUberspect(delegate))
        .create();
  }

  @Test
  public void testGetPropertyFromDataRow() {
    JexlEngine jexl = buildEngineWithDataRowUberspect();

    Types types = new Types();
    DataRow row = DataRow.create(types)
        .put("name", "Alice")
        .put("age", 30);

    JexlContext ctx = new MapContext();
    ctx.set("row", row);

    Object name = jexl.createExpression("row.name").evaluate(ctx);
    Object age = jexl.createExpression("row.age").evaluate(ctx);

    assertEquals("Alice", name);
    assertEquals(30, age);
  }

  @Test
  public void testSetPropertyOnDataRow() {
    JexlEngine jexl = buildEngineWithDataRowUberspect();

    Types types = new Types();
    DataRow row = DataRow.create(types)
        .put("price", 12)
        .put("qty", 5);

    JexlContext ctx = new MapContext();
    ctx.set("row", row);

    // Single-expression assignment; no semicolon/compound statements
    Object total = jexl.createExpression("(row.total = row.price * row.qty)").evaluate(ctx);

    assertEquals(60, total);
    assertEquals(60, row.get("total"));
  }

  @Test
  public void testSetThenReadMultipleFields() {
    JexlEngine jexl = buildEngineWithDataRowUberspect();

    Types types = new Types();
    DataRow row = DataRow.create(types)
        .put("first", "Ada")
        .put("last", "Lovelace");

    JexlContext ctx = new MapContext();
    ctx.set("row", row);

    // Single-expression assignment
    jexl.createExpression("(row.full = row.first + ' ' + row.last)").evaluate(ctx);

    assertEquals("Ada Lovelace", row.get("full"));
  }

  @Test
  public void testWorksAlongsideNormalObjects() {
    JexlEngine jexl = buildEngineWithDataRowUberspect();

    Types types = new Types();
    DataRow row = DataRow.create(types).put("x", 2).put("y", 3);

    JexlContext ctx = new MapContext();
    ctx.set("row", row);
    ctx.set("list", List.of(1, 2, 3));

    // First set row.sum, then evaluate a separate expression using list and row.sum
    jexl.createExpression("(row.sum = row.x + row.y)").evaluate(ctx);
    Object res = jexl.createExpression("list[1] + row.sum").evaluate(ctx);

    assertEquals(2 + 3 + 2, res);
    assertEquals(5, row.get("sum"));
  }

  @Test
  public void testContextIrrelevantObjectsDoNotBreak() {
    // Ensure unrelated objects in the context do not interfere with DataRow handling
    JexlEngine jexl = buildEngineWithDataRowUberspect();

    Types types = new Types();
    DataRow row = DataRow.create(types).put("v", 7);

    JexlContext ctx = new MapContext();
    ctx.set("row", row);
    // Something arbitrary, e.g. a RequestContext instance
    ctx.set("req", new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null));

    Object v = jexl.createExpression("row.v").evaluate(ctx);
    assertEquals(7, v);
    assertTrue(row.containsKey("v"));
  }
  
  private static JexlUberspect newUberspect() {
    JexlPermissions permissions = JexlPermissions.RESTRICTED
            .compose(
                    "io.vertx.core.http.impl.headers.*",
                     "uk.co.spudsoft.jwtvalidatorvertx.*",
                     "uk.co.spudsoft.query.exec.DataRow",
                     "uk.co.spudsoft.query.exec.conditions.*",
                     "uk.co.spudsoft.query.exec.context.*",
                     "java.time.*"
            );

    Log log = LogFactory.getLog(DataRowUberspectTest.class);

    return new Uberspect(log, null, permissions);
  }
  
  @Test
  public void testGetClassLoader() {
    DataRowUberspect dru = new DataRowUberspect(newUberspect());
    assertNotNull(dru.getClassLoader());
    dru.setClassLoader(this.getClass().getClassLoader());
    assertEquals(this.getClass().getClassLoader(), dru.getClassLoader());
  }
  
  @Test
  public void testGetIterator() {
    DataRowUberspect dru = new DataRowUberspect(newUberspect());
    assertNotNull(dru.getIterator(Arrays.asList(1,2,3)));
  }
  
  @Test
  public void testGetConstructor() {
    
    Map<String, String> environment = new HashMap<>();
    HttpServerRequest request = mock(HttpServerRequest.class);
    
    DataRowUberspect dru = new DataRowUberspect(newUberspect());
    assertNotNull(dru.getConstructor(RequestContext.class, environment, request));
  }
  
  @Test
  public void testPropertyManually() {
    DataRowUberspect dru = new DataRowUberspect(newUberspect());
    Types types = new Types();
    DataRow dr = DataRow.create(types);
    dr.put("field1", 3);
    dr.put("field2", "bob");
    
    JexlPropertyGet getter = dru.getPropertyGet(dr, "field1");
    assertTrue(getter.isCacheable());
    assertEquals(3, getter.tryInvoke(dr, "field1"));
    assertEquals(TRY_FAILED, getter.tryInvoke(dr, "field2"));
    assertEquals(TRY_FAILED, getter.tryInvoke(dr, "field3"));
    assertEquals(TRY_FAILED, getter.tryInvoke(new HashMap<>(), "field3"));
    assertTrue(getter.tryFailed(TRY_FAILED));
    assertFalse(getter.tryFailed(3));
    
    JexlPropertySet setter = dru.getPropertySet(dr, "field1", 4);
    assertTrue(setter.isCacheable());
    assertEquals(dr, setter.tryInvoke(dr, "field1", 4));
    assertEquals(TRY_FAILED, setter.tryInvoke(dr, "field2", "fred"));
    assertEquals(TRY_FAILED, setter.tryInvoke(dr, "field1", new Point(0, 0)));
    assertEquals(TRY_FAILED, setter.tryInvoke(new HashMap<>(), "field2", 8));
    assertTrue(setter.tryFailed(TRY_FAILED));
    assertFalse(setter.tryFailed(dr));

    Map<String, String> map = new HashMap<>();
    setter = dru.getPropertySet(map, "field1", 4);
    assertNotNull(setter);
    assertTrue(setter.isCacheable());    
    
    assertNull(dru.getPropertyGet(dr, Long.MIN_VALUE));
    assertNotNull(dru.getPropertyGet(new HashMap<>(), "key"));

    assertNull(dru.getPropertySet(dr, Long.MIN_VALUE, 9L));
    assertNull(dru.getPropertySet(dr, "field1", new Point(0,0)));

  }
  
}
