/*
 * Copyright (C) 2023 jtalbut
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
package uk.co.spudsoft.query.web.formio;

import com.fasterxml.jackson.core.JsonGenerator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;

/**
 *
 * @author jtalbut
 */
public class TextArea extends Component<TextArea> {

  @SuppressFBWarnings({"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"})
  public TextArea(JsonGenerator generator) throws IOException {
    super(generator, "textarea");
  }

  public TextArea withRows(final Integer value) throws IOException {
    return with("rows", value);
  }

  public TextArea withWysiwyg(final Boolean value) throws IOException {
    return with("wysiwyg", value);
  }
  
}
