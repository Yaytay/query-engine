/*
 * Copyright (C) 2024 jtalbut
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
 * The protocol used for reporting traces to a collector.
 * 
 * Depending on the tracing service being used different protocols may be used by different services.
 * All services must use a protocol supported by the tracing service.
 * 
 * @author jtalbut
 */
public enum TracingProtocol {

  /**
   * Disable tracing.
   */
  none
  , 
  /**
   * Use the Zipkin protocol.
   * See <a href="https://github.com/openzipkin/zipkin-api/blob/master/zipkin.proto">https://github.com/openzipkin/zipkin-api/blob/master/zipkin.proto</a>
   */
  zipkin
  , 
  /**
   * Use to OTLP protocol over HTTP.
   * See <a href="https://github.com/open-telemetry/oteps/blob/main/text/0035-opentelemetry-protocol.md">https://github.com/open-telemetry/oteps/blob/main/text/0035-opentelemetry-protocol.md</a>.
   */
  otlphttp
  , 
  /**
   * Use to OTLP protocol over GRPC.
   * See <a href="https://github.com/open-telemetry/oteps/blob/main/text/0035-opentelemetry-protocol.md">https://github.com/open-telemetry/oteps/blob/main/text/0035-opentelemetry-protocol.md</a>.
   */
  otlpgrpc
  
}
