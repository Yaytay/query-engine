/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.sources.sql;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowStream;

/**
 *
 * @author jtalbut
 */
public class RowStreamWrapper implements ReadStream<JsonObject> {
  
  private final RowStream<Row> rowStream;
  private Handler<Throwable> exceptionHandler;

  public RowStreamWrapper(RowStream<Row> rowStream) {
    this.rowStream = rowStream;
  }

  @Override
  public ReadStream<JsonObject> exceptionHandler(Handler<Throwable> handler) {
    rowStream.exceptionHandler(handler);
    this.exceptionHandler = handler;
    return this;            
  }

  @Override
  public ReadStream<JsonObject> handler(Handler<JsonObject> handler) {
    rowStream.handler(row -> {
      try {
        JsonObject json = row.toJson();
        handler.handle(json);
      } catch(Throwable ex) {
        if (exceptionHandler != null) {
          exceptionHandler.handle(ex);
        }
      }
    });
    return this;
  }

  @Override
  public ReadStream<JsonObject> pause() {
    rowStream.pause();
    return this;
  }

  @Override
  public ReadStream<JsonObject> resume() {
    rowStream.resume();
    return this;
  }

  @Override
  public ReadStream<JsonObject> fetch(long amount) {
    rowStream.fetch(amount);
    return this;
  }

  @Override
  public ReadStream<JsonObject> endHandler(Handler<Void> endHandler) {
    rowStream.endHandler(endHandler);
    return this;
  }
  
  
  
}
