/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec.procs;

import io.vertx.core.Future;

/**
 * The ChainedAsyncHandler is similar to the {@link AsyncHandler}, but each call also receives an AsyncHandler that it <em>may</em> call to propagate the data.
 * @param <T> The type of data being handled.
 * @author jtalbut
 */
@FunctionalInterface
public interface ChainedAsyncHandler<T> {
 
    /**
     * Handle this data and complete the Future when done.
     * 
     * The implementation should either return a newly complected future (if the data is not to be passed on the pipeline) 
     * or it should return the future returned by {@link AsyncHandler#handle(java.lang.Object)} to pass the data on.
     * Typically this means that the implementations has a structure like: 
     * <pre>
     * Future&lt;Void> handle(T data, AsyncHandler&lt;T> chain) {
     *   return methodThatDoesStuffAsynchronousely(data)
     *     .compose(data -> chain.handle(data));
     * }
     * </pre>
     * 
     * @param data The data to process.
     * @param chain The processor that should be called to pass on the data.
     * @return A Future that will be completed when the work is done.
     */
    Future<Void> handle(T data, AsyncHandler<T> chain);
  
}
