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
package uk.co.spudsoft.query.web;

import io.vertx.core.MultiMap;
import io.vertx.core.net.HostAndPort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class OriginalUrlTest {

  // -----------------------
  // get(String, HostAndPort, ...) validation tests
  // -----------------------
  @Test
  void get_throwsWhenSchemeMissing() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()
            -> OriginalUrl.get(null, HostAndPort.create("example.com", 80), "/p", "q=1", headers));
    assertEquals("Scheme from current request must be specified", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, ()
            -> OriginalUrl.get("", HostAndPort.create("example.com", 80), "/p", "q=1", headers));
    assertEquals("Scheme from current request must be specified", ex.getMessage());
  }

  @Test
  void get_throwsWhenHostAndPortMissing() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()
            -> OriginalUrl.get("http", null, "/p", "q=1", headers));
    assertEquals("Host and port from current request must be specified", ex.getMessage());
  }

  // -----------------------
  // Protocol selection tests (X-Forwarded-Proto > X-Forwarded-Scheme > requestScheme)
  // -----------------------
  @Test
  void get_usesXForwardedProtoWhenPresent() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap().add("X-Forwarded-Proto", "https");
    String url = OriginalUrl.get("http", HostAndPort.create("example.com", 443), "/path", "a=1", headers);
    assertEquals("https://example.com/path?a=1", url); // default 443 omitted for https
  }

  @Test
  void get_usesXForwardedSchemeWhenProtoMissing() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap().add("X-Forwarded-Scheme", "https");
    String url = OriginalUrl.get("http", HostAndPort.create("example.com", 444), "/path", "a=1", headers);
    assertEquals("https://example.com:444/path?a=1", url); // non-standard https port included
  }

  @Test
  void get_fallsBackToRequestSchemeWhenNoForwardedHeaders() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    String url = OriginalUrl.get("http", HostAndPort.create("example.com", 80), "/p", "q=1", headers);
    assertEquals("http://example.com/p?q=1", url); // default 80 omitted
  }

  // -----------------------
  // Host/Port resolution tests
  // -----------------------
  @Test
  void get_usesXForwardedHostWithoutPort() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap().add("X-Forwarded-Host", "forwarded.example.com");
    String url = OriginalUrl.get("http", HostAndPort.create("ignored.example.com", 80), "/p", "q=1", headers);
    assertEquals("http://forwarded.example.com/p?q=1", url);
  }

  @Test
  void get_usesXForwardedHostWithPort() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap().add("X-Forwarded-Host", "forwarded.example.com:8080");
    String url = OriginalUrl.get("http", HostAndPort.create("ignored.example.com", 80), "/p", "q=1", headers);
    assertEquals("http://forwarded.example.com:8080/p?q=1", url);
  }

  @Test
  void get_usesXForwardedPortOverridesEverything() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap()
            .add("X-Forwarded-Host", "forwarded.example.com:1234")
            .add("X-Forwarded-Port", "9090");
    String url = OriginalUrl.get("http", HostAndPort.create("ignored", 80), "/p", "q=1", headers);
    assertEquals("http://forwarded.example.com:9090/p?q=1", url);
  }

  @Test
  void get_invalidXForwardedPortFallsBack() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap()
            .add("X-Forwarded-Port", "not-a-number");
    String url = OriginalUrl.get("http", HostAndPort.create("example.com", 81), "/p", "q=1", headers);
    // invalid forwarded port ignored, use request port (81)
    assertEquals("http://example.com:81/p?q=1", url);
  }

  @Test
  void get_invalidPortInXForwardedHostFallsBackToRequestPort() {
    // Invalid port after colon: will be caught, host set, port remains unset then falls back to request port
    MultiMap headers = MultiMap.caseInsensitiveMultiMap()
            .add("X-Forwarded-Host", "forwarded.example.com:not-a-number");
    String url = OriginalUrl.get("http", HostAndPort.create("ignored.com", 88), "/p", "q=1", headers);
    assertEquals("http://forwarded.example.com:88/p?q=1", url);
  }

  @Test
  void get_noForwardedHeaders_usesRequestHostAndPort() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    String url = OriginalUrl.get("https", HostAndPort.create("example.com", 8443), "/p", "q=1", headers);
    assertEquals("https://example.com:8443/p?q=1", url);
  }

  // -----------------------
  // Default port suppression tests
  // -----------------------
  @Test
  void get_httpsDefaultPortOmitted() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap().add("X-Forwarded-Proto", "https");
    String url = OriginalUrl.get("http", HostAndPort.create("example.com", 443), "/p", "q=1", headers);
    assertEquals("https://example.com/p?q=1", url);
  }

  @Test
  void get_httpDefaultPortOmitted() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    String url = OriginalUrl.get("http", HostAndPort.create("example.com", 80), "/p", "q=1", headers);
    assertEquals("http://example.com/p?q=1", url);
  }

  @Test
  void get_nonDefaultPortIncluded() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    String url = OriginalUrl.get("http", HostAndPort.create("example.com", 81), "/p", "q=1", headers);
    assertEquals("http://example.com:81/p?q=1", url);
  }

  // -----------------------
  // Path handling tests
  // -----------------------
  @Test
  void get_addsLeadingSlashIfMissing() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    String url = OriginalUrl.get("http", HostAndPort.create("example.com", 80), "noSlash", "q=1", headers);
    assertEquals("http://example.com/noSlash?q=1", url);
  }

  @Test
  void get_preservesLeadingSlashIfPresent() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    String url = OriginalUrl.get("http", HostAndPort.create("example.com", 80), "/already", "q=1", headers);
    assertEquals("http://example.com/already?q=1", url);
  }

  @Test
  void get_omitsPathIfNullOrEmpty() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    String url1 = OriginalUrl.get("http", HostAndPort.create("example.com", 80), null, "q=1", headers);
    String url2 = OriginalUrl.get("http", HostAndPort.create("example.com", 80), "", "q=1", headers);
    assertEquals("http://example.com?q=1", url1);
    assertEquals("http://example.com?q=1", url2);
  }

  // -----------------------
  // Query handling tests
  // -----------------------
  @Test
  void get_addsQuestionMarkIfMissing() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    String url = OriginalUrl.get("http", HostAndPort.create("example.com", 80), "/p", "a=1&b=2", headers);
    assertEquals("http://example.com/p?a=1&b=2", url);
  }

  @Test
  void get_preservesQuestionMarkIfPresent() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    String url = OriginalUrl.get("http", HostAndPort.create("example.com", 80), "/p", "?a=1&b=2", headers);
    assertEquals("http://example.com/p?a=1&b=2", url);
  }

  @Test
  void get_omitsQueryIfNullOrEmpty() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    String url1 = OriginalUrl.get("http", HostAndPort.create("example.com", 80), "/p", null, headers);
    String url2 = OriginalUrl.get("http", HostAndPort.create("example.com", 80), "/p", "", headers);
    assertEquals("http://example.com/p", url1);
    assertEquals("http://example.com/p", url2);
  }

  // -----------------------
  // Combined precedence scenarios
  // -----------------------
  @Test
  void get_forwardedSchemeAndHostAndPort_allApplied() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap()
            .add("X-Forwarded-Proto", "https")
            .add("X-Forwarded-Host", "fw.example.com:444")
            .add("X-Forwarded-Port", "445");
    String url = OriginalUrl.get("http", HostAndPort.create("orig.example.com", 8080), "/x", "y=1", headers);
    // Proto=https, host from XFH, port from XFP
    assertEquals("https://fw.example.com:445/x?y=1", url);
  }

  @Test
  void get_forwardedHostOnly_withRequestSchemeAndPort() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap()
            .add("X-Forwarded-Host", "fw.example.com");
    String url = OriginalUrl.get("https", HostAndPort.create("orig", 443), "/a/b", "c=d", headers);
    // https default 443 omitted
    assertEquals("https://fw.example.com/a/b?c=d", url);
  }

  @Test
  void get_forwardedSchemeButNoForwardedHost_usesOriginalHost() {
    MultiMap headers = MultiMap.caseInsensitiveMultiMap()
            .add("X-Forwarded-Scheme", "https");
    String url = OriginalUrl.get("http", HostAndPort.create("original.com", 1234), "/p", "q=1", headers);
    assertEquals("https://original.com:1234/p?q=1", url);
  }
}
