package uk.co.spudsoft.query.main;

import io.vertx.core.Future;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Static class for performing health checks via HTTP request.
 */
public final class HealthChecker {

  private HealthChecker() {
  }

  private static HttpClient httpClient() {
    return HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .followRedirects(HttpClient.Redirect.NEVER)
      .build();
  }

  /**
   * Perform an HTTP request to localhost on either port or, if specified, the alternative port.
   * The path will always be "/manage/health".
   *
   * @param port The standard part of the port to use.
   * @param alternativePort The alternative port to use if specified.
   * @return True if the health check was successful, false otherwise.
   */
  public static boolean healthCheck(int port, Integer alternativePort) {
    if (alternativePort != null) {
      port = alternativePort;
    }
    if (port == 0) {
      return false;
    }

    try (HttpClient client = httpClient()) {
      HttpRequest request = HttpRequest.newBuilder()
        .uri(java.net.URI.create("http://localhost:" + port + "/manage/health"))
        .timeout(Duration.ofSeconds(10))
        .GET()
        .build();

      java.net.http.HttpResponse<Void> response =
        client.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() != 200) {
        return false;
      }
    } catch (Throwable ex) {
      return false;
    }
    return true;
  }
}
