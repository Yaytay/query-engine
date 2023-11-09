/*
 * Copyright (C) 2023 njt
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

import io.vertx.core.Future;
import io.vertx.core.json.Json;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author njt
 */
public class LoginDaoMemoryImpl implements LoginDao {
  
  private static final Logger logger = LoggerFactory.getLogger(LoginDaoMemoryImpl.class);

  private static class Data {
    private final LocalDateTime timestamp;
    private LocalDateTime completed;
    private RequestData requestData;

    public Data(RequestData requestData) {      
      this.timestamp = LocalDateTime.now();
      this.requestData = requestData;
    }

    public LocalDateTime getCompleted() {
      return completed;
    }

    public void setCompleted(LocalDateTime completed) {
      this.completed = completed;
    }

    public LocalDateTime getTimestamp() {
      return timestamp;
    }

    public RequestData getRequestData() {
      return requestData;
    }
    
  }
  
  private final Map<String, Data> data = new HashMap<>();
  
  @Override
  public Future<Void> store(String state, String provider, String codeVerifier, String nonce, String redirectUri, String targetUrl) {

    synchronized(data) {
      data.put(state, new Data(new RequestData(provider, codeVerifier, nonce, redirectUri, targetUrl)));
      
      // Purge history
      int previousCount = data.size();
      LocalDateTime limit = LocalDateTime.now().minus(6, ChronoUnit.HOURS);
      data.entrySet().removeIf(e -> e.getValue().timestamp.isBefore(limit));
      if (data.size() < previousCount) {
        logger.debug("Size of login request store reduced from {} to {}", previousCount, data.size());
      }
      if (logger.isDebugEnabled()) {
        logger.debug("All auth states: {}", Json.encode(data));
      }
    }
    return Future.succeededFuture();
  }

  @Override
  public Future<RequestData> getRequestData(String state) {
    synchronized(data) {
      Data input = data.get(state);
      if (input == null) {
        logger.debug("State {} not found in {}", state, Json.encode(data.keySet()));
        return Future.failedFuture(new SecurityException("State not known"));
      }
      return Future.succeededFuture(input.getRequestData());
    }    
  }

}
