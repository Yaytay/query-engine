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
package uk.co.spudsoft.query.web.rest;

import inet.ipaddr.IPAddressString;
import java.io.IOException;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.exec.FilterFactory;
import uk.co.spudsoft.query.exec.conditions.RequestContext;

/**
 *
 * @author njt
 */
public class FormIoHandlerTest {
  
  // This should not actually leak an exception
  @Test
  public void testPipelineStreamExceptions() throws IOException {
    
    RequestContext requestContext = new RequestContext("requestId", "url", "host", "path", null, null, null, new IPAddressString("0.0.0.0"), null);

    FormIoHandler.PipelineStreamer streamer = new FormIoHandler.PipelineStreamer(requestContext, null, 0, new FilterFactory(Collections.emptyList()));
    streamer.write(null);
    
  }
  
}
