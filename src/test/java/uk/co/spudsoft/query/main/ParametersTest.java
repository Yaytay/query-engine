/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main;

import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author jtalbut
 */
public class ParametersTest {
  
  @Test
  public void testGetVertxOptions() {
    Parameters instance = new Parameters();
    assertNull(instance.getVertxOptions());
    instance.setVertxOptions(null);
    assertNull(instance.getVertxOptions());
    VertxOptions vo = new VertxOptions()
            .setHAGroup("haGroup")
            ;
    instance.setVertxOptions(vo);
    assertEquals(vo.toJson(), instance.getVertxOptions().toJson());
    assertEquals("haGroup", instance.getVertxOptions().getHAGroup());
  }

  @Test
  public void testGetHttpServerOptions() {
    Parameters instance = new Parameters();
    assertNull(instance.getHttpServerOptions());
    instance.setHttpServerOptions(null);
    assertNull(instance.getHttpServerOptions());
    HttpServerOptions hso = new HttpServerOptions()
            .setHost("host")
            ;
    instance.setHttpServerOptions(hso);
    assertEquals(hso.toJson(), instance.getHttpServerOptions().toJson());
    assertEquals("host", instance.getHttpServerOptions().getHost());
  }

  @Test
  public void testIsExitOnRun() {
    Parameters instance = new Parameters();
    assertFalse(instance.isExitOnRun());
    instance.setExitOnRun(true);
    assertTrue(instance.isExitOnRun());
  }

  @Test
  public void testGetBaseConfigPath() {
    Parameters instance = new Parameters();
    assertNull(instance.getBaseConfigPath());
    instance.setBaseConfigPath("baseConfigPath");
    assertEquals("baseConfigPath", instance.getBaseConfigPath());
  }

}
