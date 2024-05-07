/*
 * Copyright (C) 2023 jtalbut
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
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import uk.co.spudsoft.dircache.AbstractTree;
import uk.co.spudsoft.dircache.DirCacheTree;

/**
 *
 * @author jtalbut
 */
public class DesignNodesTree extends AbstractTree {

  private DesignNodesTree() {
  }
  
  @JsonSubTypes({
    @JsonSubTypes.Type(value = DesignNodesTree.DesignDir.class),
    @JsonSubTypes.Type(value = DesignNodesTree.DesignFile.class)
  })
  @Schema(description = """
                        <P>
                        Base class for pipeline design files and the directories that contain them.
                        </P>
                        """)
  public abstract static class DesignNode extends AbstractNode<DesignNode> {

    private final String path;
    private final LocalDateTime modified;

    /**
     * File constructor.
     * 
     * @param path The path represented by this Node.
     * @param modified The modified timestamp.
     * @param name The leaf name of the node.
     */
    protected DesignNode(@NotNull String path, @NotNull LocalDateTime modified, @NotNull String name) {
      super(name);
      this.path = path;
      this.modified = modified;
    }

    /**
     * Directory constructor.
     * 
     * @param path The path represented by this Node.
     * @param modified The modified timestamp.
     * @param name The leaf name of the node.
     * @param children The nodes in this directory.
     */
    protected DesignNode(@NotNull String path, @NotNull LocalDateTime modified, @NotNull String name, List<DesignNode> children) {
      super(name, children);
      this.path = path;
      this.modified = modified;
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
    public List<DesignNode> getChildren() {
      return super.getChildren();
    }

    /**
     * Get the leaf name of the node.
     * @return the leaf name of the node.
     */
    @Override
    @NotNull
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
     * Get the modified timestamp.
     * @return the modified timestamp.
     */
    @NotNull 
    @Schema(description = """
                          <P>
                          The time that the design file was last modified, as reported by the underlying filesystem.
                          </P>
                          <P>
                          Design files must be stored on a filesystem that supports a last-modified timestamp.
                          </P>
                          """
            , maxLength = 40
            , requiredMode = Schema.RequiredMode.REQUIRED
    )
    public LocalDateTime getModified() {
      return modified;
    }

  }

  @Schema(description = """
                        <P>
                        A directory containing pipeline files.
                        </P>
                        """)
  public static class DesignDir extends DesignNode {

    private static List<DesignNode> buildChildren(java.nio.file.Path root, DirCacheTree.Directory src) {
      List<DesignNode> children = new ArrayList<>();

      for (DirCacheTree.Node child : src.getChildren()) {
        if (child instanceof DirCacheTree.Directory d) {
          children.add(new DesignDir(root, d, d.getName()));
        } else if (child instanceof DirCacheTree.File f) {
          children.add(new DesignFile(root, f));
        }
      }
      
      return children;
    }
    
    /**
     * Constructor.
     * @param path The path represented by this Node.
     * @param modified The modified timestamp.
     * @param name The name of the directory in the parent directory.
     * @param children The children of the Directory - in the order returned by FileWalker (which will be dirs first, then probably sorted by name).
     */
    @JsonCreator
    public DesignDir(String path, LocalDateTime modified, String name, List<DesignNode> children) {
      super(path, modified, name, List.copyOf(children));
    }
    
    public DesignDir(java.nio.file.Path root, DirCacheTree.Directory src, String name) {
      super(relativize(File.separator, root, src), src.getModified(), name, buildChildren(root, src));
    }
    
    /**
     * Get the children of the Directory.
     * @return the children of the Directory.
     */
    @Override
    @NotNull
    @Schema(nullable = false
            , requiredMode = Schema.RequiredMode.REQUIRED
            , description = """
                          <P>
                          The children of the node.
                          </P>
                          <P>
                          If this is null then the node is a file, otherwise it is a directory.
                          </P>
                          """)
    public List<DesignNodesTree.DesignNode> getChildren() {
      return super.getChildren(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }
    

  }
  
  static String relativize(String separator, java.nio.file.Path root, DirCacheTree.Node node) {
    String relativePath = root.relativize(node.getPath()).toString();
    if (!"/".equals(separator)) {
      relativePath = relativePath.replaceAll(Pattern.quote(separator), "/");
    }
    return relativePath;    
  }
  
  @Schema(description = """
                        <P>
                        A pipeline definition file.
                        </P>
                        """)
  public static class DesignFile extends DesignNode {

    private final long size;

    /**
     * Constructor.
     * @param path The path represented by this Node.
     * @param modified The modified timestamp.
     * @param name The name of the file in the parent directory.
     * @param size The size of the file, in bytes.
     */
    public DesignFile(@NotNull String path, @NotNull LocalDateTime modified, @NotNull String name, long size) {
      super(path, modified, name);
      this.size = size;
    }
    
    public DesignFile(java.nio.file.Path root, DirCacheTree.File src) {
      super(relativize(File.separator, root, src), src.getModified(), src.getName());
      this.size = src.getSize();
    }

    /**
     * Get the size of the file on disc, in bytes.
     * @return the size of the file on disc, in bytes.
     */
    @NotNull 
    @Schema(description = """
                          <P>
                          The size of the file on disc, in bytes.
                          </P>
                          """)
    public long getSize() {
      return size;
    }

  }

}
