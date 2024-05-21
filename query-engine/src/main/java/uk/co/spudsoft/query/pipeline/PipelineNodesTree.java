/*
 * Copyright (C) 2022 jtalbut
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
package uk.co.spudsoft.query.pipeline;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import uk.co.spudsoft.dircache.AbstractTree;
import uk.co.spudsoft.dircache.AbstractTree.AbstractNode;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;
import uk.co.spudsoft.query.defn.Format;

/**
 * Tree of {@link uk.co.spudsoft.dircache.AbstractTree} specialized for pipelines.
 * <p>
 * This is for returning a tree of directories and pipeline definitions to the UI.
 * 
 * @author jtalbut
 */
public class PipelineNodesTree extends AbstractTree {

  private PipelineNodesTree() {
  }

  /**
   * Base class for pipelines and the directories that contain them.
   */
  @JsonSubTypes({
    @JsonSubTypes.Type(value = PipelineDir.class),
    @JsonSubTypes.Type(value = PipelineFile.class)
  })
  @Schema(description = """
                        <P>
                        Base class for pipelines and the directories that contain them.
                        </P>
                        """)
  public static class PipelineNode extends AbstractNode<PipelineNode> {

    /**
     * The path to the file, with no extension.
     */
    private final String path;

    /**
     * Constructor.
     * @param path full path to the file/directory.
     */
    public PipelineNode(String path) {
      super(nameFromPath(path));
      this.path = undot(path);
    }

    /**
     * Constructor for a directory.
     * @param path full path to the directory.
     * @param children child directories/files.
     */
    public PipelineNode(String path, List<PipelineNode> children) {
      super(nameFromPath(path), children);
      this.path = undot(path);
    }

    /**
     * The relative path to the node from the root.
     * @return the relative path to the node from the root.
     */
    @NotNull
    @Schema(description = """
                          <P>
                          The relative path to the node from the root.
                          </P>
                          """
            , maxLength = 1000
    )
    public String getPath() {
      return path;
    }

    /**
     * The children of the node.
     * <p>
     * If this is null then the node is a file, otherwise it is a directory.
     * @return the children of the node.
     */
    @Override
    @Schema(description = """
                          <P>
                          The children of the node.
                          </P>
                          <P>
                          If this is null then the node is a file, otherwise it is a directory.
                          </P>
                          """)
    public List<PipelineNode> getChildren() {
      return super.getChildren();
    }

    /**
     * The leaf name of the node.
     * @return the leaf name of the node.
     */
    @Override
    @Schema(description = """
                          <P>
                          The leaf name of the node.
                          </P>
                          """
            , maxLength = 100
    )
    public String getName() {
      return super.getName();
    }

    /**
     * Return the name of the last component of the path without any extension.
     * <p>
     * i.e. the string between the last slash and the first dot after that.
     * @param path The path to be extracted.
     * @return the name of the last component of the path without any extension.
     */
    static String nameFromPath(String path) {
      if (path == null) {
        return null;
      } else {
        int slashPos = path.lastIndexOf("/");
        if (slashPos > 0) {
          path = path.substring(slashPos + 1);
        }
        int dotPos = path.indexOf(".");
        if (dotPos > 0) {
          path = path.substring(0, dotPos);
        }
        return path;
      }
    }

    /**
     * Remove any extension from the path.
     * @param path The full path to the file.
     * @return the path without any extension. 
     */
    static String undot(String path) {
      int dotPos = path.indexOf(".");
      if (dotPos > 0) {
        path = path.substring(0, dotPos);
      }
      return path;
    }
  }

  /**
   * A directory containing files or other directories.
   */
  @Schema(description = """
                        <P>
                        A directory containing pipelines.
                        </P>
                        """)
  public static class PipelineDir extends PipelineNode {

    /**
     * Constructor for a directory.
     * @param path full path to the directory.
     * @param children child directories/files.
     */
    public PipelineDir(String path, List<PipelineNode> children) {
      super(path, children);
    }

