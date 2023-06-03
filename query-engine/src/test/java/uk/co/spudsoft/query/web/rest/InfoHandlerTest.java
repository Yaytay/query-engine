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
package uk.co.spudsoft.query.web.rest;

import io.vertx.core.file.FileSystemException;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.core.Response;
import java.nio.file.NoSuchFileException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import uk.co.spudsoft.query.web.ServiceException;

/**
 *
 * @author jtalbut
 */
public class InfoHandlerTest {
  
  @Test
  public void testReportErrorWithServiceException() {
    String log = "Test: ";
    
    ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
    AsyncResponse response = mock(AsyncResponse.class);
    Throwable ex = new ServiceException(456, "It went wrong");
    InfoHandler.reportError(log, response, ex, true);
    verify(response).resume(responseCaptor.capture());
    assertEquals(456, responseCaptor.getValue().getStatus());
    assertEquals("It went wrong (from ServiceException@uk.co.spudsoft.query.web.rest.InfoHandlerTest:42)", responseCaptor.getValue().getEntity());
  }
  
  @Test
  public void testReportErrorWithIllegalArgumentException() {
    String log = "Test: ";
    
    ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
    AsyncResponse response = mock(AsyncResponse.class);
    Throwable ex = new IllegalArgumentException("It went wrong");
    InfoHandler.reportError(log, response, ex, false);
    verify(response).resume(responseCaptor.capture());
    assertEquals(400, responseCaptor.getValue().getStatus());
    assertEquals("It went wrong", responseCaptor.getValue().getEntity());
  }
  
  @Test
  public void testReportErrorWithOtherException() {
    String log = "Test: ";
    
    ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
    AsyncResponse response = mock(AsyncResponse.class);
    Throwable ex = new NullPointerException("It went wrong");
    InfoHandler.reportError(log, response, ex, false);
    verify(response).resume(responseCaptor.capture());
    assertEquals(500, responseCaptor.getValue().getStatus());
    assertEquals("Unknown error", responseCaptor.getValue().getEntity());
  }
  
  @Test
  public void testReportErrorWithVertxFileNotFoundException() {
    String log = "Test: ";
    
    ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
    AsyncResponse response = mock(AsyncResponse.class);
    Throwable ex = new FileSystemException("It went wrong", new NoSuchFileException("Not found"));
    InfoHandler.reportError(log, response, ex, false);
    verify(response).resume(responseCaptor.capture());
    assertEquals(404, responseCaptor.getValue().getStatus());
    assertEquals("File not found", responseCaptor.getValue().getEntity());
  }
  
  @Test
  public void testReportErrorWithVertxOtherFileException() {
    String log = "Test: ";
    
    ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
    AsyncResponse response = mock(AsyncResponse.class);
    Throwable ex = new FileSystemException("It went wrong", new IllegalArgumentException("Bad argument"));
    InfoHandler.reportError(log, response, ex, false);
    verify(response).resume(responseCaptor.capture());
    assertEquals(500, responseCaptor.getValue().getStatus());
    assertEquals("Unknown error", responseCaptor.getValue().getEntity());
  }
  
}
