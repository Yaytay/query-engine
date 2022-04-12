/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main;

import java.io.File;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.main.testcontainers.ServerProviderPostgreSQL;

/**
 *
 * @author jtalbut
 */
public class MainIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  
  @Test
  public void testBadAudit() {
    File paramsDir = new File("target/query-engine");
    paramsDir.mkdirs();
    
    Main main = new Main();
    main.testMain(new String[]{
      "audit.datasource.url=wibble"
    });
    
    
    main.shutdown();
  }
  
  @Test
  public void testMainDaemon() {
    File paramsDir = new File("target/query-engine");
    paramsDir.mkdirs();
    
    Main main = new Main();
    main.testMain(new String[]{
      "audit.datasource.url=jdbc:" + postgres.getUrl()
      , "audit.datasource.adminUser.username=" + postgres.getUser()
      , "audit.datasource.adminUser.password=" + postgres.getPassword()
    });
    
    
    main.shutdown();
  }
  
}
