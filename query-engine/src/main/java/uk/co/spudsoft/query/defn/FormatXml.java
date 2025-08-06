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

package uk.co.spudsoft.query.defn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.MediaType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import uk.co.spudsoft.query.exec.fmts.xml.FormatXmlInstance;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import uk.co.spudsoft.query.exec.context.RequestContext;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * Output the data stream in XML.
 * This format has no specific configuration options.
 *
 * @author jtalbut
 */
@JsonDeserialize(builder = FormatXml.Builder.class)
@Schema(description = """
                      Configuration for an output format of XML.
                      There are no formatting options for XML output.
                      """)
public class FormatXml extends AbstractTextFormat implements Format {

  private final boolean xmlDeclaration;
  private final String encoding;
  private final boolean indent;
  private final boolean fieldsAsAttributes;
  private final String docName;
  private final String rowName;
  private final String fieldInitialLetterFix;
  private final String fieldInvalidLetterFix;
  private final ImmutableList<FormatXmlCharacterReference> characterReferences;
  private final Map<String, String> characterReferenceMap;

  private static final String NAME_START_REGEX_STR = "["
    + ":"
    + "A-Z"
    + "_"
    + "a-z"
    + "\\u00C0-\\u00D6"
    + "\\u00D8-\\u00F6"
    + "\\u00F8-\\u02ff"
    + "\\u0370-\\u037d"
    + "\\u037f-\\u1fff"
    + "\\u200c-\\u200d"
    + "\\u2070-\\u218f"
    + "\\u2c00-\\u2fef"
    + "\\u3001-\\ud7ff"
    + "\\uf900-\\ufdcf"
    + "\\ufdf0-\\ufffd"
    + "]";

  private static final String NAME_CHAR_REGEX_STR = "["
    + ":"
    + "A-Z"
    + "_"
    + "a-z"
    + "\\u00C0-\\u00D6"
    + "\\u00D8-\\u00F6"
    + "\\u00F8-\\u02ff"
    + "\\u0370-\\u037d"
    + "\\u037f-\\u1fff"
    + "\\u200c\\u200d"
    + "\\u2070-\\u218f"
    + "\\u2c00-\\u2fef"
    + "\\u3001-\\udfff"
    + "\\uf900-\\ufdcf"
    + "\\ufdf0-\\ufffd"
    + "\\-"
    + "\\."
    + "0-9"
    + "\\u00b7"
    + "\\u0300-\\u036f"
    + "\\u203f-\\u2040"
    + "]";

  /**
   * A compiled regular expression Pattern used to validate full names in an XML structure.
   * This pattern ensures that the entire string starts with a valid name-start character,
   * followed by other valid characters.
   */
  public static final Pattern NAME_START_REGEX = Pattern.compile("^" + NAME_START_REGEX_STR + NAME_CHAR_REGEX_STR + "*$");
  /**
   * A compiled regular expression Pattern used to validate names in an XML structure.
   * This pattern ensures that the entire string starts consists of valid following characters,
   */
  public static final Pattern NAME_CHAR_REGEX = Pattern.compile("^" + NAME_CHAR_REGEX_STR + "*$");

  /**
   * Constructor for FormatXml, using the Builder class for initialization.
   */
  private FormatXml(Builder builder) {
    super(builder);
    this.xmlDeclaration = builder.xmlDeclaration;
    this.encoding = builder.encoding;
    this.indent = builder.indent;
    this.fieldsAsAttributes = builder.fieldsAsAttributes;
    this.docName = builder.docName;
    this.rowName = builder.rowName;
    this.fieldInitialLetterFix = builder.fieldInitialLetterFix;
    this.fieldInvalidLetterFix = builder.fieldInvalidLetterFix;
    this.characterReferences = ImmutableCollectionTools.copy(builder.characterReferences);
    
    ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.<String, String>builder();
    this.characterReferences.forEach(cr -> {
      mapBuilder.put(cr.getReplace(), cr.getWith());
    });
    this.characterReferenceMap = mapBuilder.build();
  }

  /**
   * Construct a new instance of the FormatXml.Builder class.
   * @return a new instance of the FormatXml.Builder class.
   */
  public static FormatXml.Builder builder() {
    return new FormatXml.Builder();
  }


  @Override
  public FormatXmlInstance createInstance(Vertx vertx, RequestContext requestContext, WriteStream<Buffer> writeStream) {
    return new FormatXmlInstance(this, requestContext, writeStream);
  }

