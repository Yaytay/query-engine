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

import io.vertx.core.Promise;
import static io.vertx.core.Promise.promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.co.spudsoft.query.json.ObjectMapperConfiguration;


/**
 *
 * @author njt
 */
@ExtendWith(VertxExtension.class)
public class LoginDaoMemoryImplTest {
  
  @BeforeAll
  public static void configureObjectMapper() {
    ObjectMapperConfiguration.configureObjectMapper(DatabindCodec.mapper());
  }
  
  @Test
  public void testPrepare() throws Exception {
    LoginDao dao = new LoginDaoMemoryImpl(Duration.of(1, ChronoUnit.MINUTES));
    dao.prepare();
    assertThrows(IllegalStateException.class, () -> {
      dao.prepare();
    });
  }
  
  @Test
  public void testStoreThenGetRequestDataThenMarkUsedSuccessful(Vertx vertx, VertxTestContext testContext) {
    
    LoginDao dao = new LoginDaoMemoryImpl(Duration.of(100, ChronoUnit.MINUTES));

    dao.store("state", "provider", "codeVerifier", "nonce", "redirectUri", "targetUrl")
            .compose(v -> {
              return dao.getRequestData("state");
            })
            .compose(rd -> {
              testContext.verify(() -> {
                assertEquals("nonce", rd.nonce());
                assertEquals("provider", rd.provider());
                assertEquals("redirectUri", rd.redirectUri());
                assertEquals("targetUrl", rd.targetUrl());
              });
              return dao.markUsed("state");
            })
            .onSuccess(v -> {
              testContext.completeNow();
            })
            .onFailure(ex -> {
              testContext.failNow(ex);
            });
  }
  
  @Test
  public void testStoreThenGetRequestDataThenExpire(Vertx vertx, VertxTestContext testContext) {
    
    LoginDao dao = new LoginDaoMemoryImpl(Duration.of(100, ChronoUnit.MILLIS));

    dao.store("state", "provider", "codeVerifier", "nonce", "redirectUri", "targetUrl")
            .compose(v -> {
              return dao.getRequestData("state");
            })
            .compose(rd -> {
              testContext.verify(() -> {
                assertEquals("nonce", rd.nonce());
                assertEquals("provider", rd.provider());
                assertEquals("redirectUri", rd.redirectUri());
                assertEquals("targetUrl", rd.targetUrl());
              });
              Promise<Void> delay = Promise.promise();
              vertx.setTimer(100, id -> {
                delay.complete();
              });
              return delay.future();
            })
            .compose(v -> {
              return dao.store("state2", "provider", "codeVerifier", "nonce", "redirectUri", "targetUrl");
            })
            .compose(v -> {
              return dao.markUsed("state");
            })
            .onSuccess(v -> {
              testContext.failNow("Expected exception");
            })
            .onFailure(ex -> {
              testContext.completeNow();
            });
  }
  
  @Test
  public void testStoreThenGetRequestDataThenMarkUsedExpired(Vertx vertx, VertxTestContext testContext) {
    
    LoginDao dao = new LoginDaoMemoryImpl(Duration.of(100, ChronoUnit.MILLIS));

    dao.store("state", "provider", "codeVerifier", "nonce", "redirectUri", "targetUrl")
            .compose(v -> {
              return dao.getRequestData("state");
            })
            .compose(rd -> {
              testContext.verify(() -> {
                assertEquals("nonce", rd.nonce());
                assertEquals("provider", rd.provider());
                assertEquals("redirectUri", rd.redirectUri());
                assertEquals("targetUrl", rd.targetUrl());
              });
              Promise<Void> delay = Promise.promise();
              vertx.setTimer(100, id -> {
                delay.complete();
              });
              return delay.future();
            })
            .compose(v -> {
              return dao.markUsed("state");
            })
            .onSuccess(v -> {
              testContext.failNow("Expected exception");
            })
            .onFailure(ex -> {
              testContext.completeNow();
            });
  }
  
  @Test
  public void testStoreThenGetRequestDataUnsuccessful(Vertx vertx, VertxTestContext testContext) {
    
    LoginDao dao = new LoginDaoMemoryImpl(Duration.of(1, ChronoUnit.MINUTES));

    dao.store("state", "provider", "codeVerifier", "nonce", "redirectUri", "targetUrl")
            .compose(v -> {
              return dao.getRequestData("bob");
            })
            .onSuccess(rd -> {
              testContext.failNow("Should have failed");
            })
            .onFailure(ex -> {
              testContext.completeNow();
            });
    
  }
}
