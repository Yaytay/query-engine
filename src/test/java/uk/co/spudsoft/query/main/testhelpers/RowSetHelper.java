/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.testhelpers;

import io.vertx.core.json.JsonArray;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

/**
 *
 * @author jtalbut
 */
public class RowSetHelper {
  
  public static String toString(RowSet<Row> rowSet) {
    JsonArray ja = new JsonArray();
    for (Row row : rowSet) {
      ja.add(row.toJson());
    }
    return ja.encode();
  }

}
