/*
 * Copyright (C) 2025 njt
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
package uk.co.spudsoft.query.main.sample;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.testcontainers.ServerProviderPostgreSQL;

/**
 *
 * @author njt
 */
@ExtendWith(VertxExtension.class)
public class AbstractSampleDataLoaderIT {
  
  private ServerProviderPostgreSQL provider = new ServerProviderPostgreSQL().init();

  @Test
  public void testPrepareTestDatabase(Vertx vertx, VertxTestContext testContext) {
    
    Future<Void> future1 = prep(vertx, provider);
    Future<Void> future2 = prep(vertx, provider);
    Future<Void> future3 = prep(vertx, provider);
    
    Future.all(future1, future2, future3)
            .andThen(testContext.succeedingThenComplete());
    
  }
  
  private Future<Void> prep(Vertx vertx, ServerProviderPostgreSQL provider) {
    return provider
            .prepareTestDatabase(vertx)
            .compose(v -> {
              return provider.prepareTestDatabase(vertx);
            });
  }
  
  @Test
  public void testScriptNotFound(Vertx vertx, VertxTestContext testContext) {
    TestSampleDataLoaderScriptDoesNotExist dataLoader = new TestSampleDataLoaderScriptDoesNotExist();
    Future<Void> future = dataLoader.prepareTestDatabase(vertx, provider.getVertxUrl(), provider.getUser(), provider.getPassword());
    future.andThen(ar -> {
      testContext.verify(() -> {
        assertTrue(ar.failed());
        assertEquals("java.io.FileNotFoundException: SQL script not found: ThisFileDoesNotExist", ar.cause().getMessage());
      });      
      testContext.completeNow();
    });
  }
  
  @Test
  public void testGetScriptThrows(Vertx vertx, VertxTestContext testContext) {
    TestSampleDataLoaderScriptThrowsException dataLoader = new TestSampleDataLoaderScriptThrowsException();
    Future<Void> future = dataLoader.prepareTestDatabase(vertx, provider.getVertxUrl(), provider.getUser(), provider.getPassword());
    future.andThen(ar -> {
      testContext.verify(() -> {
        assertTrue(ar.failed());
        assertEquals("java.lang.IllegalStateException: You asked me to throw an exception", ar.cause().getMessage());
      });      
      testContext.completeNow();
    });
  }
  
  @Test
  public void testBadScript(Vertx vertx, VertxTestContext testContext) {
    TestSampleDataLoaderBadScript dataLoader = new TestSampleDataLoaderBadScript();
    Future<Void> future = dataLoader.prepareTestDatabase(vertx, provider.getVertxUrl(), provider.getUser(), provider.getPassword());
    future.andThen(ar -> {
      testContext.verify(() -> {
        assertTrue(ar.failed());
        assertEquals("ERROR: syntax error at or near \"This\" (42601)", ar.cause().getMessage());
      });      
      testContext.completeNow();
    });
  }
  
  private static class TestSampleDataLoaderScriptDoesNotExist extends AbstractSampleDataLoader {

    public TestSampleDataLoaderScriptDoesNotExist() {
      super("target" + File.separator + "temp");
    }

    @Override
    protected String getScript() {
      return "ThisFileDoesNotExist";
    }

    @Override
    public String getName() {
      return this.getClass().getSimpleName();
    }

    @Override
    public String getIdentifierQuote() {
      return "'";
    }

    @Override
    protected Path getCompletionFilePath(String lockKey) {
      return null;
    }
  }
  
  private static class TestSampleDataLoaderScriptThrowsException extends TestSampleDataLoaderScriptDoesNotExist {

    @Override
    protected String getScript() {
      throw new IllegalStateException("You asked me to throw an exception");
    }
  }
  
  private static class TestSampleDataLoaderBadScript extends TestSampleDataLoaderScriptDoesNotExist {

    @Override
    protected String getScript() {
      return "/bad-init-script.sql";
    }
  }

  
}
