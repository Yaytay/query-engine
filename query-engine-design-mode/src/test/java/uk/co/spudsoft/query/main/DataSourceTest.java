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
package uk.co.spudsoft.query.main;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 *
 * @author jtalbut
 */
public class DataSourceTest {
  
  @Test
  public void testGetUrl() {
    DataSourceConfig ds = new DataSourceConfig().setUrl("url");
    assertThat(ds.getUrl(), equalTo("url"));
  }

  @Test
  public void testGetSchema() {
    DataSourceConfig ds = new DataSourceConfig().setSchema("schema");
    assertThat(ds.getSchema(), equalTo("schema"));
  }

  @Test
  public void testGetUser() {
    DataSourceConfig ds = new DataSourceConfig().setUser(new Credentials().setUsername("user"));
    assertThat(ds.getUser().getUsername(), equalTo("user"));
  }

  @Test
  public void testGetAdminUser() {
    DataSourceConfig ds = new DataSourceConfig().setAdminUser(new Credentials().setUsername("admin"));
    assertThat(ds.getAdminUser().getUsername(), equalTo("admin"));
  }

  @Test
  public void testGetMaxPoolSize() {
    DataSourceConfig ds = new DataSourceConfig();
    assertThat(ds.getMaxPoolSize(), equalTo(10));    
    ds = new DataSourceConfig().setMaxPoolSize(17);
    assertThat(ds.getMaxPoolSize(), equalTo(17));
  }

}
