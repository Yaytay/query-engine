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
 *
 * @author jtalbut
 */
public class DocNodesTree extends AbstractTree {

  private DocNodesTree() {
  }
  
  @JsonSubTypes({
    @JsonSubTypes.Type(value = DocNodesTree.DocDir.class),
    @JsonSubTypes.Type(value = DocNodesTree.DocFile.class)
  })
  public static class DocNode extends AbstractNode<DocNode> {
    
    private final String path; 

    public DocNode(String path) {
      super(nameFromPath(path));
      this.path = path;
    }    

    public DocNode(String path, List<DocNode> children) {
      super(nameFromPath(path), children);
      this.path = path;      
    }

    @NotNull
    public String getPath() {
      return path;
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

  }
  
  public static class DocDir extends DocNode {
    
    public DocDir(String path,  List<DocNode> children) {
      super(path, children);
    }

    @Override
    @NotNull
    @Schema(nullable = false)
    public List<DocNode> getChildren() {
      return super.getChildren(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }
    
  }
  
  public static class DocFile extends DocNode {
    
    private final String title;
    
    @JsonCreator
    public DocFile(String path, String title) {
      super(path);
      this.title = Objects.requireNonNull(title);
    }

    @NotNull
    public String getTitle() {
      return title;
    }
    
  }
  
}
