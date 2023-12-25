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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import uk.co.spudsoft.dircache.AbstractTree;
import uk.co.spudsoft.dircache.AbstractTree.AbstractNode;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;
import uk.co.spudsoft.query.defn.Format;

/**
 *
 * @author jtalbut
 */
public class PipelineNodesTree extends AbstractTree {

  private PipelineNodesTree() {
  }

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

    private final String path;

    public PipelineNode(String path) {
      super(nameFromPath(path));
      this.path = undot(path);
    }

    public PipelineNode(String path, List<PipelineNode> children) {
      super(nameFromPath(path), children);
      this.path = undot(path);
    }

    @NotNull
    @Schema(description = """
                          <P>
                          The relative path to the node from the root.
                          </P>
                          """)
    public String getPath() {
      return path;
    }

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

    @Override
    @Schema(description = """
                          <P>
                          The leaf name of the node.
                          </P>
                          """)
    public String getName() {
      return super.getName();
    }

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

    static String undot(String path) {
      int dotPos = path.indexOf(".");
      if (dotPos > 0) {
        path = path.substring(0, dotPos);
      }
      return path;
    }
  }

  @Schema(description = """
                        <P>
                        A directory containing pipelines.
                        </P>
                        """)
  public static class PipelineDir extends PipelineNode {

    public PipelineDir(String path, List<PipelineNode> children) {
      super(path, children);
    }

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

    public PipelineFile(String path, String title, String description, List<Argument> arguments, List<Format> destinations) {
      super(path);
      this.title = title;
      this.description = description;
      this.arguments = ImmutableCollectionTools.copy(arguments);
      this.destinations = ImmutableCollectionTools.copy(destinations);
    }

    @Schema(description = """
                          <P>
                          The title of the pipeline, to be displayed in the UI.
                          </P>
                          """)
    public String getTitle() {
      return title;
    }

    @Schema(description = """
                          <P>
                          The description of the pipeline.
                          </P>
                          """)
    public String getDescription() {
      return description;
    }

    @Schema(description = """
                          <P>
                          The arguments that the pipeline accepts/requires.
                          </P>
                          """)
    public ImmutableList<Argument> getArguments() {
      return arguments;
    }

    @Schema(description = """
                          <P>
                          The outputs that the pipeline supports.
                          </P>
                          """)
    public ImmutableList<Format> getDestinations() {
      return destinations;
    }

  }

}
