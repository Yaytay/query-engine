/*
 * Copyright (C) 2025 jtalbut
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

/**
 * Represents the types of OAuth 2.0 grant types that utilize basic authentication credentials.
 */
public enum BasicAuthGrantType {

  /**
   * Basic Auth credentials will be used to make a <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-4.4">client credentials grant</a> request  to an IdP.
   */
  clientCredentials
  ,
  /**
   * Basic Auth credentials will be used to make a <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-4.3">resource owner password credentials grant</a> request  to an IdP.
   */
  resourceOwnerPasswordCredentials
}
