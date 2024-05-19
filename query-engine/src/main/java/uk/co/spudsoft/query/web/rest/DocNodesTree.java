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
package uk.co.spudsoft.query.web.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import uk.co.spudsoft.dircache.AbstractTree;
import uk.co.spudsoft.dircache.AbstractTree.AbstractNode;

/**
 * Specialization of {@link uk.co.spudsoft.dircache.AbstractTree} for reporting the Query Engine documentation.
 * @author jtalbut
 */
public class DocNodesTree extends AbstractTree {

  /**
   * Constructor.
   */
  private DocNodesTree() {
  }
  
  /**
   * Base class for documentation files and the directories that contain them.
   */
  @JsonSubTypes({
    @JsonSubTypes.Type(value = DocNodesTree.DocDir.class),
    @JsonSubTypes.Type(value = DocNodesTree.DocFile.class)
  })
  @Schema(description = """
                        <P>
                        Base class for documentation files and the directories that contain them.
                        </P>
                        """)
  public static class DocNode extends AbstractNode<DocNode> {
    
    private final String path; 

    /**
     * Constructor that will result in a File node.
     * 
     * @param path the relative path to the node.
     */
    public DocNode(String path) {
      super(nameFromPath(path));
      this.path = path;
    }    

    /**
     * Constructor that will result in a Directory node.
     * 
     * @param path  the relative path to the node.
     * @param children children of the this node.
     */
    public DocNode(String path, List<DocNode> children) {
      super(nameFromPath(path), children);
      this.path = path;      
    }

    /**
     * Get the relative path to the node from the root.
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
     * Get the children of the node.
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
    public List<DocNode> getChildren() {
      return super.getChildren();
    }

    /**
     * Get the leaf name of the node.
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

    static String nameFromPath(String path) {
      if (path == null) {
        return null;
      } else {
        int slashPos = path.lastIndexOf("/");
        if (slashPos > 0) {
          path = path.substring(slashPos + 1);
        }
        int dotPos = path.lastIndexOf(".");
        if (dotPos > 0) {
          path = path.substring(0, dotPos);
        }
        return path;
      }
    }

  }
  
  /**
   * A directory containing documentation files.
   */
  @Schema(description = """
                        <P>
                        A directory containing documentation files.
                        </P>
                        """)
  public static class DocDir extends DocNode {
    
    /**
     * Constructor.
     * @param path The relative path to the dir.
     * @param children The children of the dir.
     */
    public DocDir(String path,  List<DocNode> children) {
      super(path, children);
    }

    /**
     * Get the children of the node.
     * This value must be not-null (though it may be empty), because this is a directory.
     * @return the children of the node.
     */
    @Override
    @NotNull
    @Schema(description = """
                          <P>
                          The children of the node.
                          </P>
                          """
            , nullable = false
            , requiredMode = Schema.RequiredMode.REQUIRED
    )
    public List<DocNode> getChildren() {
      return super.getChildren(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }
    
  }
  
  /**
   * A documentation file.
   */
  @Schema(description = """
                        <P>
                        A documentation file.
                        </P>
                        """)
  public static class DocFile extends DocNode {
    
    private final String title;
    
    /**
     * Constructor.
     * @param path The relative path to the document.
     * @param title The title of the document.
     */
    @JsonCreator
    public DocFile(String path, String title) {
      super(path);
      this.title = Objects.requireNonNull(title);
    }

    /**
     * Get the title of the document.
     * The title is what should be displayed in any UI.
     * @return the title of the document.
     */
    @NotNull
    @Schema(description = """
                          <P>
                          The title of the document.
                          </P>
                          <P>
                          The title is what should be displayed in any UI.
                          </P>
                          """
          , maxLength = 10000
    )
    public String getTitle() {
      return title;
    }
    
  }
  
}
