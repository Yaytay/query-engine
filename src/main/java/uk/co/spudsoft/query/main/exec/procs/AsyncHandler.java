/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.procs;

import io.vertx.core.Future;

/**
 * The AsyncHandler class is like the {@link io.vertx.core.Handler} class, but asynchronous.
 * 
 * @author jtalbut
 * @param <T> The type of data being handled.
 */
@FunctionalInterface
public interface AsyncHandler<T> {

    /**
     * Handle this data and complete the Future when done.
     * 
     * @param data The data to be handled.
     * @return A Future that will be completed when the work is done.
     */
    Future<Void> handle(T data);
  
}
