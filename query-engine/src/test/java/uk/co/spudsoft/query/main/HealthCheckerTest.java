/*
 * Copyright (C) 2026 jtalbut
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

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for HealthChecker.
 */
public class HealthCheckerTest {

  private final List<HttpServer> servers = new ArrayList<>();
  private final List<String> requests = new ArrayList<>();

  @AfterEach
  public void stopServers() {
    for (HttpServer server : servers) {
      server.stop(0);
    }
    servers.clear();
  }

  /**
   * Start an HTTP server on the loopback interface that records the requests it receives and
   * responds to all of them with the given status code.
   *
   * @param statusCode the status code to return for every request.
   * @return the ephemeral port that the server is listening on.
   */
  private int startServer(int statusCode) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext("/", exchange -> {
      synchronized (requests) {
        requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
      }
      exchange.sendResponseHeaders(statusCode, -1);
      exchange.close();
    });
    server.start();
    servers.add(server);
    return server.getAddress().getPort();
  }

  /**
   * Find a port that (very probably) has nothing listening on it.
   *
   * @return a port number that was free at the time of the call.
   */
  private int findClosedPort() throws IOException {
    try (ServerSocket socket = openLoopbackSocket()) {
      return socket.getLocalPort();
    }
  }

  /**
   * Open a server socket on an ephemeral port on the same interface that HealthChecker connects to.
   *
   * @return a bound, listening, server socket.
   */
  private ServerSocket openLoopbackSocket() throws IOException {
    ServerSocket socket = new ServerSocket();
    socket.bind(new InetSocketAddress("localhost", 0), 0);
    return socket;
  }

  @Test
  public void testHealthCheckSucceedsWhenServerReturns200() throws IOException {
    int port = startServer(200);

    assertTrue(HealthChecker.healthCheck(port, null));
    assertEquals(List.of("GET /manage/health"), requests);
  }

  @ParameterizedTest
  @ValueSource(ints = {201, 204, 301, 400, 401, 404, 500, 503})
  public void testHealthCheckFailsWhenServerDoesNotReturn200(int statusCode) throws IOException {
    int port = startServer(statusCode);

    assertFalse(HealthChecker.healthCheck(port, null));
    assertEquals(List.of("GET /manage/health"), requests);
  }

  @Test
  public void testHealthCheckFailsWhenPortIsZero() throws IOException {
    // Start a server so that we can prove that nothing was called.
    startServer(200);

    assertFalse(HealthChecker.healthCheck(0, null));
    assertEquals(List.of(), requests);
  }

  @Test
  public void testHealthCheckFailsWhenAlternativePortIsZero() throws IOException {
    int port = startServer(200);

    assertFalse(HealthChecker.healthCheck(port, 0));
    assertEquals(List.of(), requests);
  }

  @Test
  public void testAlternativePortTakesPrecedenceOverPort() throws IOException {
    int failingPort = startServer(503);
    int succeedingPort = startServer(200);

    assertTrue(HealthChecker.healthCheck(failingPort, succeedingPort));
    assertFalse(HealthChecker.healthCheck(succeedingPort, failingPort));
    assertEquals(List.of("GET /manage/health", "GET /manage/health"), requests);
  }

  @Test
  public void testHealthCheckFailsWhenNothingIsListening() throws IOException {
    assertFalse(HealthChecker.healthCheck(findClosedPort(), null));
  }

  @Test
  public void testHealthCheckFailsWhenConnectionIsClosedWithoutResponse() throws IOException {
    try (ServerSocket socket = openLoopbackSocket()) {
      Thread thread = new Thread(() -> {
        try {
          socket.accept().close();
        } catch (IOException ex) {
          // Test is ending, nothing useful to do.
        }
      });
      thread.setDaemon(true);
      thread.start();

      assertFalse(HealthChecker.healthCheck(socket.getLocalPort(), null));
    }
  }
}
