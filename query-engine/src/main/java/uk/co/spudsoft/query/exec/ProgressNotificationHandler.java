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
package uk.co.spudsoft.query.exec;

import uk.co.spudsoft.query.exec.conditions.RequestContext;

/**
 * Handler for receiving messages during the progress of a pipeline.
 * The Handler can do whatever if wants with the messages.
 * @author njt
 */
public interface ProgressNotificationHandler {
  
  /**
   * Handle an event.
   * 
   * @param runID The ID provided by the caller for this run.
   * If the runID is null or blank only the NullNotificationHandler will be called.
   * @param requestContext The request context for the pipeline.
   * @param pipeline The pipeline instance.
   * @param source The source that this message relates to - may be null.
   * @param processor The processor that this message relates to - may be null.
   * @param message The message, which may contain slf4j formatting instructions.
   * @param arguments Arguments to the message.
   */
  void event(String runID
          , RequestContext requestContext
          , PipelineInstance pipeline
          , SourceInstance source
          , ProcessorInstance processor
          , String message
          , Object... arguments
  );
  
}
