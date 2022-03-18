/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

/**
 *
 * @author jtalbut
 */
public enum EndpointType {
  /**
   * The endpoint is a datasource compatible with the eclipse vertx-sql-client.
   */
  SQL
  , 
  /**
   * The endpoint returns JSON or XML data from an http/https URL.
   */
  HTTP
}
