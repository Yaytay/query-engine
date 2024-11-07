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

import io.opentelemetry.api.GlobalOpenTelemetry;
import uk.co.spudsoft.query.main.*;
import io.vertx.junit5.VertxExtension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

import uk.co.spudsoft.query.testcontainers.ServerProviderMsSQL;
import uk.co.spudsoft.query.testcontainers.ServerProviderMySQL;


/**
 * An Integration Test that serves purely to bring up the Query Engine and keep it up until explicitly killed.
 * <p>
 * Provides a simple way to do demos, manual testing or UI development.
 * <p>
 * If you try to run this from DesignMode you may run into issues with the version numbers of DesignMode and the base project not matching.
 * To work around this you will need to either:
 * <ul>
 * <li>Have done a build of the base project since the last git commit.
 * <li>Have fudged the dependency in the DesignNode pom.
 * </ul>
 * The former is much nicer.
 * 
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
  public static void createDirs() {
    File paramsDir = new File("target/query-engine/samples-runit");
    try {
      FileUtils.deleteDirectory(paramsDir);
    } catch (Exception ex) {
    }
    paramsDir.mkdirs();
}
    
  @Test
  public void testMainDaemon() throws Exception {
    // The MySQL JDBC driver used by TestContainers will call OpenTelementry.get() :(
    GlobalOpenTelemetry.resetForTest();
    Main main = new DesignMain();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
        "--persistence.datasource.url=" + postgres.getJdbcUrl()
      , "--persistence.datasource.adminUser.username=" + postgres.getUser()
      , "--persistence.datasource.adminUser.password=" + postgres.getPassword()
      , "--persistence.datasource.schema=public" 
      , "--baseConfigPath=target/query-engine/samples-runit"
      , "--jwt.acceptableIssuerRegexes[0]=.*"
      , "--jwt.jwksEndpoints[0]=" + System.getProperty("queryEngineEntraUrl").replace("v2.0", "discovery/v2.0/keys")
      , "--jwt.requiredAudiences[0]=query-engine"
      , "--jwt.requiredAudiences[1]=" + System.getProperty("queryEngineEntraId")
      , "--logging.jsonFormat=false"
      , "--logging.level.uk\\\\.co\\\\.spudsoft\\\\.query\\\\.exec=TRACE"
//      , "--logging.level.uk\\\\.co\\\\.spudsoft\\\\.query\\\\.exec\\\\.procs\\\\.query=TRACE"
      , "--sampleDataLoads[0].url=" + postgres.getVertxUrl()
      , "--sampleDataLoads[0].adminUser.username=" + postgres.getUser()
      , "--sampleDataLoads[0].adminUser.password=" + postgres.getPassword()
      , "--sampleDataLoads[1].url=" + mysql.getVertxUrl()
      , "--sampleDataLoads[1].user.username=" + mysql.getUser()
      , "--sampleDataLoads[1].user.password=" + mysql.getPassword()
      , "--sampleDataLoads[2].url=sqlserver://localhost:1234/test"
      , "--sampleDataLoads[2].adminUser.username=sa"
      , "--sampleDataLoads[2].adminUser.password=unknown"
      , "--httpServerOptions.port=8000"
      , "--secrets.tc-master.username=" + System.getProperty("tc-master-username")
      , "--secrets.tc-master.password=" + System.getProperty("tc-master-password")
      , "--secrets.tc-master.condition=true"
      , "--secrets.tc-demo-uat6.username=" + System.getProperty("tc-master-username")
      , "--secrets.tc-demo-uat6.password=" + System.getProperty("tc-master-password")
      , "--secrets.tc-demo-uat6.condition=true"
      // , "--managementEndpointPort=8001" 
      // , "--managementEndpointUrl=http://localhost:8001/manage"
      , "--corsAllowedOriginRegex=.*"
//      , "--session.requireSession=true"
//      , "--session.codeVerifierLength=30"
//      , "--session.oauth.Microsoft.logoUrl=https://upload.wikimedia.org/wikipedia/commons/4/44/Microsoft_logo.svg"
//      , "--session.oauth.Microsoft.issuer=" + System.getProperty("queryEngineEntraUrl")
//      , "--session.oauth.Microsoft.credentials.id=" + System.getProperty("queryEngineEntraId")
//      , "--session.oauth.Microsoft.credentials.secret=" + System.getProperty("queryEngineEntraSecret")
//      , "--session.oauth.Microsoft.scope=openid profile api://" + System.getProperty("queryEngineEntraId") + "/query-engine"
    }, stdout);
    
    for (int i = 0; i < 14400; ++i) {
      try {
        Thread.sleep(1000);
      } catch(InterruptedException ex) {
        
      }
    }
    
  }
  
}
