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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import uk.co.spudsoft.dircache.AbstractTree;
import uk.co.spudsoft.dircache.AbstractTree.AbstractNode;

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
                        Base class for pipelines and the directories that contain them.
                        """
          , oneOf = {
                  PipelineDir.class
                  , PipelineFile.class
          }
  )
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
    @ArraySchema(
            arraySchema = @Schema(
                    description = """
                                  The children of the node.
                                  <P>
                                  If this is null then the node is a file, otherwise it is a directory.
                                  </P>
                                  """
                    , requiredMode = Schema.RequiredMode.NOT_REQUIRED
                    , nullable = true
            )
    )
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
    @ArraySchema(
            arraySchema = @Schema(
                    description = """
                                  The children of the node.
                                  <P>
                                  If this is null then the node is a file, otherwise it is a directory.
                                  </P>
                                  """
                    , requiredMode = Schema.RequiredMode.REQUIRED
                    , nullable = false
            )
    )
    public List<PipelineNode> getChildren() {
      return super.getChildren(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

  }

  /**
   * A single pipeline file that contains enough information from the {@link uk.co.spudsoft.query.defn.Pipeline} definition to build an input form.
   */
  @Schema(description = """
                        <P>
                        A pipeline.
                        </P>
                        """)
  public static class PipelineFile extends PipelineNode {

    private final String title;
    private final String description;

    /**
     * Constructor.
     * 
     * @param path The full path to the file.
     * @param title The title of the pipeline, extracted from the file.
     * @param description The description of the pipeline, extracted from the file.
     */
    public PipelineFile(String path, String title, String description) {
      super(path);
      this.title = title;
      this.description = description;
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

  }

}
