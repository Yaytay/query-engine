/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.sources.test;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import java.util.Map;
import uk.co.spudsoft.query.main.defn.Endpoint;
import uk.co.spudsoft.query.main.defn.SourceTest;
import uk.co.spudsoft.query.main.exec.SourceInstance;

/**
 *
 * @author jtalbut
 */
public class TestSource implements SourceInstance<SourceTest> {

  private final Context context;
  private final int rowCount;
  private final BlockingReadStream<JsonObject> stream;


  public TestSource(Context context, SourceTest definition) {
    this.context = context;
    this.rowCount = definition.getRowCount();
    this.stream = new BlockingReadStream<>(context, rowCount);
  }    

  @Override
  public Future<Void> initialize(Map<String, Endpoint> endpoints) {
    stream.pause();
    try {
      for (int i = 0; i < rowCount; ++i) {
        stream.add(new JsonObject().put("value", i));
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

