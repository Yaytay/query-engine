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
package uk.co.spudsoft.query.testcontainers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;


/**
 *
 * @author jtalbut
 */
public abstract class AbstractServerProvider {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(AbstractServerProvider.class);

  public static final String ROOT_PASSWORD = "T0p-secret"; 
  
  protected abstract String getScript();
  
  protected abstract String getName();
  
  protected Container findContainer(String containerName) {
    DockerClient dockerClient = DockerClientFactory.lazyClient();
    Container createdContainer = dockerClient.listContainersCmd().withShowAll(true).exec().stream().filter(container -> {
      return Arrays.asList(container.getNames()).contains(containerName);
    }).findFirst().orElse(null);
    if (createdContainer != null) {
      logger.info("Container {} has state {}", createdContainer.getNames(), createdContainer.getState());
      logger.info("Container {} has status {}", createdContainer.getNames(), createdContainer.getStatus());
      if (!"running".equals(createdContainer.getState())) {
        return null;
      }
    }
    return createdContainer;
  }
  
}
