/*
 * Copyright (C) 2022 jtalbut
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.co.spudsoft.query.exec.procs;

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
