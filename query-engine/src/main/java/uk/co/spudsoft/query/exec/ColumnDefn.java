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
package uk.co.spudsoft.query.exec;

import io.vertx.sqlclient.desc.ColumnDescriptor;
import java.sql.JDBCType;
import uk.co.spudsoft.query.defn.DataType;

/**
 * The definition of a column.
 * <P>
 * This is not part of the definition of a pipeline, but is used in the output from pipelines.
 * </P>
 * @author jtalbut
 */
public class ColumnDefn implements ColumnDescriptor {
  
  private final String name;
  private final String key;
  private final DataType type;

  /**
   * Constructor.
   * @param name The name of the column.
   * @param type The type of the column.
   */
  public ColumnDefn(String name, DataType type) {
    this.name = name;
    this.key = name;
    this.type = type;
  }

  /**
   * Constructor.
   * @param name The name of the column.
   * @param key The key of the column.
   * @param type The type of the column.
   */
  public ColumnDefn(String name, String key, DataType type) {
    this.name = name;
    this.key = key;
    this.type = type;
  }

  @Override
  public String name() {
    return name;
  }

  /**
   * Get the key for the column.
   * The key is usually the same as the name, but if a column may be created with two names that differ only in case
   * using a case-insensitive key can allow the engine to consider them the same.
   * @return the key for the column.
   */
  public String key() {
    return key;
  }

  @Override
  public boolean isArray() {
    return false;
  }

  @Override
  public String typeName() {
    return type.name();
  }

  @Override
  public JDBCType jdbcType() {
    return type.jdbcType();
  }

  /**
   * Get the type of the column.
   * @return the type of the column.
   */
  public DataType type() {
    return type;
  }

  @Override
  public String toString() {
    return name + ":" + type;
  }
  
}