  @Override
  public void validate() {
    super.validate(FormatType.XML, "", "");
    if (fieldInitialLetterFix != null && !NAME_START_REGEX.matcher(fieldInitialLetterFix).matches()) {
      throw new IllegalArgumentException("The value '" + fieldInitialLetterFix + "' provided as fieldInitialLetterFix is not a valid name for XML node");
    }
    if (fieldInvalidLetterFix != null && !NAME_CHAR_REGEX.matcher(fieldInvalidLetterFix).matches()) {
      throw new IllegalArgumentException("The value '" + fieldInvalidLetterFix + "' provided as fieldInvalidLetterFix is not a valid character in the name of an XML node");
    }
    if (docName != null && !NAME_START_REGEX.matcher(docName).matches()) {
      throw new IllegalArgumentException("The value '" + docName + "' provided as docName is not a valid element name in an XML document");
    }
    if (rowName != null && !NAME_START_REGEX.matcher(rowName).matches()) {
      throw new IllegalArgumentException("The value '" + rowName + "' provided as rowName is not a valid element name in an XML document");
    }
    if (fieldsAsAttributes && !characterReferences.isEmpty()) {
      throw new IllegalArgumentException("XML output is set to output values as attributes, but replacement character reference have been set");
    }
    if (encoding != null) {
      try {
        Charset.forName(encoding);
      } catch (Throwable ex) {
        throw new IllegalArgumentException("The charset '" + encoding + "' is not recognised by this JVM");
      }
    }
  }

  /**
   * Get the name of the format, as will be used on query string parameters.
   * No two formats in a single pipeline should have the same name.
   *
   * @return the name of the format, as will be used on query string parameters.
   */
  @Override
  @Schema(defaultValue = "XML")
  public String getName() {
    return super.getName();
  }

  /**
   * Get the extension of the format.
   *
   * @return the file extension used for this format.
   */
  @Schema(defaultValue = "xml")
  @Override
  public String getExtension() {
    return super.getExtension();
  }

  /**
   * Get the media type of this format.
   *
   * @return the {@link MediaType}, which maps to Content-Type in HTTP headers.
   */
  @Schema(defaultValue = "application/xml; charset=utf-8")
  @Override
  public MediaType getMediaType() {
    return super.getMediaType();
  }

  /**
   * Check whether an XML declaration is included.
   *
   * @return true if an XML declaration should be included; false otherwise.
   */
  @Schema(description = "Whether the XML declaration should be included.",
    defaultValue = "true")
  public boolean isXmlDeclaration() {
    return xmlDeclaration;
  }

  /**
   * Get the encoding to be used for XML output.
   *
   * @return the character encoding to be used.
   */
  @Schema(description = "The character encoding (e.g., utf-8) for the XML.",
    maxLength = 100,
    defaultValue = "utf-8")
  public String getEncoding() {
    return encoding;
  }

  /**
   * Check whether the XML output should be indented.
   *
   * @return true if indentation is enabled; false otherwise.
   */
  @Schema(description = "Whether the XML output should include indentation.",
    defaultValue = "false")
  public boolean isIndent() {
    return indent;
  }

  /**
   * Check whether the fields should be written as attributes.
   *
   * @return true if fields should be written as attributes; false otherwise.
   */
  @Schema(description = "Whether to write fields as attributes in the XML.",
    defaultValue = "false")
  public boolean isFieldsAsAttributes() {
    return fieldsAsAttributes;
  }

  /**
   * Get the document name to be used for XML structure.
   *
   * @return the XML document name.
   */
  @Schema(description = "The root document name in the XML output.",
    maxLength = 100,
    defaultValue = "data")
  public String getDocName() {
    return docName;
  }

  /**
   * Get the name for each row in the XML document structure.
   *
   * @return the row name.
   */
  @Schema(description = "The row element name.",
    maxLength = 100,
    defaultValue = "row")
  public String getRowName() {
    return rowName;
  }

  /**
   * Get the field initial letter fix to adjust XML field names.
   *
   * @return the initial letter fix strategy.
   */
  @Schema(description = "Fix applied to the initial letter of a field's name.",
    maxLength = 100,
    defaultValue = "F"
  )
  public String getFieldInitialLetterFix() {
    return fieldInitialLetterFix;
  }

  /**
   * Get the fix strategy for invalid letters in XML field names.
   *
   * @return the fix strategy for invalid letters.
   */
  @Schema(description = "Fix applied to invalid letters in field names.",
    maxLength = 100,
    defaultValue = "_")
  public String getFieldInvalidLetterFix() {
    return fieldInvalidLetterFix;
  }

