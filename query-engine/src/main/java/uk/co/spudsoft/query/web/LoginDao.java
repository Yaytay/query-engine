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

import io.vertx.core.Future;
import java.time.LocalDateTime;

/**
 *
 * @author jtalbut
 */
public interface LoginDao {

  void prepare() throws Exception;
  
  Future<Void> store(String state, String provider, String codeVerifier, String nonce, String redirectUri, String targetUrl);
  
  Future<Void> markUsed(String state);
  
  record RequestData(String provider, String codeVerifier, String nonce, String redirectUri, String targetUrl) {};
  
  Future<RequestData> getRequestData(String state);
 
  Future<Void> storeToken(String id, LocalDateTime expiry, String token);

  Future<String> getToken(String id);

  Future<Void> removeToken(String id);

}
