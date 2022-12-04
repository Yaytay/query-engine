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

import com.google.common.collect.ImmutableList;
import java.util.List;
import uk.co.spudsoft.dircache.AbstractTree;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;
import uk.co.spudsoft.query.defn.Format;

/**
 *
 * @author jtalbut
 */
public class AccessibleNodesTree extends AbstractTree {

  private AccessibleNodesTree() {
  }
  
  public static class Node extends AbstractNode<Node> {
    private final String path; 

    public Node(String path) {
      super(nameFromPath(path));
      this.path = undot(path);
    }    

    public Node(String path, List<Node> children) {
      super(nameFromPath(path), children);
      this.path = undot(path);      
    }

    
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

    static String undot(String path) {
      int dotPos = path.indexOf(".");
      if (dotPos > 0) {
        path = path.substring(0, dotPos);
      }
      return path;
    }
  }
  
  public static class Dir extends Node {
    public Dir(String path,  List<Node> children) {
      super(path, children);
    }
  }
  
  public static class Pipeline extends Node {
    private String title; 
    private String description; 
    private ImmutableList<Argument> arguments;
    private ImmutableList<Format> destinations;

    public Pipeline(String path, String title, String description, List<Argument> arguments, List<Format> destinations) {
      super(path);
      this.title = title;
      this.description = description;
      this.arguments = ImmutableCollectionTools.copy(arguments);
      this.destinations = ImmutableCollectionTools.copy(destinations);
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    public ImmutableList<Argument> getArguments() {
      return arguments;
    }

    public ImmutableList<Format> getDestinations() {
      return destinations;
    }

  }
  
}
