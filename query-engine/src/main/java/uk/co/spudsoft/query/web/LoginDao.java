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
import uk.co.spudsoft.query.exec.context.RequestContext;

/**
 * Interface for defining the data storage of login/session data.
 * @author jtalbut
 */
public interface LoginDao {

  /**
   * Set up the store so that other methods are ready to be called.
   * <p>
   * This method must be called before any others.
   * @throws Exception if something goes wrong.
   */
  void prepare() throws Exception;
  
  /**
   * Store information regarding an OAuth request into the database.
   * @param state The OAuth state value passed to the OAuth provider.
   * This value is used as the key for accessing requests in future.
   * @param requestContext the context of the request.
   * @param provider The OAuth provider.
   * @param codeVerifier The OAuth PKCE code verifier calculated from the code passed to the OAuth provider.
   * @param nonce The OAuth nonce value passed to the OAuth provider.
   * @param redirectUri The OAuth redirect URI passed to the OAuth provider.
   * @param targetUrl The URL that the user should be directed to after login has completed.
   * @return A Future that will be completed when the request has been recorded.
   */
  Future<Void> store(RequestContext requestContext, String state, String provider, String codeVerifier, String nonce, String redirectUri, String targetUrl);
  
  /**
   * Mark a request as having been used.
   * <p>
   * A request can only be used once.
   * @param requestContext the context of the request.
   * @param state The OAuth state value that was passed to the OAuth provider.  The key to the request.
   * @return A Future that will be completed when the request has been marked as used.
   */
  Future<Void> markUsed(RequestContext requestContext, String state);
  
  /**
   * A record of the data that was used in making an OAuth request.
   * @param provider The OAuth provider.
   * @param codeVerifier The OAuth PKCE code verifier calculated from the code passed to the OAuth provider.
   * @param nonce The OAuth nonce value passed to the OAuth provider.
   * @param redirectUri The OAuth redirect URI passed to the OAuth provider.
   * @param targetUrl The URL that the user should be directed to after login has completed.
   */
  record RequestData(String provider, String codeVerifier, String nonce, String redirectUri, String targetUrl) {};
  
  /**
   * Get details of the OAuth request.
   * @param requestContext the context of the request.
   * @param state The OAuth state value that was passed to the OAuth provider.  The key to the request.
   * @return A Future that will be completed with details of the OAuth request.
   */
  Future<RequestData> getRequestData(RequestContext requestContext, String state);
 
  /**
   * Store a token so that it may be recalled in future.
   * @param requestContext the context of the request.
   * @param id The ID to use for the token - this is the value of the session cookie.
   * @param expiry The date/time at which the token expires.
   * @param token The token to be stored (a JWT).
   * @param provider The OAuth provider.
   * @param refreshToken The OAuth refresh token from the authentication endpoint.
   * @param idToken The OIDC ID Token from the authentication endpoint.
   * @return A Future that will be completed when the token has been stored.
   */
  Future<Void> storeTokens(RequestContext requestContext, String id, LocalDateTime expiry, String token, String provider, String refreshToken, String idToken);

  /**
   * Get a token by its ID.
   * <p>
   * If the token has expired it will not be returned.
   * @param requestContext the context of the request.
   * @param id The ID of the token - this is the value of the session cookie.
   * @return A Future that will be completed with the token.
   */
  Future<String> getToken(RequestContext requestContext, String id);
  
  /**
   * A Record containing the provider and the idToken for use when logging out.
   * 
   * @param provider The name of the provider, for locating its configuration.
   * @param accessToken The access token provided when the user logged in, may be null.
   * @param refreshToken The refresh token provided when the user logged in, may be null.
   * @param idToken The id token provided when the user logged in, may be null.
   * @param sessionId The session ID corresponding to this provider/token.
   */
  record ProviderAndTokens(
          String provider
          , String accessToken
          , String refreshToken
          , String idToken
          , String sessionId
          ) {};
  
  /**
   * Get the name of the provider and the ID token to use for logging out.
   * 
   * There should never be multiple session IDs for a given session.
   * If this does happen, please ensure that the path for the session cookie is configured correctly.
   * 
   * @param requestContext the context of the request.
   * @param sessionId The ID of the token - this is the values of the session cookies.
   * @return A Future containing a List of Records containing the provider and the idToken for use when logging out.
   */
  Future<ProviderAndTokens> getProviderAndTokens(RequestContext requestContext, String sessionId);  

  /**
   * Remove a token from the data store.
   * @param requestContext the context of the request.
   * @param sessionId The ID of the token - this is the value of the session cookie.
   * @return A Future that will be completed when the token has been removed.
   */
  Future<Void> removeToken(RequestContext requestContext, String sessionId);

}
