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
