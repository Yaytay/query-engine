/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.sources.sql;

import io.vertx.core.Context;
import uk.co.spudsoft.query.main.defn.Source;
import uk.co.spudsoft.query.main.defn.SourceSql;
import uk.co.spudsoft.query.main.exec.SourceInstanceFactory;

/**
 *
 * @author jtalbut
 */
public class BlockingSqlQuerySourceFactory implements SourceInstanceFactory<SourceSql, BlockingSqlQuerySource> {

  @Override
  public BlockingSqlQuerySource create(Context context, Source definition) {
    return new BlockingSqlQuerySource(context, (SourceSql) definition);
  }
  
}
