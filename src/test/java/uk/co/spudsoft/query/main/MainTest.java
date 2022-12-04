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

import brave.http.HttpTracing;
import java.io.File;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 */
public class MainTest {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(MainTest.class);
  
  @BeforeAll
  public static void createDirs() {
    File paramsDir = new File("target/query-engine");
    paramsDir.mkdirs();
  }
  
  @Test
  public void testMainExitOnRun() throws Exception {
    Main main = new Main();
    main.testMain(new String[]{
            "exitOnRun"
            , "baseConfigPath=target/query-engine"
            , "acceptableIssuerRegexes[0]=.*"
            , "logging.level.uk_co_spudsoft_query_main=TRACE" 
            , "vertxOptions.tracingOptions.serviceName=Query-Engine"
    });
  }
  
  @Test
  public void testMain() throws IOException {
    Main.main(new String[]{
            "baseConfigPath=target/query-engine"
            , "acceptableIssuerRegexes[0]=.*"
    });
  }
  
  @Test
  public void testZipkinConfig() {
    ZipkinConfig config = new ZipkinConfig();
    assertNull(Main.buildZipkinTrace(null));
    assertNull(Main.buildZipkinTrace(config));
    config.setBaseUrl("http://baseurl/");
    config.setServiceName(null);
    HttpTracing tracing = Main.buildZipkinTrace(config);
    assertNotNull(tracing);
    config.setBaseUrl("http://baseurl");
    config.setServiceName("wibble");
    tracing = Main.buildZipkinTrace(config);
    assertNotNull(tracing);
  }
  
}
