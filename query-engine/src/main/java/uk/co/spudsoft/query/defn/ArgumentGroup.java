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
package uk.co.spudsoft.query.defn;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.regex.Pattern;
import static java.util.regex.Pattern.UNICODE_CHARACTER_CLASS;

/**
 * An ArgumentGroup represents a group of {@link Argument} instances.
 * 
 * Argument groups are entirely for the benefit of the user interface, they are not used in the processing of pipelines.
 * 
 * Each {@link Argument} in the {@link Pipeline} can specify a group name.
 * It is an error if the group does not also exist in the Pipeline.
 * 
 * @author jtalbut
 */
@JsonDeserialize(builder = ArgumentGroup.Builder.class)
@Schema(description = """
                      <P>
                      An ArgumentGroup represents a group of {@link Argument} instances.
                      </P>
                      <P>
                      Argument groups are entirely for the benefit of the user interface, they are not used in the processing of pipelines.
                      </P>
                      <P>
                      Each {@link Argument} in the {@link Pipeline} can specify a group name.
                      It is an error if the group does not also exist in the Pipeline.
                      </P>
                      """)
public class ArgumentGroup {
  
  /**
   * Regular expression for a valid argument name - a case sensitive string of alpha numeric characters.
   */
  public static final Pattern VALID_NAME = Pattern.compile("\\p{Alnum}+", UNICODE_CHARACTER_CLASS);
  
  private final String name;
  private final String title;
  private final String description;
  private final ArgumentGroupType type;
  private final String theme;

  /**
   * Validate the provided definition.
   * @throws IllegalArgumentException If the argument definition is not valid.
   */
  public void validate() throws IllegalArgumentException {
    if (name == null) {
      throw new IllegalArgumentException("An argument group has a null name.");
    } else if (name.isBlank()) {
      throw new IllegalArgumentException("An argument group has a blank name.");
    } else if (!VALID_NAME.matcher(name).matches()) {
      throw new IllegalArgumentException("The argument group \"" + name + "\" does not have a valid name.");
    }
  }
  
  /**
   * Return the name of the argument group, should be specified by any arguments in this group.
   * No two argument groups in a single pipeline should have the same name.
   * @return the name of the argument group, should be specified by any arguments in this group.
   */
  @Schema(description = """
                        <P>The name of the argument group, should be specified by any arguments in this group.</P>
                        <P>No two argument groups in a single pipeline should have the same name.</P>
                        <P>
                        The name must consist entirely of Unicode alpha-numeric characters, it is
                        recommended that the name use only ASCII characters to avoid needing to encode it
                        but this is not a requirement.
                        </P>
                        """
          , pattern = "\\p{Letter}[\\p{Letter}\\p{Number}]+"
          , type = "string"
          , minLength = 1
          , maxLength = 100
          , requiredMode = Schema.RequiredMode.REQUIRED
          , extensions = {@Extension(properties = {@ExtensionProperty(name="prompt", value="name")})}
  )
  public String getName() {
    return name;
  }

  /**
   * Return the title to be displayed for the argument group in any UI.
   * If the title is not set the name should be used.
   * @return the title to be displayed for the argument group in any UI.
   */
  @Schema(description = """
                        <P>The title to be displayed for the argument group in any UI.</P>
                        <P>
                        If the title is not set the UI will display the name.
                        </P>
                        """
          , maxLength = 100
          , type = "string"
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          )
  public String getTitle() {
    return title;
  }

  /**
   * Return the description to use for the argument group in any UI.
   * @return the description to use for the argument group in any UI.
   */
  @Schema(description = """
                        <P>The description to be displayed for the argument group in any UI.</P>
                        """
          , type = "string"
          , maxLength = 10000
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          )
  public String getDescription() {
    return description;
  }    

  
  /**
   * Return the type of rendering to use for the argument group.
   * <P>
   * The type of the argument group is optional, if it is not specified a default rendering will be used.
   * </P>
   * @return the type of rendering to use for the argument group.
   */
  @Schema(description = """
                        <P>The type of rendering to use for the argument group.</P>
                        <P>
                        The type of the argument group is optional, if it is not specified a default rendering will be used.
                        </P>
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          )
  public ArgumentGroupType getType() {
    return type;
  }
  
  /**
   * The theme used to display the argument group in the UI.
   * <P>
   * This should be any valid Bootstrap Panel Theme
   * <P>
   * Valid values include: default, primary, success, info, warning, danger.
   * 
   * @return the theme used to display the argument group in the UI.
   */
  @Schema(description = """
                        The theme used to display the argument group in the UI.
                        <P>
                        This should be any valid Bootstrap Panel Theme
                        <P>
                        Valid values include: default, primary, success, info, warning, danger.
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , type = "string"
          , maxLength = 100
          )
  public String getTheme() {
    return theme;
  }

  /**
   * Builder class for ArgumentGroup objects.
   */
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private String name;
    private String title;
    private String description;
    private ArgumentGroupType type;
    private String theme;

    private Builder() {
    }

    /**
     * Set the name for the ArgumentGroup in the builder.
     * @param value the name for the ArgumentGroup.
     * @return this, so that the builder may be used fluently.
     */
    public Builder name(final String value) {
      this.name = value;
      return this;
    }

    /**
     * Set the title for the ArgumentGroup in the builder.
     * @param value the title for the ArgumentGroup.
     * @return this, so that the builder may be used fluently.
     */
    public Builder title(final String value) {
      this.title = value;
      return this;
    }

    /**
     * Set the title for the ArgumentGroup in the builder.
     * @param value the title for the ArgumentGroup.
     * @return this, so that the builder may be used fluently.
     */
    public Builder description(final String value) {
      this.description = value;
      return this;
    }

    /**
     * Set the type of the ArgumentGroup in the builder.
     * @param value the type of the ArgumentGroup.
     * @return this, so that the builder may be used fluently.
     */
    public Builder type(final ArgumentGroupType value) {
      this.type = value;
      return this;
    }
    
    /**
     * Set the ignored flag of the ArgumentGroup in the builder.
     * @param value the ignored flag of the ArgumentGroup.
     * @return this, so that the builder may be used fluently.
     */
    public Builder theme(final String value) {
      this.theme = value;
      return this;
    }

    /**
     * Construct a new ArgumentGroup object.
     * @return a new ArgumentGroup object.
     */
    public ArgumentGroup build() {
      return new uk.co.spudsoft.query.defn.ArgumentGroup(name, title, description, type, theme);
    }
  }

  /**
   * Construct a new Builder object.
   * @return a new Builder object.
   */
  public static ArgumentGroup.Builder builder() {
    return new ArgumentGroup.Builder();
  }

  private ArgumentGroup(String name, String title, String description, ArgumentGroupType type, String theme) {
    this.name = name;
    this.title = title;
    this.description = description;
    this.type = type;
    this.theme = theme;
  }
  
}
