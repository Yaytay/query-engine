/*
 * Copyright (C) 2023 njt
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

/**
 *
 * @author njt
 */
public class TextArea extends Component<TextArea> {
  
  private Integer rows;
  private Boolean wysiwyg;
          
  public TextArea() {
    super("textarea");
  }

  public Integer getRows() {
    return rows;
  }

  public void setRows(Integer rows) {
    this.rows = rows;
  }

  public Boolean getWysiwyg() {
    return wysiwyg;
  }

  public void setWysiwyg(Boolean wysiwyg) {
    this.wysiwyg = wysiwyg;
  }

  public TextArea withRows(final Integer value) {
    this.rows = value;
    return this;
  }

  public TextArea withWysiwyg(final Boolean value) {
    this.wysiwyg = value;
    return this;
  }

  
}
