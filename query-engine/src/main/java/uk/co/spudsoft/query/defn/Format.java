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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.net.MediaType;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import uk.co.spudsoft.query.exec.FormatInstance;

/**
 * The configuration for the final WriteStream of a pipeline.
 * 
 * Typically the final WriteStream is the HttpResponse, but it can be something else entirely.
 * 
 * The format to use for a pipeline is chosen by iterating through the formats defined for the pipeline and choosing the first that
 * matches any of the selection criteria:
 * <ol>
 * <li><pre>_fmt</pre> query string.<br>
 * If the HTTP request includes a <pre>_fmt</pre> query string argument each Format specified in the Pipeline will be checked (in order)
 * for a matching response from the {@link uk.co.spudsoft.query.defn.Format#getName()} method.
 * The first matching Format will be returned.
 * If no matching Format is found an error will be returned.
 * <li>Path extension.<br>
 * If the path in the HTTP request includes a '.' (U+002E, Unicode FULL STOP) after the last '/' (U+002F, Unicode SOLIDUS) character everything following that 
 * character will be considered to be the extension, furthermore the extension (and full stop character) will be removed from the filename being sought.
 * If an extension is found each Format specified in the Pipeline will be checked (in order)
 * for a matching response from the {@link uk.co.spudsoft.query.defn.Format#getExtension()} method.
 * The first matching Format will be returned.
 * If no matching Format is found an error will be returned.
 * <li>Accept header.<br>
 * If the HTTP request includes an 'Accept' header each Format specified in the Pipeline will be checked (in order)
 * for a matching response from the {@link uk.co.spudsoft.query.defn.Format#getMediaType() ()} method.
 * Note that most web browsers include "*\/*" in their default Accept headers, which will match any Format that specifies a MediaType.
 * The first matching Format will be returned.
 * If no matching Format is found an error will be returned.
 * <li>Default<br>
 * If the request does not use any of these mechanisms then the first Format specified in the Pipeline will be used.
 * </ol>
 * 
 * @author jtalbut
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME, 
  include = JsonTypeInfo.As.EXISTING_PROPERTY,   
  property = "type")
@JsonSubTypes({ 
  @JsonSubTypes.Type(value = FormatJson.class, name = "JSON"), 
  @JsonSubTypes.Type(value = FormatHtml.class, name = "HTML"), 
  @JsonSubTypes.Type(value = FormatXlsx.class, name = "XLSX"), 
  @JsonSubTypes.Type(value = FormatDelimited.class, name = "Delimited")
})
@Schema(
        description = """
                      <P>The configuration for the final WriteStream of a pipeline.</P>
                      <P>
                      Typically the final WriteStream is the HttpResponse.
                      </P>
                      <P>
                      The format to use for a pipeline is chosen by according to the following rules:
                      <ol>

                      <li><pre>_fmt</pre> query string.<br>
                      If the HTTP request includes a <pre>_fmt</pre> query string argument each Format specified in the Pipeline will be checked (in order)
                      for a matching response from the {@link uk.co.spudsoft.query.defn.Format#getName()} method.
                      The first matching Format will be returned.
                      If no matching Format is found an error will be returned.

                      <li>Path extension.<br>
                      If the path in the HTTP request includes a '.' (U+002E, Unicode FULL STOP) after the last '/' (U+002F, Unicode SOLIDUS) character everything following that
                      character will be considered to be the extension, furthermore the extension (and full stop character) will be removed from the filename being sought.
                      If an extension is found each Format specified in the Pipeline will be checked (in order)
                      for a matching response from the {@link uk.co.spudsoft.query.defn.Format#getExtension()} method.
                      The first matching Format will be returned.
                      If no matching Format is found an error will be returned.

                      <li>Accept header.<br>
                      If the HTTP request includes an 'Accept' header each Format specified in the Pipeline will be checked (in order)
                      for a matching response from the {@link uk.co.spudsoft.query.defn.Format#getMediaType() ()} method.
                      Note that most web browsers include "*\\/*" in their default Accept headers, which will match any Format that specifies a MediaType.
                      The first matching Format will be returned.
                      If no matching Format is found an error will be returned.

                      <li>Default<br>
                      If the request does not use any of these mechanisms then the first Format specified in the Pipeline will be used.
                      </ol>
                      <p>

                      """
        , discriminatorProperty = "type"
        , discriminatorMapping = {
          @DiscriminatorMapping(schema = FormatJson.class, value = "JSON")
          , @DiscriminatorMapping(schema = FormatHtml.class, value = "HTML")
          , @DiscriminatorMapping(schema = FormatXlsx.class, value = "XLSX")
          , @DiscriminatorMapping(schema = FormatDelimited.class, value = "Delimited")
        }
)
public interface Format {
  
  /**
   * Throw an IllegalArgumentException if the Format is not usable.
   * @throws IllegalArgumentException if the Format is not usable.
   */
  void validate();
  
  /**
   * Create a FormatInstance.
   * Each implementation of a FormatInstance should subclass this class and provide a concrete implementation of this method.
   * Note that the context passed in to this method may not be the same as that returned by vertx.getOrCreateContext.
   * @param vertx The Vertx instance that will be used for the data processing.
   * @param context The Vertx context that will be used for this format.
   * @param writeStream The write stream that will contain the result.
   * @return a newly created FormatInstance object that will be used for processing the pipeline format.
   */
  @JsonIgnore
  FormatInstance createInstance(Vertx vertx, Context context, WriteStream<Buffer> writeStream);
  
  /**
   * Get the type of Format being configured.
   * @return the type of Format being configured.
   */
  @Schema(description = """
                        <P>The type of Format being configured.<P>
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  FormatType getType();
  
  /**
   * Get the name of the format, as will be used on query string parameters.
   * No two formats in a single pipeline should have the same name.
   * @return the name of the format, as will be used on query string parameters.
   */
  @Schema(description = """
                        <P>The name of the format.</P>
                        <P>
                        The name is used to determine the format based upon the '_fmt' query string argument.
                        </P>
                        <P>
                        It is an error for two Formats to have the same name.
                        This is different from the other Format determinators which can be repeated, the name is the
                        ultimate arbiter and must be unique.
                        This ensures that all configured Formats can be used.
                        </P>
                        """
          , maxLength = 100
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  String getName();
  
  /**
   * Get the extension of the format.
   * The extension is used to determine the format based upon the URL path and also to set the default filename for the content-disposition header.
   * If multiple formats have the same extension the first in the list will be used.
   * @return the extension of the format.
   */
  @Schema(description = """
                        <P>The extension of the format.</P>
                        <P>
                        The extension is used to determine the format based upon the URL path and also to set the default filename for the content-disposition header.
                        If multiple formats have the same extension the first in the list will be used.
                        </P>
                        """
          , maxLength = 20
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  String getExtension();
  
  /**
   * Get the media type of the format.
   * The media type is used to determine the format based upon the Accept header in the request.
   * If multiple formats have the same media type the first in the list will be used.
   * The media type will also be set as the Content-Type header in the response.
   * @return the media type of the format.
   */
  @Schema(description = """
                        <P>The media type of the format.</P>
                        <P>
                        The media type is used to determine the format based upon the Accept header in the request.
                        If multiple formats have the same media type the first in the list will be used.
                        </P>
                        <P>
                        The media type will also be set as the Content-Type header in the response.
                        </P>
                        """
            , requiredMode = Schema.RequiredMode.NOT_REQUIRED
)
  MediaType getMediaType();

  /**
   * Helper method for implementation to validate that they have been configured with the required {@link FormatType}.
   * <p>
   * There is no reason for users to specify the format type, but they can, so it is necessary to validate it.
   * 
   * @param required the {@link FormatType} that the {@link Format} requires.
   * @param actual the {@link FormatType} configured.
   */
  default void validateType(FormatType required, FormatType actual) {
    if (required != actual) {
      throw new IllegalArgumentException("Format of type " + required + " configured with type " + actual);
    }
  }
  
}
