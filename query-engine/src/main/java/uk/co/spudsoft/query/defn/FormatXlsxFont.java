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
package uk.co.spudsoft.query.defn;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.co.spudsoft.xlsx.FontDefinition;

/**
 *
 * @author njt
 */
@JsonDeserialize(builder = FormatXlsxFont.Builder.class)
public class FormatXlsxFont {
  
  private final String fontName;
  private final int fontSize;

  public FontDefinition toFontDefinition() {
    return new FontDefinition(Strings.isNullOrEmpty(fontName) ? "Calibri" : fontName, fontSize);
  }
  
  public void validate() {
    if (fontSize < 1) {
      throw new IllegalArgumentException("FormatXlsxFont has non-positive fontSize (" + fontSize +").");
    }
  }

  /**
   * Get the name of the font.
   * @return the name of the font.
   */
  @Schema(description = """
                        <P>The name of the font.</P>
                        """
          , maxLength = 100
  )
  public String getFontName() {
    return fontName;
  }

  /**
   * Get the size of the font.
   * @return the size of the font.
   */
  @Schema(description = """
                        <P>The size of the font.</P>
                        <P>
                        Font size is measured in points (approximately 1/72 of an inch).
                        </P>
                        """
  )
  public int getFontSize() {
    return fontSize;
  }
  
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private String fontName = "Calibri";
    private int fontSize = 11;

    private Builder() {
    }

    public Builder fontName(final String value) {
      this.fontName = value;
      return this;
    }

    public Builder fontSize(final int value) {
      this.fontSize = value;
      return this;
    }

    public FormatXlsxFont build() {
      return new uk.co.spudsoft.query.defn.FormatXlsxFont(fontName, fontSize);
    }
  }

  public static FormatXlsxFont.Builder builder() {
    return new FormatXlsxFont.Builder();
  }

  private FormatXlsxFont(final String fontName, final int fontSize) {
    this.fontName = fontName;
    this.fontSize = fontSize;
  }
  
}