/*
 * Copyright (C) 2026 jtalbut
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

import com.google.common.base.Strings;
import io.vertx.core.json.Json;
import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import uk.co.spudsoft.query.exec.StringTemplateListener;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.logging.Log;

/**
 * Helper class for evaluating {@link ST StringTemplate} instances.
 *
 * Unlike the JexlEvaluator this class is implemented in a single static method.
 *
 * @author jtalbut
 */
public class StringTemplateEvaluator {

  private static final Logger logger = LoggerFactory.getLogger(StringTemplateEvaluator.class);

  private static TopLevelJexlFunctions fns = new TopLevelJexlFunctions();

  private StringTemplateEvaluator() {
  }

  /**
   * Functional interface used by StringTemplate for top level functions.
   */
  public interface Fn {
    /**
     * Call the function.
     * @param args Arguments to the function.
     * @return The return value from the function.
     */
      Object call(Object... args);
  }

  /**
   * Render a {@link ST StringTemplate}.
   * @param name the name of the template, used for error reporting.
   * @param template the {@link ST StringTemplate}.
   * @param pipelineContext the context in which the pipeline is being run.
   * @param extraContext additional data items that will be added to the evaluation context.
   * @return the output from the {@link ST StringTemplate}.
   */
  // Compare the values added with the ProcessorScriptInstance#runSource and ConditionInstance#evaluate
  public static String renderTemplate(
          String name
          , String template
          , PipelineContext pipelineContext
          , Map<String, Object> extraContext
  ) throws IllegalStateException {
    if (Strings.isNullOrEmpty(template)) {
      return template;
    }

    StringTemplateListener errorListener = new StringTemplateListener();
    try {
      /*
       * IMPORTANT:
       * - `template` is treated as a complete ST *template body*, not an STGroup definition.
       * - `name` is only used for diagnostics (error reporting), not as a template identifier.
       *
       * This allows callers to pass templates like:
       *   ConditionalArgument-<now.call("yyyy-MM-dd_HHmm")>
       *
       * If callers need a literal '<' or '>' in output, they must escape it per ST rules.
       */
      STGroup stgroup = new STGroup();
      stgroup.setListener(errorListener);
      stgroup.registerRenderer(LocalDateTime.class, new StringTemplateLocalDateTimeRenderer());

      ST st = new ST(stgroup, template);

      // Provide "now" function object; invoke as: <now.call("pattern")>
      st.add("obj", new TopLevelStringTemplateObjects());

      // Common context
      st.add("request", pipelineContext.getRequestContext());

      // Extra context
      if (extraContext != null) {
        extraContext.forEach(st::add);
      }

      return st.render();
    } catch (Throwable ex) {
      if (extraContext != null) {
        try {
          String contextAsJson = Json.encode(extraContext);
          Log.decorate(logger.atWarn(), pipelineContext)
                  .log("Failed to render template {} with values {}: ", template, contextAsJson, ex);
        } catch (Throwable ex2) {
          Log.decorate(logger.atWarn(), pipelineContext)
                  .log("Failed to render template and cannot represent context as json {}: ", template, ex);
        }
      } else {
        Log.decorate(logger.atWarn(), pipelineContext)
                .log("Failed to render template {}: ", template, ex);
      }

      throw new IllegalStateException(
              "Error(s) evaluating " + name + " template: " + Json.encode(errorListener.getErrors()),
              ex
      );
    } finally {
      if (!errorListener.getErrors().isEmpty()) {
        Log.decorate(logger.atWarn(), pipelineContext).log("Errors: ", Json.encode(errorListener.getErrors()));
      }
    }
  }

}
