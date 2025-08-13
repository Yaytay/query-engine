/*
 * Copyright (C) 2025 jtalbut
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
package uk.co.spudsoft.query.exec.fmts.json;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

/**
 * A simple PrettyPrinter implementation that always uses "\n" as the newline character and that allows the specification of an initial nesting amount.
 * 
 * @author jtalbut
 */
public class PrettyPrinterIndent extends DefaultPrettyPrinter {

  private static final long serialVersionUID = 1L;
  
  /**
   * Constructor.
   * 
   * @param initialNesting The amount of nesting to begin with.
   */
  public PrettyPrinterIndent(int initialNesting) {
    this._nesting = initialNesting;
    this._objectIndenter = new DefaultIndenter("  ", "\n");
  }

}
