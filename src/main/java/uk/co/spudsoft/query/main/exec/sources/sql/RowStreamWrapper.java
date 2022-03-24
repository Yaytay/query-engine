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
import io.vertx.sqlclient.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 */
public class RowStreamWrapper implements ReadStream<JsonObject> {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(RowStreamWrapper.class);
  
  private final RowStream<Row> rowStream;
  private final Transaction transaction;
  private Handler<Throwable> exceptionHandler;

  public RowStreamWrapper(Transaction transaction, RowStream<Row> rowStream) {
    this.transaction = transaction;
    this.rowStream = rowStream;
  }

  @Override
  public ReadStream<JsonObject> exceptionHandler(Handler<Throwable> handler) {
    this.exceptionHandler = handler;
    rowStream.exceptionHandler(ex -> {
      logger.warn("Exception in RowStream: ", ex);
      this.exceptionHandler.handle(ex);
    });
    return this;            
  }
  
  @Override
  public ReadStream<JsonObject> handler(Handler<JsonObject> handler) {
    logger.trace("handler({})", handler);
    if (handler == null) {
      // When rowStream.handler is called with a non-null handler it will always handle the currently in-memory rows
      // So must not pass in a 'debug' handler here.
      rowStream.handler(null);
    } else {
      rowStream.handler(row -> {
        try {
          JsonObject json = row.toJson();
          logger.trace("{} Received row: {}", this, json);
          handler.handle(json);
        } catch(Throwable ex) {
          if (exceptionHandler != null) {
            exceptionHandler.handle(ex);
          }
        }
      });
    }
    return this;
  }
  
  @Override
  public ReadStream<JsonObject> pause() {
    logger.trace("{} paused", this);
    rowStream.pause();
    return this;
  }

  @Override
  public ReadStream<JsonObject> resume() {
    logger.trace("{} resumed", this);
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
    rowStream.endHandler(ehv -> {
      rowStream.close()
              .compose(v -> {
                return transaction.commit();
              })
              .onComplete(ar -> {
                if (!ar.succeeded()) {
                  logger.warn("Transaction failed: ", ar.cause());
                }
                endHandler.handle(null);                
              });
    });
    return this;
  }
  
  
  
}
