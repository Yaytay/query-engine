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
package uk.co.spudsoft.query.exec.procs;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Vertx;
import uk.co.spudsoft.query.defn.SourcePipeline;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.context.PipelineContext;

/**
 * Base class for {@link ProcessorInstance} implementations.
 * @author jtalbut
 */
public abstract class AbstractProcessor implements ProcessorInstance {

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
   * The name of the processor.
   */
  protected final String name;
  

  /**
   * Constructor.
   * 
   * The name passed in will be used as-is and should be either the name configured in the definition file or
   * a constructed name based on the JSON path to the processor in the definition file.
   * 
   * In both cases the name should already have any parent processor name prepended to it.
   * In this way, if processors are not explicitly named, the name used should be a full JsonPath to it.
   * 
   * @param vertx the Vert.x instance.
   * @param meterRegistry MeterRegistry for production of processor-specific metrics.
   * @param pipelineContext The context in which this {@link SourcePipeline} is being run.
   * @param name The name of the processor.
   */
  protected AbstractProcessor(Vertx vertx, MeterRegistry meterRegistry, PipelineContext pipelineContext, String name) {
    this.vertx = vertx;
    this.meterRegistry = meterRegistry;
    this.pipelineContext = pipelineContext;
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }
  
}
