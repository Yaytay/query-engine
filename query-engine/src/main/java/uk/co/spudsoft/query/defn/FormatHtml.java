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
import com.google.common.net.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.exec.fmts.html.FormatHtmlInstance;

/**
 * The definition of an HTML output format.
 * @author jtalbut
 */
@JsonDeserialize(builder = FormatHtml.Builder.class)
@Schema(
        description = """
                      <P>The definition of an HTML output format.</P>
                      <P>
                      The HTML output format produces an HTML snippet containing a table.
                      The output itself has no formatting, but a number of CSS classes are applied to the elements enabling the UI to format them as they wish.
                      </P>
                      <P>
                      The CSS classes are:
                      <UL>
                      <LI>header</BR>
                      The header row.
                      <LI>dataRow</BR>
                      A row of data (other than the header row).
                      <LI>evenRow</BR>
                      An even numbered data row (the first dataRow is row 0, which is even).
                      <LI>oddRow</BR>
                      An odd numbered data row (the first dataRow is row 0, which is even).
                      <LI>evenCol</BR>
                      An even numbered column (header or dataRow, the first column is 0, which is even).
                      <LI>oddRow</BR>
                      An odd numbered column (header or dataRow, the first column is 0, which is even).
                      </UL>
                      </P>
                      """
)
public class FormatHtml extends AbstractTextFormat implements Format {
  
  private static final String DEFAULT_NAME = "html";

  private static final String DEFAULT_EXTENSION = "html";
  private static final String DEFAULT_MEDIA_TYPE = "text/html;charset=UTF-8";
  
  @Override
  public FormatHtmlInstance createInstance(Vertx vertx, RequestContext requestContext, WriteStream<Buffer> writeStream) {
    return new FormatHtmlInstance(this, requestContext, writeStream);
  }

  @Override
  public void validate() {
    super.validate(FormatType.HTML, null, null);
  }
    
  /**
   * Builder class for FormatJson.
   */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends AbstractTextFormat.Builder<Builder> {

    /**
     * Default constructor.
     */
    public Builder() {
      super(FormatType.HTML, DEFAULT_NAME, null, DEFAULT_EXTENSION, null, MediaType.parse(DEFAULT_MEDIA_TYPE), false
              , null, null, null, null, null
      );
    }
    
    /**
     * Construct a new instance of the FormatHtml class.
     * @return a new instance of the FormatHtml class.
     */
    public FormatHtml build() {
      return new FormatHtml(this);
    }
  }

  /**
   * Construct a new instance of the FormatJson.Builder class.
   * @return a new instance of the FormatJson.Builder class.
   */
  public static FormatHtml.Builder builder() {
    return new FormatHtml.Builder();
  }

  private FormatHtml(Builder builder) { 
    super(builder);
    validateType(FormatType.HTML, getType());
  }
  
}
