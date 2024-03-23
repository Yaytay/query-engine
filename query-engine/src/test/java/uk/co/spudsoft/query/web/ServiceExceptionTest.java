/*
 * Copyright (C) 2024 njt
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
package uk.co.spudsoft.query.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import org.junit.jupiter.api.Test;


/**
 *
 * @author njt
 */
public class ServiceExceptionTest {
  
  @Test
  public void testRethrowOrWrapSimple() {
    ServiceException simple = ServiceException.rethrowOrWrap(new ServiceException(400, "bob"));
    assertThat(simple, instanceOf(ServiceException.class));
    assertThat(simple.getCause(), nullValue());
    assertThat(simple.getStatusCode(), equalTo(400));
    assertThat(simple.getMessage(), equalTo("bob"));
  }
  
  @Test
  public void testRethrowOrWrapWrapped() {
    ServiceException wrapper = ServiceException.rethrowOrWrap(new IllegalArgumentException("bob"));
    assertThat(wrapper, instanceOf(ServiceException.class));
    assertThat(wrapper.getStatusCode(), equalTo(500));
    assertThat(wrapper.getMessage(), equalTo("Failed to execute query"));
    
    Throwable wrapped = wrapper.getCause();
    assertThat(wrapped, instanceOf(IllegalArgumentException.class));
    assertThat(wrapped.getCause(), nullValue());
    assertThat(wrapped.getMessage(), equalTo("bob"));
  }
  
}
