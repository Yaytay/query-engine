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
package uk.co.spudsoft.query.exec.sources;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.Auditor;
import uk.co.spudsoft.query.exec.SourceInstance;
import uk.co.spudsoft.query.exec.context.PipelineContext;

/**
 * Base class for {@link SourceInstance} implementations.
 *
 * @author jtalbut
 */
public abstract class AbstractSource implements SourceInstance {

  /**
   * The Vert.x instance.
   */
  protected final Vertx vertx;
  
  /**
   * MeterRegistry for production of processor-specific metrics.
   */
  protected final MeterRegistry meterRegistry;
  
  /**
   * The context in which this {@link SourcePipeline} is being run.
   */
  protected final PipelineContext pipelineContext;

  /**
   * The auditor that the source should use for recording details of the data accessed.
   */
  protected final Auditor auditor;

  
  /**
   * Constructor.
   * 
   * @param vertx the Vert.x instance.
   * @param meterRegistry MeterRegistry for production of processor-specific metrics.
   * @param auditor The auditor that the source should use for recording details of the data accessed.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   */
  protected AbstractSource(Vertx vertx, MeterRegistry meterRegistry, Auditor auditor, PipelineContext pipelineContext) {
    this.vertx = vertx;
    this.meterRegistry = meterRegistry;
    this.auditor = auditor;
    this.pipelineContext = pipelineContext;
  }
  
}
