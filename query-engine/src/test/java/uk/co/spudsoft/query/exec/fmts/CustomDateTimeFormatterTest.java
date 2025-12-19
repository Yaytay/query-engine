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
package uk.co.spudsoft.query.exec.fmts;

import inet.ipaddr.IPAddressString;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.context.RequestContext;

/**
 *
 * @author jtalbut
 */
public class CustomDateTimeFormatterTest {
  
  private static final Logger logger = LoggerFactory.getLogger(CustomDateTimeFormatterTest.class);
  
  @Test
  public void testFormat() {
    
    RequestContext requestContext = new RequestContext(null, "requestId", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.0"), null);
    PipelineContext pipelineContext = new PipelineContext("test", requestContext);
    
    LocalDateTime dateTime = LocalDateTime.of(2023, 5, 15, 13, 45, 30, 120000000);
    
    logger.debug("{}", new CustomDateTimeFormatter("EPOCH_SECONDS").format(pipelineContext, dateTime));
    logger.debug("{}", new CustomDateTimeFormatter("EPOCH_MILLISECONDS").format(pipelineContext, dateTime));
    logger.debug("{}", new CustomDateTimeFormatter("BASIC_ISO_DATE").format(pipelineContext, dateTime));
    logger.debug("{}", new CustomDateTimeFormatter("ISO_LOCAL_DATE").format(pipelineContext, dateTime));
    logger.debug("{}", new CustomDateTimeFormatter("ISO_DATE").format(pipelineContext, dateTime));
    logger.debug("{}", new CustomDateTimeFormatter("ISO_LOCAL_TIME").format(pipelineContext, dateTime));
    logger.debug("{}", new CustomDateTimeFormatter("ISO_TIME").format(pipelineContext, dateTime));
    logger.debug("{}", new CustomDateTimeFormatter("ISO_LOCAL_DATE_TIME").format(pipelineContext, dateTime));
    logger.debug("{}", new CustomDateTimeFormatter("ISO_ORDINAL_DATE").format(pipelineContext, dateTime));
    logger.debug("{}", new CustomDateTimeFormatter("ISO_WEEK_DATE").format(pipelineContext, dateTime));

    logger.debug("{}", new CustomDateTimeFormatter("ISO_OFFSET_DATE").format(pipelineContext, dateTime));
    logger.debug("{}", new CustomDateTimeFormatter("ISO_OFFSET_TIME").format(pipelineContext, dateTime));
    logger.debug("{}", new CustomDateTimeFormatter("ISO_OFFSET_DATE_TIME").format(pipelineContext, dateTime));
    logger.debug("{}", new CustomDateTimeFormatter("ISO_ZONED_DATE_TIME").format(pipelineContext, dateTime));
    logger.debug("{}", new CustomDateTimeFormatter("ISO_DATE_TIME").format(pipelineContext, dateTime));
    logger.debug("{}", new CustomDateTimeFormatter("ISO_INSTANT").format(pipelineContext, dateTime));
    logger.debug("{}", new CustomDateTimeFormatter("RFC_1123_DATE_TIME").format(pipelineContext, dateTime));

    logger.debug("{}", new CustomDateTimeFormatter("VV zzz O X x Z").format(pipelineContext, dateTime));

    assertEquals(1684158330L, new CustomDateTimeFormatter("EPOCH_SECONDS").format(pipelineContext, dateTime));
    assertEquals(1684158330120L, new CustomDateTimeFormatter("EPOCH_MILLISECONDS").format(pipelineContext, dateTime));
    assertEquals("2023-05-15T13:45:30.120", new CustomDateTimeFormatter("DEFAULT").format(pipelineContext, dateTime));
    assertEquals("20230515", new CustomDateTimeFormatter("BASIC_ISO_DATE").format(pipelineContext, dateTime));
    assertEquals("2023-05-15", new CustomDateTimeFormatter("ISO_LOCAL_DATE").format(pipelineContext, dateTime));
    assertEquals("2023-05-15", new CustomDateTimeFormatter("ISO_DATE").format(pipelineContext, dateTime));
    assertEquals("13:45:30.12", new CustomDateTimeFormatter("ISO_LOCAL_TIME").format(pipelineContext, dateTime));
    assertEquals("13:45:30.12", new CustomDateTimeFormatter("ISO_TIME").format(pipelineContext, dateTime));
    assertEquals("2023-05-15T13:45:30.12", new CustomDateTimeFormatter("ISO_LOCAL_DATE_TIME").format(pipelineContext, dateTime));
    assertEquals("2023-135", new CustomDateTimeFormatter("ISO_ORDINAL_DATE").format(pipelineContext, dateTime));
    assertEquals("2023-W20-1", new CustomDateTimeFormatter("ISO_WEEK_DATE").format(pipelineContext, dateTime));
    
    assertEquals("2023-05-15Z", new CustomDateTimeFormatter("ISO_OFFSET_DATE").format(pipelineContext, dateTime));
    assertEquals("13:45:30.12Z", new CustomDateTimeFormatter("ISO_OFFSET_TIME").format(pipelineContext, dateTime));
    assertEquals("2023-05-15T13:45:30.12Z", new CustomDateTimeFormatter("ISO_OFFSET_DATE_TIME").format(pipelineContext, dateTime));
    assertEquals("2023-05-15T13:45:30.12Z", new CustomDateTimeFormatter("ISO_ZONED_DATE_TIME").format(pipelineContext, dateTime));
    assertEquals("2023-05-15T13:45:30.12Z", new CustomDateTimeFormatter("ISO_DATE_TIME").format(pipelineContext, dateTime));
    assertEquals("2023-05-15T13:45:30.120Z", new CustomDateTimeFormatter("ISO_INSTANT").format(pipelineContext, dateTime));
    assertEquals("Mon, 15 May 2023 13:45:30 GMT", new CustomDateTimeFormatter("RFC_1123_DATE_TIME").format(pipelineContext, dateTime));

    assertEquals("Z Z GMT Z +00 +0000", new CustomDateTimeFormatter("VV zzz O X x Z").format(pipelineContext, dateTime));
  }
  
  @Test
  public void testStrippedSeconds() {
    
    RequestContext requestContext = new RequestContext(null, "requestId", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.0"), null);
    PipelineContext pipelineContext = new PipelineContext("test", requestContext);
    
    assertEquals("2023-05-15T13:45:30.12", new CustomDateTimeFormatter("ISO_LOCAL_DATE_TIME_TRIM").format(pipelineContext, LocalDateTime.of(2023, 5, 15, 13, 45, 30, 120000000)));
    assertEquals("2023-05-15T13:45:30", new CustomDateTimeFormatter("ISO_LOCAL_DATE_TIME_TRIM").format(pipelineContext, LocalDateTime.of(2023, 5, 15, 13, 45, 30)));
    assertEquals("2023-05-15T13:45", new CustomDateTimeFormatter("ISO_LOCAL_DATE_TIME_TRIM").format(pipelineContext, LocalDateTime.of(2023, 5, 15, 13, 45)));
    assertEquals("2023-05-15T13:45", new CustomDateTimeFormatter("ISO_LOCAL_DATE_TIME_TRIM").format(pipelineContext, LocalDateTime.of(2023, 5, 15, 13, 45, 00, 0000000)));

    assertEquals("2023-05-15T13:45:30.120", new CustomDateTimeFormatter("DEFAULT").format(pipelineContext, LocalDateTime.of(2023, 5, 15, 13, 45, 30, 120000000)));
    assertEquals("2023-05-15T13:45:30", new CustomDateTimeFormatter("DEFAULT").format(pipelineContext, LocalDateTime.of(2023, 5, 15, 13, 45, 30)));
    assertEquals("2023-05-15T13:45", new CustomDateTimeFormatter("DEFAULT").format(pipelineContext, LocalDateTime.of(2023, 5, 15, 13, 45)));
    assertEquals("2023-05-15T13:45", new CustomDateTimeFormatter("DEFAULT").format(pipelineContext, LocalDateTime.of(2023, 5, 15, 13, 45, 00, 0000000)));

    assertEquals("2023-05-15T13:45:30.120", new CustomDateTimeFormatter("").format(pipelineContext, LocalDateTime.of(2023, 5, 15, 13, 45, 30, 120000000)));
    assertEquals("2023-05-15T13:45:30", new CustomDateTimeFormatter("").format(pipelineContext, LocalDateTime.of(2023, 5, 15, 13, 45, 30)));
    assertEquals("2023-05-15T13:45", new CustomDateTimeFormatter("").format(pipelineContext, LocalDateTime.of(2023, 5, 15, 13, 45)));
    assertEquals("2023-05-15T13:45", new CustomDateTimeFormatter("").format(pipelineContext, LocalDateTime.of(2023, 5, 15, 13, 45, 00, 0000000)));
  }
  
}
