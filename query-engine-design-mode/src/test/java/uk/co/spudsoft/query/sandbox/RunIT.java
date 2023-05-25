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
package uk.co.spudsoft.query.sandbox;

import uk.co.spudsoft.query.main.*;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

import uk.co.spudsoft.query.testcontainers.ServerProviderMsSQL;
import uk.co.spudsoft.query.testcontainers.ServerProviderMySQL;


/**
 * An Integration Test that serves purely to bring up the Query Engine and keep it up until explicitly killed.
 * Provides a simple way to do demos, manual testing or UI development.
 * @author jtalbut
 */
@ExtendWith(VertxExtension.class)
public class RunIT {
  
  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();
  private static final ServerProviderMySQL mysql = new ServerProviderMySQL().init();
  private static final ServerProviderMsSQL mssql = new ServerProviderMsSQL().init();
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(RunIT.class);
  
  @BeforeAll
  public static void createDirs(Vertx vertx, VertxTestContext testContext) {
    File paramsDir = new File("target/query-engine/samples");
    paramsDir.mkdirs();
    postgres.prepareTestDatabase(vertx)
            .compose(v -> mysql.prepareTestDatabase(vertx))
            .compose(v -> mssql.prepareTestDatabase(vertx))
            .onComplete(testContext.succeedingThenComplete())
            ;
}
    
  @Test
  public void testMainDaemon() throws Exception {
    Main main = new DesignMain();
    main.testMain(new String[]{
      "audit.datasource.url=jdbc:" + postgres.getUrl()
      , "audit.datasource.adminUser.username=" + postgres.getUser()
      , "audit.datasource.adminUser.password=" + postgres.getPassword()
      , "audit.datasource.schema=public" 
      , "baseConfigPath=target/test-classes/sources"
      , "vertxOptions.tracingOptions.serviceName=Query-Engine"
      , "jwt.acceptableIssuerRegexes[0]=.*"
      , "logging.jsonFormat=false"
      , "logging.level.uk\\\\.co\\\\.spudsoft\\\\.vertx\\\\.rest=TRACE"
//      , "logging.level.uk\\\\.co\\\\.spudsoft\\\\.query\\\\.exec\\\\.procs\\\\.query=TRACE"
      , "httpServerOptions.port=8000"
    });
    
    for (int i = 0; i < 14400; ++i) {
      try {
        Thread.sleep(1000);
      } catch(InterruptedException ex) {
        
      }
    }
    
  }
  
}
