/*
 * Copyright (C) 2024 njt
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
package uk.co.spudsoft.query.testcontainers;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import uk.co.spudsoft.query.main.TracingProtocol;

/**
 *
 * @author njt
 */
public class ServerProviderDistributedTracing {
  
  private static final Logger logger = LoggerFactory.getLogger(ServerProviderDistributedTracing.class);
  
  private String url;
  private TracingProtocol protocol;
  
  public String getUrl() {
    return url;
  }
  
  public TracingProtocol getProtocol() {
    return protocol;
  }
  
  private int findPort(ContainerPort[] ports, final int target) {
    for (ContainerPort port : ports) {
      if (target == port.getPrivatePort()) {
        return port.getPublicPort();
      }
    }
    return 0;
  }
  
  public ServerProviderDistributedTracing init() {
    Container container;
    container = AbstractServerProvider.findContainer("/query-engine-jaeger-1");
    if (container != null) {
      protocol = TracingProtocol.otlphttp;
      url = "http://localhost:" + findPort(container.ports, 4318) + "/v1/traces";
      return this;
    } 
    container = AbstractServerProvider.findContainer("/query-engine-zipkin-1");
    if (container != null) {
      protocol = TracingProtocol.zipkin;
      url = "http://localhost:" + findPort(container.ports, 9411) + "/api/v2/spans";
      return this;
    }
    GenericContainer<?> jc = new GenericContainer<>("jaegertracing/all-in-one:latest");
    jc.addEnv("COLLECTOR_OTLP_ENABLED", "true");
    jc.addExposedPorts(
     5778, 
     4317, 
     4318, 
     14268,
     16686
    );
    jc.start();
    protocol = TracingProtocol.otlphttp;
    url = "http://localhost:" + jc.getMappedPort(4318) + "/v1/traces";
    logger.debug("Jaeger URI: http://localhost:" + jc.getMappedPort(16686) + "/");
    return this;
  }
  
}
