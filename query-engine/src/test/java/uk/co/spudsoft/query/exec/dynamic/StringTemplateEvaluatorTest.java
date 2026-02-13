/*
 * Copyright (C) 2026 njt
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
package uk.co.spudsoft.query.exec.dynamic;

import inet.ipaddr.IPAddressString;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.context.RequestContext;

import static org.junit.jupiter.api.Assertions.*;

public class StringTemplateEvaluatorTest {

  private static PipelineContext pipelineContext(String host) {
    RequestContext reqctx = new RequestContext(
            null,
            "requestId",
            "http://example.invalid",
            host,
            "/path",
            null,
            null,
            null,
            new IPAddressString("127.0.0.1"),
            null
    );
    return new PipelineContext("pipe", reqctx);
  }

  @Test
  public void testNullTemplateReturnsNullWithoutTouchingContext() {
    // Should short-circuit before accessing pipelineContext
    assertNull(StringTemplateEvaluator.renderTemplate("any", null, null, null));
  }

  @Test
  public void testEmptyTemplateReturnsEmptyStringWithoutTouchingContext() {
    // Should short-circuit before accessing pipelineContext
    assertEquals("", StringTemplateEvaluator.renderTemplate("any", "", null, null));
  }

  @Test
  public void testWhitespaceOnlyTemplateIsNotConsideredEmptyAndIsRendered() {
    PipelineContext ctx = pipelineContext("hostA");

    // A template that is non-empty but has no ST expressions should render verbatim
    assertEquals("   ", StringTemplateEvaluator.renderTemplate("t", "   ", ctx, null));
  }

  @Test
  public void testRendersLiteralTemplateVerbatim() {
    PipelineContext ctx = pipelineContext("hostA");

    assertEquals("hello world", StringTemplateEvaluator.renderTemplate("t", "hello world", ctx, null));
  }

  @Test
  public void testRendersUsingRequestContextFromPipelineContext() {
    PipelineContext ctx = pipelineContext("my-host");

    // StringTemplate attribute reference; RequestContext has getHost()
    assertEquals("my-host", StringTemplateEvaluator.renderTemplate("t", "<request.host>", ctx, null));
    assertEquals("/path", StringTemplateEvaluator.renderTemplate("t", "<request.path>", ctx, null));
  }

  @Test
  public void testRendersUsingExtraContextAttributes() {
    PipelineContext ctx = pipelineContext("hostA");

    Map<String, Object> extra = new HashMap<>();
    extra.put("who", "Bob");
    extra.put("n", 42);

    assertEquals("Bob", StringTemplateEvaluator.renderTemplate("t", "<who>", ctx, extra));
    assertEquals("42", StringTemplateEvaluator.renderTemplate("t", "<n>", ctx, extra));
    assertEquals("Hello Bob #42", StringTemplateEvaluator.renderTemplate("t", "Hello <who> #<n>", ctx, extra));
  }

  @Test
  public void testExtraContextMayBeNull() {
    PipelineContext ctx = pipelineContext("hostA");

    assertEquals("hostA", StringTemplateEvaluator.renderTemplate("t", "<request.host>", ctx, null));
  }

  @Test
  public void testThrowsIllegalStateExceptionOnTemplateParseOrRenderFailure() {
    PipelineContext ctx = pipelineContext("hostA");

    // Deliberately malformed template (unclosed '<...') to trigger an exception inside ST
    String badTemplate = "<request.host";

    IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> StringTemplateEvaluator.renderTemplate("myName", badTemplate, ctx, null)
    );

    assertTrue(ex.getMessage().contains("Error(s) evaluating myName template:"), ex.getMessage());
    assertNotNull(ex.getCause(), "Expected original cause to be preserved");
  }

  @Test
  public void testFailurePathStillThrowsEvenIfExtraContextCannotBeJsonEncoded() {
    PipelineContext ctx = pipelineContext("hostA");

    // Create a self-referential structure that is likely to break JSON encoding
    // (Vert.x Json.encode typically uses Jackson and may fail on cycles).
    Map<String, Object> extra = new HashMap<>();
    extra.put("self", extra);

    // Force the method into the catch block with a malformed template
    String badTemplate = "<if("; // malformed control structure

    IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> StringTemplateEvaluator.renderTemplate("cyclic", badTemplate, ctx, extra)
    );

    assertTrue(ex.getMessage().contains("Error(s) evaluating cyclic template:"), ex.getMessage());
    assertNotNull(ex.getCause(), "Expected original cause to be preserved");
  }
}