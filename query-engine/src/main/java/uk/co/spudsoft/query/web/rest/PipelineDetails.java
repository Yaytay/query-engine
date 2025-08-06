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
package uk.co.spudsoft.query.web.rest;

import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.defn.ArgumentGroup;
import uk.co.spudsoft.query.defn.Format;
import uk.co.spudsoft.query.exec.context.RequestContext;

/**
 * Representation of a Pipeline for display in a custom UI.
 * 
 * 
 * 
 * @author jtalbut
 */
@Schema(description = """
                      <P>
                      Representation of a Pipeline for display in a custom UI.
                      </P>
                      """)
public class PipelineDetails {

  private final String name;
  private final String path;
  private final String title;
  private final String description;
  private final ImmutableList<ArgumentGroup> argumentGroups;
  private final ImmutableList<ArgumentDetails> arguments;
  private final ImmutableList<FormatDetails> formats;

  /**
   * Constructor.
   * 
   * As an implementation detail it's worth noting that for better JSON output we prefer null values over empty ones here.
   *
   * @param requestContext The {@link RequestContext} used to generate {@link ArgumentDetails}.
   * @param name The name of the file.
   * @param path The full path to the file.
   * @param title The title of the pipeline, extracted from the file.
   * @param description The description of the pipeline, extracted from the file.
   * @param argumentGroups The groups of arguments to the pipeline, extracted from the file.
   * @param arguments The arguments to the pipeline, extracted from the file.
   * @param formats The output formats that the pipeline supports, extracted from the file.
   */
  public PipelineDetails(RequestContext requestContext, String name, String path, String title, String description, List<ArgumentGroup> argumentGroups, List<Argument> arguments, List<Format> formats) {
    this.name = name;
    this.path = path;
    this.title = title;
    this.description = description;
    if (argumentGroups == null || argumentGroups.isEmpty()) {
      this.argumentGroups = null;
    } else {
      this.argumentGroups = ImmutableList.copyOf(argumentGroups);
    }
    if (arguments == null || arguments.isEmpty()) {
      this.arguments = null;
    } else {
      this.arguments = arguments.stream()
              .filter(a -> !a.isHidden())
              .map(a -> new ArgumentDetails(requestContext, a))
              .collect(ImmutableList.toImmutableList())
              ;
    }
    if (formats == null || formats.isEmpty()) {
      this.formats = null;
    } else {
      this.formats = formats.stream()              
              .filter(f -> !f.isHidden())
              .map(f -> new FormatDetails(f))
              .collect(ImmutableList.toImmutableList())
              ;
    }
  }

  /**
   * The name of the pipeline file, to be displayed in the UI if no title is set.
   *
   * @return the name of the pipeline, to be displayed in the UI if necessary.
   */
  @Schema(description = """
                          <P>
                          The name of the pipeline file, to be displayed in the UI if no title is set.
                          </P>
                          """,
           maxLength = 100
  )
  public String getName() {
    return name;
  }

  /**
   * The path to the pipeline.
   * 
   * @return the path to the pipeline.
   */
  @Schema(description = """
                          <P>
                          The path to the pipeline.
                          </P>
                          """,
           maxLength = 1000
  )
  public String getPath() {
    return path;
  }  
  
  /**
   * The title of the pipeline, to be displayed in the UI.
   *
   * @return the title of the pipeline, to be displayed in the UI.
   */
  @Schema(description = """
                          <P>
                          The title of the pipeline, to be displayed in the UI.
                          </P>
                          """,
           maxLength = 100
  )
  public String getTitle() {
    return title;
  }

  /**
   * The description of the pipeline.
   *
   * @return the description of the pipeline.
   */
  @Schema(description = """
                          <P>
                          The description of the pipeline.
                          </P>
                          """,
           maxLength = 10000
  )
  public String getDescription() {
    return description;
  }

  /**
   * Declared argument groups in the Pipeline.
   * <P>
   * The UI for gathering arguments should group them into titles sets that may also have a comment.
   * <P>
   * Arguments with no group will always be presented first.
   *
   * @return declared argument groups in the Pipeline.
   */
  @ArraySchema(
          arraySchema = @Schema(
                  description = """
                            <P>Declared argument groups in the Pipeline.</P>
                            <P>
                            The UI for gathering arguments should group them into titles sets that may also have a comment.
                            </P>
                            <P>
                            Arguments with no group will always be presented first.
                            </P>
                            """
          ),
           schema = @Schema(
                  implementation = ArgumentGroup.class
          ),
           minItems = 0,
           uniqueItems = true
  )
  public List<ArgumentGroup> getArgumentGroups() {
    return argumentGroups;
  }

