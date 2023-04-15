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
import java.util.regex.Pattern;
import uk.co.spudsoft.xlsx.ColourDefinition;

/**
 *
 * @author njt
 */
@JsonDeserialize(builder = FormatXlsxColours.Builder.class)
public class FormatXlsxColours {
  
  public static final Pattern VALID_COLOUR = Pattern.compile("[0-9A-F]{6}([0-9A-F]{2})?");  
  
  private final String fgColour;
  private final String bgColour;

  public void validate() {
    if (!Strings.isNullOrEmpty(fgColour)) {
      if (!VALID_COLOUR.matcher(fgColour).matches()) {
        throw new IllegalArgumentException("The foreground colour \"" + fgColour + "\" is not valid (must be 6 or 8 hex values).");
      }
    }
    if (!Strings.isNullOrEmpty(bgColour)) {
      if (!VALID_COLOUR.matcher(bgColour).matches()) {
        throw new IllegalArgumentException("The background colour \"" + bgColour + "\" is not valid (must be 6 or 8 hex values).");
      }
    }    
  }
  
  public ColourDefinition toColourDefinition() {
    return new ColourDefinition(
            Strings.isNullOrEmpty(fgColour) ? "000000" : fgColour
            , Strings.isNullOrEmpty(bgColour) ? "FFFFFF" : bgColour
    );
  }

  /**
   * Get the foreground colour to use.
   * @return the foreground colour to use.
   */
  @Schema(description = """
                        <P>The foreground colour to use.</P>
                        <P>
                        Colours must be expressed as 6 or 8 uppercase hexadecimal digits.
                        </P>
                        <P>
                        Some examples:
                        <UL>
                        <LI><font style="color: #FFFFFF">FFFFFF</font>
                        <LI><font style="color: #999999">999999</font>
                        <LI><font style="color: #990000">990000</font>
                        <LI><font style="color: #000099">000099</font>
                        <LI><font style="color: #0A5F42">0A5F42</font>
                        </UL>
                        </P>
                        """
          , defaultValue = "000000"
          , maxLength = 8
  )
  public String getFgColour() {
    return fgColour;
  }


  /**
   * Get the background colour to use.
   * @return the background colour to use.
   */
  @Schema(description = """
                        <P>The background colour to use.</P>
                        <P>
                        Colours must be expressed as 6 or 8 uppercase hexadecimal digits.
                        </P>
                        <P>
                        Some examples:
                        <UL>
                        <LI><font style="background-color: #000000">000000</font>
                        <LI><font style="background-color: #999999">999999</font>
                        <LI><font style="background-color: #990000">990000</font>
                        <LI><font style="background-color: #000099">000099</font>
                        <LI><font style="background-color: #0A5F42">0A5F42</font>
                        </UL>
                        </P>
                        """
          , defaultValue = "FFFFFF"
          , maxLength = 8
  )
  public String getBgColour() {
    return bgColour;
  }
  
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private String fgColour;
    private String bgColour;

    private Builder() {
    }

    public Builder fgColour(final String value) {
      this.fgColour = value;
      return this;
    }

    public Builder bgColour(final String value) {
      this.bgColour = value;
      return this;
    }

    public FormatXlsxColours build() {
      return new uk.co.spudsoft.query.defn.FormatXlsxColours(fgColour, bgColour);
    }
  }

  public static FormatXlsxColours.Builder builder() {
    return new FormatXlsxColours.Builder();
  }

  private FormatXlsxColours(final String fgColour, final String bgColour) {
    this.fgColour = fgColour;
    this.bgColour = bgColour;
  }
  
}
