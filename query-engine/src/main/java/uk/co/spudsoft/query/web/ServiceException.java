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
package uk.co.spudsoft.query.web;

/**
 *
 * @author jtalbut
 */
public class ServiceException extends Exception {

  private static final long serialVersionUID = 1123543245L;
  
  private final int statusCode;

  public ServiceException(int statusCode, String message, Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
  }

  public ServiceException(int statusCode, String message) {
    super(message);
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return statusCode;
  }  
  
  public static ServiceException rethrowOrWrap(Throwable ex) {
    if (ex instanceof ServiceException se) {
      return se;
    } else {
      return new ServiceException(500, "Failed to execute query", ex);
    }
    
  }
}
