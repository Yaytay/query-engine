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
package uk.co.spudsoft.query.main;

import com.google.common.base.Strings;
import java.net.URI;

/**
 * Configuration of distributed tracing.
 * <p>
 * The Query Engine can use either Open Telemetry (using HTTP) or Zipkin.
 * <p>
 * All services participating in a distributed tracing must use the same propagation technique
 * - it is important that {@link #propagator} is configured correctly.
 * 
 * @author jtalbut
 */
public class TracingConfig {
  
  /**
   * The service name to use in distributed traces.
   */
  private String serviceName = "Query Engine";
  
  /**
   * The protocol to use to send distributed tracing data.
   */
  private TracingProtocol protocol = TracingProtocol.none;
  
  /**
   * The sampler to use to decide whether or not a given span should be reported.
   */
  private TracingSampler sampler = TracingSampler.alwaysOn;
  
  /**
   * The propagator to use to encode spans in requests sent/received.
   * All services in a given environment must use the same propagator.
   */
  private TracingPropagator propagator = TracingPropagator.w3c;
  
  /**
   * The sampler to use when sampler is set to {@link TracingSampler#parent} and there is no parent span.
   * If this value is set to {@link TracingSampler#parent} it will be treated as {@link TracingSampler#alwaysOff}.
   * If this value is set to {@link TracingSampler#ratio} the {@link TracingConfig#sampleRatio} will be used as the ratio.
   * If sampler is not set to {@link TracingSampler#parent} this value is ignored.
   */
  private TracingSampler rootSampler = TracingSampler.alwaysOff;
  
  /**
   * The sample ratio to use when sample is set to {@link TracingSampler#ratio}.
   * If sampler is not set to {@link TracingSampler#ratio} this value is ignored.
   * 
   */
  private double sampleRatio = 0.1;
  
  /**
   * The URL to send distributed tracing data to.
   */
  private String url;

  /**
   * Constructor.
   */
  public TracingConfig() {
  }
  
  /**
   * Get the service name to use in zipkin spans.
   * 
   * @return the service name to use in zipkin spans.
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Set the service name to use in zipkin spans.
   * @param serviceName the service name to use in zipkin spans.
   */
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  /**
   * Get the protocol to use to send distributed tracing data.
   * @return the protocol to use to send distributed tracing data.
   */
  public TracingProtocol getProtocol() {
    return protocol;
  }

  /**
   * Set the protocol to use to send distributed tracing data.
   * @param protocol the protocol to use to send distributed tracing data.
   */
  public void setProtocol(TracingProtocol protocol) {
    this.protocol = protocol;
  }

  /**
   * Get the URL to send distributed tracing data to.
   * @return the URL to send distributed tracing data to.
   */
  public String getUrl() {
    return url;
  }

  /**
   * Set the URL to send distributed tracing data to.
   * @param url the URL to send distributed tracing data to.
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Get the sampler to use to decide whether or not a given span should be reported.
   * @return the sampler to use to decide whether or not a given span should be reported.
   */
  public TracingSampler getSampler() {
    return sampler;
  }

  /**
   * Set the sampler to use to decide whether or not a given span should be reported.
   * @param sampler the sampler to use to decide whether or not a given span should be reported.
   */
  public void setSampler(TracingSampler sampler) {
    this.sampler = sampler;
  }

  /**
   * Get the propagator to use to encode spans in requests sent/received.
   * All services in a given environment must use the same propagator.
   * 
   * @return the propagator to use to encode spans in requests sent/received.
   */
  public TracingPropagator getPropagator() {
    return propagator;
  }

  /**
   * Get the propagator to use to encode spans in requests sent/received.
   * All services in a given environment must use the same propagator.
   * 
   * @param propagator the propagator to use to encode spans in requests sent/received.
   */
  public void setPropagator(TracingPropagator propagator) {
    this.propagator = propagator;
  }

  /**
   * Get the sampler to use when sampler is set to {@link TracingSampler#parent} and there is no parent span.
   * If this value is set to {@link TracingSampler#parent} it will be treated as {@link TracingSampler#alwaysOff}.
   * If this value is set to {@link TracingSampler#ratio} the {@link TracingConfig#sampleRatio} will be used as the ratio.
   * If sampler is not set to {@link TracingSampler#parent} this value is ignored.
   * @return the sampler to use when sampler is set to {@link TracingSampler#parent} and there is no parent span.
   */
  public TracingSampler getRootSampler() {
    return rootSampler;
  }

  /**
   * Get the sampler to use when sampler is set to {@link TracingSampler#parent} and there is no parent span.
   * If this value is set to {@link TracingSampler#parent} it will be treated as {@link TracingSampler#alwaysOff}.
   * If this value is set to {@link TracingSampler#ratio} the {@link TracingConfig#sampleRatio} will be used as the ratio.
   * If sampler is not set to {@link TracingSampler#parent} this value is ignored.
   * @param rootSampler the sampler to use when sampler is set to {@link TracingSampler#parent} and there is no parent span.
   */
  public void setRootSampler(TracingSampler rootSampler) {
    this.rootSampler = rootSampler;
  }

  /**
   * Get the sample ratio to use when sample is set to {@link TracingSampler#ratio}.
   * If sampler is not set to {@link TracingSampler#ratio} this value is ignored.
   * @return the sample ratio to use when sample is set to {@link TracingSampler#ratio}.
   */
  public double getSampleRatio() {
    return sampleRatio;
  }

  /**
   * Set the sample ratio to use when sample is set to {@link TracingSampler#ratio}.
   * If sampler is not set to {@link TracingSampler#ratio} this value is ignored.
   * @param sampleRatio the sample ratio to use when sample is set to {@link TracingSampler#ratio}.
   */
  public void setSampleRatio(double sampleRatio) {
    this.sampleRatio = sampleRatio;
  }

  /**
   * Validate the provided parameters.
   * 
   * @param path The name of the parent parameter, to be used in exception messages.
   * @throws IllegalArgumentException if anything in the parameters is invalid.
   */
  public void validate(String path) throws IllegalArgumentException {
    if (protocol != null && Strings.isNullOrEmpty(serviceName)) {
      throw new IllegalArgumentException("Tracing is enabled (" + path + ".protocol != none) and " + path + ".serviceName is not set");
    }
    if (sampler == TracingSampler.ratio) {
      if (sampleRatio < 0.0) {
        throw new IllegalArgumentException("Parameter " + path + ".sampler is set to ratio and the " + path + ".sampleRatio < 0.0");
      } else if (sampleRatio > 1.0) {
        throw new IllegalArgumentException("Parameter " + path + ".sampler is set to ratio and the " + path + ".sampleRatio > 1.0");
      }
    }
    if (sampler == TracingSampler.parent && rootSampler == TracingSampler.ratio) {
      if (sampleRatio < 0.0) {
        throw new IllegalArgumentException("Parameter " + path + ".sampler is set to parent, " + path + ".rootSampler is set to ratio and the " + path + ".sampleRatio < 0.0");
      } else if (sampleRatio > 1.0) {
        throw new IllegalArgumentException("Parameter " + path + ".sampler is set to parent, " + path + ".rootSampler is set to ratio and the " + path + ".sampleRatio > 1.0");
      }
    }
    if (!Strings.isNullOrEmpty(url)) {
      try {      
        new URI(url);
      } catch (Throwable ex) {
        throw new IllegalArgumentException("Parameter " + path + ".url is not a valid URL: " + ex.getMessage());
      }
    }
  }  
  
}
