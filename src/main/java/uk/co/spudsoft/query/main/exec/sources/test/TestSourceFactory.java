/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.sources.test;

import io.vertx.core.Context;
import uk.co.spudsoft.query.main.defn.Source;
import uk.co.spudsoft.query.main.defn.SourceTest;
import uk.co.spudsoft.query.main.exec.SourceInstanceFactory;

/**
 *
 * @author jtalbut
 */
public class TestSourceFactory implements SourceInstanceFactory<SourceTest, TestSource> {

  @Override
  public TestSource create(Context context, Source definition) {
    return new TestSource(context, (SourceTest) definition);
  }
  
}
