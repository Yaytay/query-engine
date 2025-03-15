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

import io.opentelemetry.api.GlobalOpenTelemetry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
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
  
  @Test
  public void testMainExitOnRun() throws Exception {
    Main main = new DesignMain();
    ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
    PrintStream stdout = new PrintStream(stdoutStream);
    GlobalOpenTelemetry.resetForTest();
    main.testMain(new String[]{
              "--exitOnRun"
            , "--baseConfigPath=target/query-engine/samples-maintest"
            , "--jwt.acceptableIssuerRegexes[0]=.*"
            , "--jwt.defaultJwksCacheDuration=PT1M"
    }, stdout, Collections.emptyMap());
  }
  
  @Test
  public void testMain() throws IOException {
    GlobalOpenTelemetry.resetForTest();
    Main.main(new String[]{
              "--baseConfigPath=target/query-engine/samples-maintest"
            , "--jwt.acceptableIssuerRegexes[0]=.*"
            , "--jwt.defaultJwksCacheDuration=PT1M"
    });
  }
}
