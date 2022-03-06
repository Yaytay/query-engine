/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.sources.http;

import io.vertx.core.Context;
import uk.co.spudsoft.query.main.defn.Source;
import uk.co.spudsoft.query.main.defn.SourceHttp;
import uk.co.spudsoft.query.main.exec.SourceInstanceFactory;

/**
 *
 * @author jtalbut
 */
public class HttpQuerySourceFactory implements SourceInstanceFactory<SourceHttp, HttpQuerySource> {

  @Override
  public HttpQuerySource create(Context context, Source definition) {
    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
  }
  
}
