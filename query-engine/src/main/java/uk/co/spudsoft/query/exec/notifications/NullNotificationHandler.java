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
package uk.co.spudsoft.query.exec.notifications;

import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.ProcessorInstance;
import uk.co.spudsoft.query.exec.ProgressNotificationHandler;
import uk.co.spudsoft.query.exec.SourceInstance;
import uk.co.spudsoft.query.exec.conditions.RequestContext;

/**
 * Null instance of the {@link ProgressNotificationHandler}.
 * This instance does nothing.
 * @author njt
 */
public class NullNotificationHandler implements ProgressNotificationHandler {

  /**
   * Constructor.
   */
  public NullNotificationHandler() {
  }

  @Override
  public void event(String runID, RequestContext requestContext, PipelineInstance pipeline, SourceInstance source, ProcessorInstance processor, String message, Object... arguments) {
  }
  
}
