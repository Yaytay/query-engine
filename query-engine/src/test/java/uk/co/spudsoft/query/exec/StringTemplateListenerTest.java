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
package uk.co.spudsoft.query.exec;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.stringtemplate.v4.misc.ErrorType;
import org.stringtemplate.v4.misc.STMessage;


/**
 *
 * @author njt
 */
public class StringTemplateListenerTest {
  
  @Test
  public void testCompileError() {
    StringTemplateListener listener = new StringTemplateListener();
    listener.compileTimeError(new STMessage(ErrorType.NO_SUCH_OPTION, null, new IllegalArgumentException("test")));
    assertEquals(1, listener.getErrors().size());
    assertEquals(StringTemplateListener.ErrorType.compile, listener.getErrors().get(0).type);
    assertThat(listener.getErrors().get(0).message, startsWith("no such option: null"));    
  }
  
  @Test
  public void testInternalError() {
    StringTemplateListener listener = new StringTemplateListener();
    listener.internalError(new STMessage(ErrorType.NO_SUCH_OPTION, null, new IllegalArgumentException("test")));
    assertEquals(1, listener.getErrors().size());
    assertEquals(StringTemplateListener.ErrorType.internal, listener.getErrors().get(0).type);
    assertThat(listener.getErrors().get(0).message, startsWith("no such option: null"));    
  }
  
  @Test
  public void testIoError() {
    StringTemplateListener listener = new StringTemplateListener();
    listener.IOError(new STMessage(ErrorType.NO_SUCH_OPTION, null, new IllegalArgumentException("test")));
    assertEquals(1, listener.getErrors().size());
    assertEquals(StringTemplateListener.ErrorType.io, listener.getErrors().get(0).type);
    assertThat(listener.getErrors().get(0).message, startsWith("no such option: null"));    
  }
  
  @Test
  public void testRunTimeError() {
    StringTemplateListener listener = new StringTemplateListener();
    listener.runTimeError(new STMessage(ErrorType.NO_SUCH_OPTION, null, new IllegalArgumentException("test")));
    assertEquals(1, listener.getErrors().size());
    assertEquals(StringTemplateListener.ErrorType.runTime, listener.getErrors().get(0).type);
    assertThat(listener.getErrors().get(0).message, startsWith("no such option: null"));    
  }

}
