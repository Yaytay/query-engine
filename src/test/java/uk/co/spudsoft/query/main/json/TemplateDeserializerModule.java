/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.json;

import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 *
 * @author jtalbut
 */
public class TemplateDeserializerModule extends SimpleModule {

  private static final long serialVersionUID = 1987676L;

  public TemplateDeserializerModule() {
    super();
    addDeserializer(String.class, new TemplateStringDeserializer());
    addDeserializer(Integer.class, new TemplateIntegerDeserializer(Integer.class));
    addDeserializer(Integer.TYPE, new TemplateIntegerDeserializer(Integer.TYPE));
  }
}
