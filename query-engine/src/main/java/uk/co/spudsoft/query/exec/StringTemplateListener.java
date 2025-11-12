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
package uk.co.spudsoft.query.exec;

import java.util.ArrayList;
import java.util.List;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.misc.STMessage;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * Listener class to collate StringTemplate errors.
 * 
 * @author jtalbut
 */
public class StringTemplateListener implements STErrorListener {
  
  /**
  Category of error from StringTemplate compiler.
  */
  public enum ErrorType {
    /**
     * Error produced during compilation.
     */
    compile
    , 
    /**
     * Error produced during evaluation.
     */
    runTime
    , 
    /**
     * Error produced by I/O operation.
     */
    io
    , 
    /**
     * Internal error with StringTemplate library.
     */
    internal
  }
  
  /**
   * Categorised error from StringTemplate.
   */
  public static class Error {
    /**
     * The category of the error.
     */
    public final ErrorType type;
    /**
     * The error message.
     */
    public final String message;

    /**
     * Constructor.
     * @param type The category of the error.
     * @param msg The error message.
     */
    public Error(ErrorType type, STMessage msg) {
      this.type = type;
      this.message = msg.toString();
    }    
  }

  private List<Error> errors;
  
  /**
   * Constructor.
   */
  public StringTemplateListener() {
    errors = new ArrayList<>();
  }
  
  /**
   * Return an immutable copy of the errors reported to this listener.
   * @return an immutable copy of the errors reported to this listener.
   */
  public List<Error> getErrors() {
    return ImmutableCollectionTools.copy(errors);
  }
  
  @Override
  public void compileTimeError(STMessage msg) {
    errors.add(new Error(ErrorType.compile, msg));
  }

  @Override
  public void runTimeError(STMessage msg) {
    errors.add(new Error(ErrorType.runTime, msg));
  }

  @Override
  public void IOError(STMessage msg) {
    errors.add(new Error(ErrorType.io, msg));
  }

  @Override
  public void internalError(STMessage msg) {
    errors.add(new Error(ErrorType.internal, msg));
  }
  
}
