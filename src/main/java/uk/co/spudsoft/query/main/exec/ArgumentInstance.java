/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import uk.co.spudsoft.query.main.defn.Argument;

/**
 *
 * @author jtalbut
 */
public class ArgumentInstance {
  
  private Argument definition;
  private String value;

  public ArgumentInstance(Argument definition, String value) {
    this.definition = definition;
    this.value = value;
  }
  
  public Argument getDefinition() {
    return definition;
  }

  public String getValue() {
    return value;
  }
    
}
