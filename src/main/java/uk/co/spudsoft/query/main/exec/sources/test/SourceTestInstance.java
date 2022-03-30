/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.sources.test;

import com.google.common.base.Strings;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import uk.co.spudsoft.query.main.defn.SourceTest;
import uk.co.spudsoft.query.main.exec.PipelineExecutor;
import uk.co.spudsoft.query.main.exec.PipelineInstance;
import uk.co.spudsoft.query.main.exec.SourceInstance;

/**
 *
 * @author jtalbut
 */
public class SourceTestInstance implements SourceInstance {

  private final int rowCount;
  private final String name;
  private final BlockingReadStream<JsonObject> stream;


  public SourceTestInstance(Context context, SourceTest definition) {
    this.rowCount = definition.getRowCount();
    this.name = definition.getName();
    this.stream = new BlockingReadStream<>(context, rowCount);
  }    

  @Override
  public Future<Void> initialize(PipelineExecutor executor, PipelineInstance pipeline) {
    stream.pause();
    try {
      for (int i = 0; i < rowCount; ++i) {
        JsonObject data = new JsonObject();
        data.put("value", i);
        if (!Strings.isNullOrEmpty(name)) {
          data.put("name", name);
        }
        stream.add(data);
      }
      stream.end();
    } catch(Throwable ex) {
      return Future.failedFuture(ex);
    }
    return Future.succeededFuture();
  }

  @Override
  public ReadStream<JsonObject> getReadStream() {
    return stream;
  }
}

