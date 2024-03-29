/*
 * Copyright (C) 2024 jtalbut
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
package uk.co.spudsoft.query.main;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.healthchecks.Status;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simple Status check that uses an AtomicBoolean to check the state.
 * 
 * @author jtalbut
 */
public class UpCheckHandler implements Handler<Promise<Status>> {
  
  private final AtomicBoolean up;

  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The whole point is that the mutable object gets modified externally")
  public UpCheckHandler(AtomicBoolean up) {
    this.up = up;
  }

  @Override
  public void handle(Promise<Status> event) {
    if (up.get()) {
      event.complete(Status.OK());
    } else {
      event.complete(Status.KO());
    }
  }
  
}
