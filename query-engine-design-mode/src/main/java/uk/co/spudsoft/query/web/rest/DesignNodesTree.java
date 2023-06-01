/*
 * Copyright (C) 2023 njt
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
 * @author njt
 */
public class DesignNodesTree extends AbstractTree {
  
  @JsonSubTypes({
    @JsonSubTypes.Type(value = DesignNodesTree.DesignDir.class),
    @JsonSubTypes.Type(value = DesignNodesTree.DesignFile.class)
  })
  public static abstract class DesignNode extends AbstractNode<DesignNode> {

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
     * Get the {@link java.nio.file.Path} that relates to this Node.
     * @return the {@link java.nio.file.Path} that relates to this Node.
     */
    @NotNull 
    public String getPath() {
      return path;
    }

    /**
     * Get the modified timestamp.
     * @return the modified timestamp.
     */
    @NotNull 
    public LocalDateTime getModified() {
      return modified;
    }

    /**
     * Get the name of the directory entry.
     * @return the name of the directory entry.
     */
    @NotNull 
    @Override
    public String getName() {
      return name;
    }

  }

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
    public DesignDir(String path, LocalDateTime modified, String name, List<DesignNode> children) {
      super(path, modified, name, List.copyOf(children));
    }
    
    public DesignDir(java.nio.file.Path root, DirCacheTree.Directory src, String name) {
      super(relativize(root, src), src.getModified(), name, buildChildren(root, src));
    }
    
    /**
     * Get the children of the Directory.
     * @return the children of the Directory.
     */
    @Override
    @NotNull
    @Schema(nullable = false)
    public List<DesignNodesTree.DesignNode> getChildren() {
      return super.getChildren(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }
    

  }
  
  private static String relativize(java.nio.file.Path root, DirCacheTree.Node node) {
    String relativePath = root.relativize(node.getPath()).toString();
    if (File.separatorChar != '/') {
      relativePath = relativePath.replaceAll(Pattern.quote(File.separator), "/");
    }
    return relativePath;    
  }
  
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
      super(relativize(root, src), src.getModified(), src.getName());
      this.size = src.getSize();
    }

    /**
     * Get the size of the file on disc, in bytes.
     * @return the size of the file on disc, in bytes.
     */
    @NotNull 
    public long getSize() {
      return size;
    }

  }

}
