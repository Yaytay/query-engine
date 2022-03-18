/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

/**
 * The data type of arguments to pipelines.
 * @author jtalbut
 */
public enum ArgumentType {
  /**
   * The argument is something that can be stored in a Java String.
   */
  String,
  /**
   * The argument must be something that can be parsed from a String using {@link java.lang.Integer#parseInt(java.lang.String)}.
   */
  Integer,
  /**
   * The argument must be something that can be parsed from a String using {@link java.lang.Long#parseLong(java.lang.String)}.
   */
  Long,
  /**
   * The argument must be something that can be parsed from a String using {@link java.lang.Double#parseDouble(java.lang.String)}.
   */
  Double,
  /**
   * The argument must be something that can be parsed from a String using {@link java.time.LocalDateTime#parse(java.lang.CharSequence)}.
   */
  DateTime
}