    /**
     * Get the children of the directory.
     * @return the children of the directory.
     */
    @Override
    @NotNull
    @Schema(nullable = false
            , requiredMode = Schema.RequiredMode.REQUIRED
            , description = """
                          <P>
                          The children of the directory.
                          </P>
                          """)
    public List<PipelineNode> getChildren() {
      return super.getChildren(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

  }

  /**
   * A single file.
   */
  @Schema(description = """
                        <P>
                        A pipeline.
                        </P>
                        """)
  public static class PipelineFile extends PipelineNode {

    private final String title;
    private final String description;
    private final ImmutableList<Argument> arguments;
    private final ImmutableList<Format> destinations;

    /**
     * Constructor.
     * 
     * @param path The full path to the file.
     * @param title The title of the pipeline, extracted from the file.
     * @param description The description of the pipeline, extracted from the file.
     * @param arguments The arguments to the pipeline, extracted from the file.
     * @param destinations The output formats that the pipeline supports, extracted from the file.
     */
    public PipelineFile(String path, String title, String description, List<Argument> arguments, List<Format> destinations) {
      super(path);
      this.title = title;
      this.description = description;
      this.arguments = ImmutableCollectionTools.copy(arguments);
      this.destinations = ImmutableCollectionTools.copy(destinations);
    }

    /**
     * The title of the pipeline, to be displayed in the UI.
     * @return the title of the pipeline, to be displayed in the UI.
     */
    @Schema(description = """
                          <P>
                          The title of the pipeline, to be displayed in the UI.
                          </P>
                          """
            , maxLength = 100
    )
    public String getTitle() {
      return title;
    }

    /**
     * The description of the pipeline.
     * @return the description of the pipeline.
     */
    @Schema(description = """
                          <P>
                          The description of the pipeline.
                          </P>
                          """
            , maxLength = 10000
    )
    public String getDescription() {
      return description;
    }

    /**
     * Declared arguments to the Pipeline.
     * <P>
     * Pipelines can receive arguments via the HTTP query string.
     * Any arguments may be provided and may be processed by the templates of the pipeline, even if they are not
     * declared here.
     * Declare all arguments here, otherwise no-one will know that they exist unless they read the pipeline definition.
     * <P>
     * The order in which Arguments are defined here is relevant as it affects the order in which they will be displayed for
     * Interactive Pipelines.
     * The order in which Arguments are provided in the query string is only relevant if an Argument can take multiple values (in which
     * case they will be presented to the query in the order that they appear in the query string, regardless of any other arguments appearing
     * between them).
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
            )
            , schema = @Schema(
                    implementation = Argument.class
            )
            , minItems = 0
            , uniqueItems = true
    )
    public List<Argument> getArguments() {
      return arguments;
    }

    /**
     * The outputs that this Pipeline supports.
     * The format to use for a pipeline is chosen by according to the following rules:
     * <ol>
     * <li><pre>_fmt</pre> query string.<br>
     * If the HTTP request includes a <pre>_fmt</pre> query string argument each Format specified in the Pipeline will be checked (in order)
     * for a matching response from the {@link uk.co.spudsoft.query.defn.Format#getName()} method.
     * The first matching Format will be returned.
     * If no matching Format is found an error will be returned.
     * 
     * <li>Path extension.<br>
     * If the path in the HTTP request includes a '.' (U+002E, Unicode FULL STOP) after the last '/' (U+002F, Unicode SOLIDUS) character everything following that
     * character will be considered to be the extension, furthermore the extension (and full stop character) will be removed from the filename being sought.
     * If an extension is found each Format specified in the Pipeline will be checked (in order)
     * for a matching response from the {@link uk.co.spudsoft.query.defn.Format#getExtension()} method.
     * The first matching Format will be returned.
     * If no matching Format is found an error will be returned.
     * 
     * <li>Accept header.<br>
     * If the HTTP request includes an 'Accept' header each Format specified in the Pipeline will be checked (in order)
     * for a matching response from the {@link uk.co.spudsoft.query.defn.Format#getMediaType() ()} method.
     * Note that most web browsers include "*\\/*" in their default Accept headers, which will match any Format that specifies a MediaType.
     * The first matching Format will be returned.
     * If no matching Format is found an error will be returned.
     * 
     * <li>Default<br>
     * If the request does not use any of these mechanisms then the first Format specified in the Pipeline will be used.
     * </ol>
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
            )
            , schema = @Schema(
                    implementation = Format.class
            )
            , minItems = 1
            , uniqueItems = true
    )
    public List<Format> getDestinations() {
      return destinations;
    }

  }

}
