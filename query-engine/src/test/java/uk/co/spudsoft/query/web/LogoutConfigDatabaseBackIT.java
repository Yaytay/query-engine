/*
 * Copyright (C) 2023 jtalbut
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
package uk.co.spudsoft.query.web;

import io.restassured.RestAssured;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.Main;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

/**
 *
 * @author jtalbut
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LogoutConfigDatabaseBackIT extends AbstractLoginLogoutTester {

  private static final Logger logger = LoggerFactory.getLogger(LogoutConfigDatabaseBackIT.class);

  private static final ServerProviderPostgreSQL postgres = new ServerProviderPostgreSQL().init();

  private static final String CONFS_DIR = "target/query-engine/samples-" + MethodHandles.lookup().lookupClass().getSimpleName().toLowerCase();

  @BeforeAll
  public void createDirs() {
    File confsDir = new File(CONFS_DIR);
    FileUtils.deleteQuietly(confsDir);
    confsDir.mkdirs();
  }

  @Test
  public void testLoginLogout() throws Exception {
    logger.debug("Running testLoginLogout");
    
    startOidc();

    Main main = new Main();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    main.testMain(new String[]{
      "--persistence.datasource.url=" + postgres.getJdbcUrl(),
       "--persistence.datasource.adminUser.username=" + postgres.getUser(),
       "--persistence.datasource.adminUser.password=" + postgres.getPassword(),
       "--persistence.datasource.schema=public",
       "--baseConfigPath=" + CONFS_DIR,
       "--jwt.acceptableIssuerRegexes[0]=.*",
       "--jwt.defaultJwksCacheDuration=PT1M",
       "--jwt.jwksEndpoints[0]=http://localhost:" + oidcPort + "/jwks_uri",
       "--logging.jsonFormat=false",
       "--logging.level.uk\\\\.co\\\\.spudsoft\\\\.query\\\\.exec=TRACE",
       "--sampleDataLoads[0].url=" + postgres.getVertxUrl(),
       "--sampleDataLoads[0].adminUser.username=" + postgres.getUser(),
       "--sampleDataLoads[0].adminUser.password=" + postgres.getPassword(),
       "--managementEndpoints[0]=up",
       "--managementEndpoints[2]=prometheus",
       "--managementEndpoints[3]=threads",
       "--managementEndpointPort=8001",
       "--managementEndpointUrl=http://localhost:8001/manage",
       "--session.purgeDelay=PT60S",
       "--session.requireSession=false",
       "--session.oauth.test.logoUrl=https://upload.wikimedia.org/wikipedia/commons/c/c2/GitHub_Invertocat_Logo.svg",
       "--session.oauth.test.authorizationEndpoint=http://localhost:" + oidcPort + "/auth",
       "--session.oauth.test.tokenEndpoint=http://localhost:" + oidcPort + "/token",
       "--session.oauth.test.endSessionEndpoint=http://localhost:" + oidcPort + "/logout",
       "--session.oauth.test.revocationEndpoint=http://localhost:" + oidcPort + "/backchannel-logout",
       "--session.oauth.test.scope=openid",
       "--session.oauth.test.credentials.id=test-client",
       "--session.oauth.test.credentials.secret=test-secret",
       "--session.oauth.test.pkce=true"
    }, stdout, System.getenv());
    assertEquals(0, stdoutStream.size());

    RestAssured.port = main.getPort();
    
    logger.info("Main port: {}", main.getPort());
    logger.info("OIDC port: {}", oidc.getActualPort());

    Map<String, String> cookies = new HashMap<>();
    performLogin(main.getPort(), "openid profile", cookies);

    performStandardLoggedInOperations(cookies, postgres.getName(), postgres.getPort());
    
    performBackChannelLogout(cookies);

    oidc.stop();
    main.shutdown();
  }


}
