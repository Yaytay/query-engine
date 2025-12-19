/*
 * Copyright (C) 2025 njt
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
package uk.co.spudsoft.query.testcontainers;

import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.mssqlserver.MSSQLServerContainer;

/**
 *
 * @author njt
 */
public class ImprovedMsSqlDatabaseContainer extends MSSQLServerContainer {

  public ImprovedMsSqlDatabaseContainer(String dockerImageName) {
    super(dockerImageName);
  }

  @Override
  protected void waitUntilContainerStarted() {
    WaitStrategy waitStrategy = getWaitStrategy();
    if (waitStrategy != null) {
        waitStrategy.waitUntilReady(this);
    }
    super.waitUntilContainerStarted();
  }
  
}
