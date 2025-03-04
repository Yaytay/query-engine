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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.ProgressNotificationHandler;
import uk.co.spudsoft.query.exec.conditions.RequestContext;

/**
 * Simple logging instance of the {@link ProgressNotificationHandler}.
 * 
 * This will be used for runs that specify a runID with pipelines that do not specify a more useful handler.
 * 
 * @author njt
 */
public class LoggingNotificationHandler implements ProgressNotificationHandler {

  private static final Logger logger = LoggerFactory.getLogger(LoggingNotificationHandler.class);
  
  /**
   * Constructor.
   */
  public LoggingNotificationHandler() {
  }
  
  @Override
  public void event(String runID
          , RequestContext requestContext
          , String pipelineTitle
          , String sourceName
          , String processorName
          , Long count
          , boolean completed
          , Boolean succeeded
          , String message, Object... arguments) {
    
    logger.info(message, arguments);
    
    if (completed) {
      logger.info("Pipeline completed {}", succeeded ? "successfully" : "unsuccessfully");
    }
    
  }

}
