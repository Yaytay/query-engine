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

import java.time.LocalDateTime;
import java.util.List;

/**
 * A Node in a DirCache representing a directory on disc.
 * Directory objects may be used as hash keys, but doing so requires the calculation of hash codes and equality for all children.
 * 
 * @author jtalbut
 */
public class Directory extends Node {
  
  private final List<Node> children;

  /**
   * Constructor.
   * @param path The path represented by this Node.
   * @param modified The modified timestamp.
   * @param name The name of the directory in the parent directory.
   * @param children The children of the Directory - in the order returned by FileWalker (which will be dirs first, then probably sorted by name).
   */
  public Directory(String path, LocalDateTime modified, String name, List<Node> children) {
    super(path, modified, name);
    this.children = List.copyOf(children);
  }

  /**
   * Get the children of the Directory.
   * @return the children of the Directory.
   */
  public List<Node> getChildren() {
    return children;
  }
  
}