  /**
   * Get any character references that should be explicitly set in the output.
   * <P>
   * The XML output factory will produce correct XML for the encoding specified and it is not usually necessary to
   * specify any character references to replace.
   * <P>
   * This facility should only be used when there is a specific requirement to encode some characters in a given way.
   * <P>
   * This is NOT a generic search and replace facility, the "with" value must be a valid XML character reference (without the &amp; and ;).
   * <P>
   * Note that character references cannot be set in attributes, so it is invalid to use character references when fieldsAsAttributes is true.
   * 
   * @return any character references that should be explicitly set in the output.
   */
  @ArraySchema(
          arraySchema = @Schema(
                  description = """
                                Get any character references that should be explicitly set in the output.
                                <P>
                                The XML output factory will produce correct XML for the encoding specified and it is not usually necessary to
                                specify any character references to replace.
                                <P>
                                This facility should only be used when there is a specific requirement to encode some characters in a given way.
                                <P>
                                This is NOT a generic search and replace facility, the "with" value must be a valid XML character reference (without the &amp; and ;).
                                <P>
                                Note that character references cannot be set in attributes, so it is invalid to use character references when fieldsAsAttributes is true.
                                """
          )
          , minItems = 0
  )  
  public List<FormatXmlCharacterReference> getCharacterReferences() {
    return characterReferences;
  }
  
  /**
   * Get the replacement character references as a map.
   * @return the replacement character references as a map.
   */
  @JsonIgnore
  public Map<String, String> getCharacterReferenceMap() {
    return characterReferenceMap;
  }

  /**
   * Builder class for creating instances of the FormatXml class.
   *
   * The Builder pattern allows for the incremental construction and customization
   * of the FormatXml class by setting various properties related to the format,
   * such as format type, name, extension, media type, and additional attributes specific to XML formatting.
   */
  @JsonPOJOBuilder(withPrefix = "")
  public static class Builder extends AbstractTextFormat.Builder<Builder> {

    private boolean xmlDeclaration = true;
    private String encoding;
    private boolean indent = false;
    private boolean fieldsAsAttributes = false;
    private String docName;
    private String rowName;
    private String fieldInitialLetterFix = "F";
    private String fieldInvalidLetterFix = "_";
    private List<FormatXmlCharacterReference> characterReferences;

    /**
     * Default constructor.
     */
    public Builder() {
      super(FormatType.XML, "xml", null, "xml", null, MediaType.parse("application/xml; charset=utf-8"), false
              , null, null, null, null, null);
    }

    /**
     * Set whether an XML declaration should be included.
     *
     * @param xmlDeclaration true to include the declaration.
     * @return this Builder instance.
     */
    public Builder xmlDeclaration(boolean xmlDeclaration) {
      this.xmlDeclaration = xmlDeclaration;
      return this;
    }

    /**
     * Set the character encoding for XML output.
     *
     * @param encoding a valid character encoding (e.g., UTF-8).
     * @return this Builder instance.
     */
    public Builder encoding(String encoding) {
      this.encoding = encoding;
      return this;
    }

    /**
     * Set whether the XML output should be indented.
     *
     * @param indent true to enable indentation.
     * @return this Builder instance.
     */
    public Builder indent(boolean indent) {
      this.indent = indent;
      return this;
    }

    /**
     * Set whether fields should be written as attributes in the XML.
     *
     * @param fieldsAsAttributes true to treat fields as XML attributes.
     * @return this Builder instance.
     */
    public Builder fieldsAsAttributes(boolean fieldsAsAttributes) {
      this.fieldsAsAttributes = fieldsAsAttributes;
      return this;
    }

    /**
     * Set the document name for the XML structure.
     *
     * @param docName the root element's name in the XML.
     * @return this Builder instance.
     */
    public Builder docName(String docName) {
      this.docName = docName;
      return this;
    }

    /**
     * Set the name for each row in the XML document.
     *
     * @param rowName the row element's name.
     * @return this Builder instance.
     */
    public Builder rowName(String rowName) {
      this.rowName = rowName;
      return this;
    }

    /**
     * Set the strategy for fixing the initial letter of field names.
     *
     * @param fieldInitialLetterFix the initial letter fix strategy.
     * @return this Builder instance.
     */
    public Builder fieldInitialLetterFix(String fieldInitialLetterFix) {
      this.fieldInitialLetterFix = fieldInitialLetterFix;
      return this;
    }

    /**
     * Set the strategy for handling invalid letters in field names.
     *
     * @param fieldInvalidLetterFix the invalid letter fix strategy.
     * @return this Builder instance.
     */
    public Builder fieldInvalidLetterFix(String fieldInvalidLetterFix) {
      this.fieldInvalidLetterFix = fieldInvalidLetterFix;
      return this;
    }
    
    /**
     * Set explicit character references that should be written in the output.
     * @param characterReferences explicit character references that should be written in the output.
     * @return this Builder instance.
     */
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
    public Builder characterReferences(List<FormatXmlCharacterReference> characterReferences) {
      this.characterReferences = characterReferences;
      return this;
    }

    
    /**
     * Build an instance of {@link FormatXml}.
     *
     * @return a new FormatXml instance.
     */
    public FormatXml build() {
      return new FormatXml(this);
    }
  }
}
