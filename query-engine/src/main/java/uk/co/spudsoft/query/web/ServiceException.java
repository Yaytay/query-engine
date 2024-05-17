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
 * Exception thrown within a web service request that encapsulates the status code to be return to HTTP clients.
 * 
 * The message for this Exception (though not for the cause of this Exception) should be prepared for use as the body of the HTTP response.
 * 
 * @author jtalbut
 */
public class ServiceException extends Exception {

  private static final long serialVersionUID = 1123543245L;
  
  private final int statusCode;

  /**
   * Constructor.
   * @param statusCode HTTP status code to use on the response to the client.
   * @param message Message to use on the response to the client.
   * @param cause Cause of this ServiceException - this will only be included in the response to the client if outputAllErrorMessages is set.
   */
  public ServiceException(int statusCode, String message, Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
  }

  /**
   * Constructor.
   * @param statusCode HTTP status code to use on the response to the client.
   * @param message Message to use on the response to the client.
   */
  public ServiceException(int statusCode, String message) {
    super(message);
    this.statusCode = statusCode;
  }

  /**
   * Get the HTTP status code.
   * @return the HTTP status code.
   */
  public int getStatusCode() {
    return statusCode;
  }  
  
  /**
   * Return a ServiceException, either the one passed in or a newly created on with that set as the cause.
   * @param ex An exception, that might already be a ServiceException.
   * @return A ServiceException.
   */
  public static ServiceException rethrowOrWrap(Throwable ex) {
    if (ex instanceof ServiceException se) {
      return se;
    } else {
      return new ServiceException(500, "Failed to execute query", ex);
    }
    
  }
}
