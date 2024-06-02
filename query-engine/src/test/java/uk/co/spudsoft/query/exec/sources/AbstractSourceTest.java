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
package uk.co.spudsoft.query.exec.sources;

import io.vertx.core.Context;
import io.vertx.core.Future;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.exec.PipelineExecutor;
import uk.co.spudsoft.query.exec.PipelineInstance;
import uk.co.spudsoft.query.exec.SourceInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import uk.co.spudsoft.query.exec.ReadStreamWithTypes;

/**
 *
 * @author jtalbut
 */
public class AbstractSourceTest {
  
  @Test
  public void testAddNameToContextLocalData() {
    AbstractSourceImpl asi = new AbstractSourceImpl("bob");
    assertEquals("bob", asi.getName());
    asi.addNameToContextLocalData(null);
    Context context = mock(Context.class);
    asi.addNameToContextLocalData(context);
    verify(context).putLocal(SourceInstance.SOURCE_CONTEXT_KEY, "bob");
  }

  public class AbstractSourceImpl extends AbstractSource {

    public AbstractSourceImpl(String name) {
      super(name);
    }

    @Override
    public Future<ReadStreamWithTypes> initialize(PipelineExecutor executor, PipelineInstance pipeline) {
      throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

  }
  
}
