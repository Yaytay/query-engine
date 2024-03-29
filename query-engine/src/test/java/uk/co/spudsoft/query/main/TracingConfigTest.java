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

import static org.junit.Assert.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class TracingConfigTest {
  
  @Test
  public void testGetServiceName() {
    TracingConfig instance = new TracingConfig();
    assertEquals("Query Engine", instance.getServiceName());
    instance.setServiceName("bob");
    assertEquals("bob", instance.getServiceName());
  }

  @Test
  public void testGetUrl() {
    TracingConfig instance = new TracingConfig();
    assertNull(instance.getUrl());
    instance.setUrl("bob");
    assertEquals("bob", instance.getUrl());
  }
  
  @Test
  public void testGetPropagator() {
    TracingConfig instance = new TracingConfig();
    assertEquals(TracingPropagator.w3c, instance.getPropagator());
    instance.setPropagator(TracingPropagator.b3multi);
    assertEquals(TracingPropagator.b3multi, instance.getPropagator());
  }
  
  @Test
  public void testGetProtocol() {
    TracingConfig instance = new TracingConfig();
    assertEquals(TracingProtocol.none, instance.getProtocol());
    instance.setProtocol(TracingProtocol.otlpgrpc);
    assertEquals(TracingProtocol.otlpgrpc, instance.getProtocol());
  }
  
  @Test
  public void testGetSampler() {
    TracingConfig instance = new TracingConfig();
    assertEquals(TracingSampler.alwaysOn, instance.getSampler());
    instance.setSampler(TracingSampler.ratio);
    assertEquals(TracingSampler.ratio, instance.getSampler());
  }
  
  @Test
  public void testGetSampleRatio() {
    TracingConfig instance = new TracingConfig();
    assertEquals(0.1, instance.getSampleRatio(), 0.001);
    instance.setSampleRatio(0.123);
    assertEquals(0.123, instance.getSampleRatio(), 0.001);
  }

}