  /**
   * Declared arguments to the Pipeline.
   * <P>
   * Pipelines can receive arguments via the HTTP query string. Any arguments may be provided and may be processed by the
   * templates of the pipeline, even if they are not declared here. Declare all arguments here, otherwise no-one will know that
   * they exist unless they read the pipeline definition.
   * <P>
   * The order in which Arguments are defined here is relevant as it affects the order in which they will be displayed for
   * Interactive Pipelines. The order in which Arguments are provided in the query string is only relevant if an Argument can take
   * multiple values (in which case they will be presented to the query in the order that they appear in the query string,
   * regardless of any other arguments appearing between them).
   *
   * @return declared arguments to the Pipeline.
   */
  @ArraySchema(
          arraySchema = @Schema(
                  description = """
                            <P>Declared arguments to the Pipeline.</P>
                            <P>
                            Pipelines can receive arguments via the HTTP query string.
                            Any arguments may be provided and may be processed by the templates of the pipeline, even if they are not
                            declared here.
                            Declare all arguments here, otherwise no-one will know that they exist unless they read the pipeline definition.
                            </P>
                            <P>
                            The order in which Arguments are defined here is relevant as it affects the order in which they will be displayed for
                            Interactive Pipelines.
                            The order in which Arguments are provided in the query string is only relevant if an Argument can take multiple values (in which
                            case they will be presented to the query in the order that they appear in the query string, regardless of any other arguments appearing
                            between them).
                            </P>
                            """
          ),
           schema = @Schema(
                  implementation = ArgumentDetails.class
          ),
           minItems = 0,
           uniqueItems = true
  )
  public List<ArgumentDetails> getArguments() {
    return arguments;
  }

  /**
   * The outputs that this Pipeline supports. The format to use for a pipeline is chosen by according to the following rules:
   * <ol>
   * <li><pre>_fmt</pre> query string.<br>
   * If the HTTP request includes a
   * <pre>_fmt</pre> query string argument each Format specified in the Pipeline will be checked (in order) for a matching
   * response from the {@link uk.co.spudsoft.query.defn.Format#getName()} method. The first matching Format will be returned. If
   * no matching Format is found an error will be returned.
   *
   * <li>Path extension.<br>
   * If the path in the HTTP request includes a '.' (U+002E, Unicode FULL STOP) after the last '/' (U+002F, Unicode SOLIDUS)
   * character everything following that character will be considered to be the extension, furthermore the extension (and full
   * stop character) will be removed from the filename being sought. If an extension is found each Format specified in the
   * Pipeline will be checked (in order) for a matching response from the {@link uk.co.spudsoft.query.defn.Format#getExtension()}
   * method. The first matching Format will be returned. If no matching Format is found an error will be returned.
   *
   * <li>Accept header.<br>
   * If the HTTP request includes an 'Accept' header each Format specified in the Pipeline will be checked (in order) for a
   * matching response from the {@link uk.co.spudsoft.query.defn.Format#getMediaType() ()} method. Note that most web browsers
   * include "*\\/*" in their default Accept headers, which will match any Format that specifies a MediaType. The first matching
   * Format will be returned. If no matching Format is found an error will be returned.
   *
   * <li>Default<br>
   * If the request does not use any of these mechanisms then the first Format specified in the Pipeline will be used.
   * </ol>
   *
   * @return the outputs that this Pipeline supports.
   */
  @ArraySchema(
          arraySchema = @Schema(
                  description = """
                                    <P>The outputs that this Pipeline supports.</P>
                                    <P>
                                    The format to use for a pipeline is chosen by according to the following rules:
                                    <ol>

                                    <li><pre>_fmt</pre> query string.<br>
                                    If the HTTP request includes a <pre>_fmt</pre> query string argument each Format specified in the Pipeline will be checked (in order)
                                    for a matching response from the {@link uk.co.spudsoft.query.defn.Format#getName()} method.
                                    The first matching Format will be returned.
                                    If no matching Format is found an error will be returned.

                                    <li>Path extension.<br>
                                    If the path in the HTTP request includes a '.' (U+002E, Unicode FULL STOP) after the last '/' (U+002F, Unicode SOLIDUS) character everything following that
                                    character will be considered to be the extension, furthermore the extension (and full stop character) will be removed from the filename being sought.
                                    If an extension is found each Format specified in the Pipeline will be checked (in order)
                                    for a matching response from the {@link uk.co.spudsoft.query.defn.Format#getExtension()} method.
                                    The first matching Format will be returned.
                                    If no matching Format is found an error will be returned.

                                    <li>Accept header.<br>
                                    If the HTTP request includes an 'Accept' header each Format specified in the Pipeline will be checked (in order)
                                    for a matching response from the {@link uk.co.spudsoft.query.defn.Format#getMediaType() ()} method.
                                    Note that most web browsers include "*\\/*" in their default Accept headers, which will match any Format that specifies a MediaType.
                                    The first matching Format will be returned.
                                    If no matching Format is found an error will be returned.

                                    <li>Default<br>
                                    If the request does not use any of these mechanisms then the first Format specified in the Pipeline will be used.
                                    </ol>
                                    <p>
                                    """
          ),
           schema = @Schema(
                  implementation = FormatDetails.class
          ),
           minItems = 1,
           uniqueItems = true
  )
  public List<FormatDetails> getFormats() {
    return formats;
  }

}
