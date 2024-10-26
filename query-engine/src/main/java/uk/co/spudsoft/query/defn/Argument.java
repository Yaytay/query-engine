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
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.regex.Pattern;
import static java.util.regex.Pattern.UNICODE_CHARACTER_CLASS;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * An Argument represents a named piece of data that will be passed in to a pipeline.
 * Typically these correspond to query string arguments.
 * 
 * @author jtalbut
 */
@JsonDeserialize(builder = Argument.Builder.class)
@Schema(description = """
                      <P>
                      An Argument represents a named piece of data that will be passed in to a pipeline.
                      </P>
                      <P>
                      Typically these correspond to query string arguments.
                      </P>
                      <P>
                      Every argument that a pipeline expects <em>should</em> be specified.
                      </P>
                      """)
public class Argument {
  
  /**
   * Regular expression for a valid argument name - a case sensitive string of alpha numeric characters.
   */
  public static final Pattern VALID_NAME = Pattern.compile("\\p{Alnum}+", UNICODE_CHARACTER_CLASS);
  
  private final DataType type;
  private final String name;
  private final String title;
  private final String prompt;
  private final String description;
  private final boolean optional;
  private final boolean hidden;
  private final boolean multiValued;
  private final boolean ignored;
  private final boolean validate;
  private final ImmutableList<String> dependsUpon; 
  private final String defaultValueExpression;
  private final String minimumValue;
  private final String maximumValue;
  private final ImmutableList<ArgumentValue> possibleValues;
  private final String possibleValuesUrl;
  private final String permittedValuesRegex;
  private final Condition condition;

  /**
   * Validate the provided definition.
   * @throws IllegalArgumentException If the argument definition is not valid.
   */
  public void validate() throws IllegalArgumentException {
    if (!VALID_NAME.matcher(name).matches()) {
      throw new IllegalArgumentException("The argument \"" + name + "\" does not have a valid name.");
    }
    if (type == DataType.Null) {
      throw new IllegalArgumentException("The argument \"" + name + "\" has a type of \"" + type + "\" which cannot be used.");
    }
    if (!Strings.isNullOrEmpty(permittedValuesRegex)) {
      try {
        Pattern.compile(permittedValuesRegex);
      } catch (Throwable ex) {
        throw new IllegalArgumentException("The argument \"" + name + "\" does not have a valid permittedValuesRegex." + ex.getMessage());
      }
    }
    if (!Strings.isNullOrEmpty(defaultValueExpression) && !optional && ! hidden && condition == null) {
      throw new IllegalArgumentException("The argument \"" + name + "\" has a default value specified, but is not optional, hidden or conditional.");
    }
    if (!Strings.isNullOrEmpty(minimumValue)) {
      try {
        type.cast(minimumValue);
      } catch (Exception ex) {
        throw new IllegalArgumentException("The argument \"" + name + "\" has a minimum value of \"" + minimumValue + "\" but that cannot be parsed as " + type + ".", ex);
      }
    }
    if (!Strings.isNullOrEmpty(maximumValue)) {
      try {
        type.cast(maximumValue);
      } catch (Exception ex) {
        throw new IllegalArgumentException("The argument \"" + name + "\" has a maximum value of \"" + maximumValue + "\" but that cannot be parsed as " + type + ".", ex);
      }
    }
    if (condition != null) {
      condition.validate();
    }
  }
  
  /**
   * Get the data type of the argument.
   * @return the data type of the argument.
   */
  @Schema(description = """
                        <P>The data type of the argument</P>
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public DataType getType() {
    return type;
  }

  /**
   * Return the name of the argument, as will be used in query string arguments and SQL parameters.
   * No two arguments in a single pipeline should have the same name.
   * @return the name of the argument, as will be used in query string arguments and SQL parameters. 
   */
  @Schema(description = """
                        <P>The name of the argument.</P>
                        <P>No two arguments in a single pipeline should have the same name.</P>
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
   * Return the title to be displayed for the argument in any UI.
   * The title serves no purpose in the processing of the pipeline.
   * If the title is not set the name should be used.
   * @return the title to be displayed for the argument in any UI.
   */
  @Schema(description = """
                        <P>The title to be displayed for the argument in any UI.</P>
                        <P>
                        The title serves no purpose in the processing of the pipeline.
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
   * Return the prompt to be displayed in any edit box for the argument prior to any entry by the user.
   * The prompt serves no purpose in the processing of the pipeline.
   * If the prompt is not set the name should be used.
   * @return the prompt to be displayed in any edit box for the argument prior to any entry by the user. 
   */
  @Schema(description = """
                        <P>The prompt to be displayed for the argument in any UI.</P>
                        <P>
                        The prompt serves no purpose in the processing of the pipeline.
                        </P>
                        <P>
                        The prompt should be kept short (a single word or phrase) so as to fit in the Input field on the UI parameter form.
                        </P>
                        """
          , maxLength = 100
          , type = "string"
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          )
  public String getPrompt() {
    return prompt;
  }

  /**
   * Return the description to use for the argument in any UI.
   * The description serves no purpose in the processing of the pipeline.
   * @return the description to use for the argument in any UI.
   */
  @Schema(description = """
                        <P>The description to be displayed for the argument in any UI.</P>
                        <P>
                        The description serves no purpose in the processing of the pipeline.
                        </P>
                        <P>
                        The description should be kept short (one short sentence) so as to fit above the Input field on the UI parameter form.
                        </P>
                        """
          , type = "string"
          , maxLength = 10000
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          )
  public String getDescription() {
    return description;
  }    

  /**
   * Return true if the argument is optional.
   * Optional arguments will be considered to have their default value, if the default value is not set they will be null.
   * @return true if the argument is optional.
   */
  @Schema(description = """
                        <P>If set to false the pipeline will fail if the argument is not supplied.</P>
                        <P>
                        Declaring mandatory arguments to not be optional results in a better user experience when
                        the users fail to provide it.
                        </P>
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , type = "boolean"
          , defaultValue = "false"          
          )
  public boolean isOptional() {
    return optional;
  }

  /**
   * Return true if the argument is hidden.
   * Hidden arguments will not be presented in the UI and cannot be set via query string parameters.
   * The purpose of hidden arguments is to use the defaultValueExpression to present dynamic data to the query.
   * @return true if the argument is hidden.
   */
  @Schema(description = """
                        <P>If set to true the pipeline UI will not show this argument and the pipeline will fail if the argument is supplied.</P>
                        <P>
                        The purpose of hidden arguments is to use the defaultValueExpression to present dynamic data to the query.
                        Suggested uses include dynamic limits on a query or values extracted from the token.
                        </P>
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , type = "boolean"
          , defaultValue = "false"          
          )
  public boolean isHidden() {
    return hidden;
  }

  /**
   * Return true if the argument supports having multiple values.
   * Multi valued arguments must be supported by "in" clauses in the SQL statement.
   * Multi valued argument instances may have multiple values, single valued argument instances must not have multiple values.
   * @return true if the argument supports having multiple values.
   */
  @Schema(description = """
                        <P>If set to true the argument may be provided multiple times.</P>
                        <P>
                        Multivalued arguments will be arrays when the Source sees them, for SQL they should usually be used
                        with an "IN" clause.
                        </P>
                        <P>
                        If an argument that is not multi-valued is provided multiple times the Query Engine will fail to run the Pipeline.
                        </P>
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , type = "boolean"
          , defaultValue = "false"
          )
  public boolean isMultiValued() {
    return multiValued;
  }

  
  /**
   * Return true if the argument will be ignored by the pipeline and may be excluded from those sent in.
   * Ignored arguments are intended to be used by the UI for derivative arguments and should not be used by the processing of the pipeline.
   * The processor will not complain if an ignored argument is passed in, but actually using one in the pipeline would be a very bad idea.
   * @return true if the argument will be ignored by the pipeline and may be excluded from those sent in.
   */
  @Schema(description = """
                        <P>If set to true the argument will be ignored by the pipeline and may be excluded from those sent in.</P>
                        <P>
                        Ignored arguments are intended to be used by the UI for derivative arguments and should not be used by the processing of the pipeline.
                        </P>
                        <P>
                        If an argument that should be ignored is provided the Query Engine will fail to run the Pipeline.
                        </P>
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , type = "boolean"
          , defaultValue = "false"
          )
  public boolean isIgnored() {
    return ignored;
  }  

  /**
   * Return true if the argument should be validated.
   * Validation can only check:
   * <ul>
   * <li>Enum values.
   * <li>Minimum and maximum values.
   * <li>Permitted values regex.
   * </ul>
   * @return true if the argument should be validated.
   */
  @Schema(description = """
                        <P>If set to true the argument will be validated.</P>
                        <P>
                        Validation can only check:
                        <ul>
                        <li>Possible values specified directly (not via possibleValuesUrl).
                        <li>Minimum and maximum values.
                        <li>Permitted values regex.
                        </ul>
                        </P>
                        <P>
                        Additionally, if the argument is not provided and takes on default values, these will only be checked
                        against the possible values list or the permitted values regexe if the default values generated by the expression
                        are Strings.
                        This does not prevent them being converted to the relevant data type after being generated and validated.
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , type = "boolean"
          , defaultValue = "true"
          )
  public boolean isValidate() {
    return validate;
  }

  /**
   * Return a list of the name(s) of another argument(s) that this argument requires.
   * 
   * This is intended to allow the UI to disable inputs until their dependent argument has been provided.
   * 
   * This serves no purpose in the processing of the pipeline (it is explicitly not validated at runtime).
   * @return a list of the name(s) of another argument(s) that this argument requires.
   */
  @ArraySchema(
          arraySchema = @Schema(
                  description = """
                          <P>
                          A list of the name(s) of another argument(s) that this argument requires.
                          </P>
                          <P>
                          This is intended to allow the UI to disable inputs until their dependent argument has been provided.
                          </P>
                          <P>
                          This serves no purpose in the processing of the pipeline (it is explicitly not validated at runtime).
                          </P>
                          """,
                   requiredMode = Schema.RequiredMode.NOT_REQUIRED
          )
          , schema = @Schema(
                  types = "string"
                  , maxLength = 100
          )
  )
  public List<String> getDependsUpon() {
    return dependsUpon;
  }

  /**
   * The default value for the argument, as a <a href="https://commons.apache.org/proper/commons-jexl/">JEXL</a> <a href="https://commons.apache.org/proper/commons-jexl/reference/syntax.html">expression</a>.
   * <P>
   * The expression will be evaluated in the same context as a {@link Condition} would be, but the result is not constrained to being a boolean value.
   * The evaluation may result in multiple values (as either an array or a Collection) or a single value.
   * Each value will be converted or parsed to the correct data type.
   * <P>
   * Numeric values can simple be entered on their own, but strings must be quoted (single or double) which does complicate things when working with YAML files (where quotes are optional).
   * When using the Design page of the Query Engine UI it is sufficient to enter a quoted string, but when editing a YAML file directly it will be necessary to quote the string twice (once for YAML and once for JEXL).
   * Both YAML and JEXL can recognise single and double quotes, JEXL treats them the same but YAML does not (see <a href="https://www.yaml.info/learn/quote.html">To Quote or not to Quote?</a>).
   * By default the recommended approach is to use single quotes for YAML and double quotes for JEXL.
   * <P>Note that the default value should <em>not</em> be URL encoded.
   * <P>
   * The default value can only be used if the argument is optional (or conditional, where it will be used if the condition is false).
   * 
   * @return the default value for the argument. 
   */
  @Schema(description = """
                        The default value for the argument, as a <a target="_blank" href="https://commons.apache.org/proper/commons-jexl/">JEXL</a> <a target="_blank" href="https://commons.apache.org/proper/commons-jexl/reference/syntax.html">expression</a>.
                        <P>
                        The expression will be evaluated in the same context as a <a target="_blank" href="/ui/help/Parameters/uk.co.spudsoft.query.defn.Condition.html">Condition</a> would be, but the result is not constrained to being a boolean value.
                        The evaluation may result in multiple values (as either an array or a Collection) or a single value.
                        Each value will be converted or parsed to the correct data type.
                        Be aware that multi-valued defaults don't work with formio.
                        <P>
                        Numeric values can simply be entered on their own, but strings must be quoted (single or double) which does complicate things when working with YAML files (where quotes are optional).
                        <P>
                        All output from a JEXL epression will be converted to the appropriate data type.
                        This can either be by parsing a string, or by converting a specific Java type.
                        <P>
                        When using the Design page of the Query Engine UI it is sufficient to enter a quoted string, but when editing a YAML file directly it will be necessary to quote the string twice (once for YAML and once for JEXL).
                        Both YAML and JEXL can recognise single and double quotes, JEXL treats them the same but YAML does not (see <a target="_blank" href="https://www.yaml.info/learn/quote.html">To Quote or not to Quote?</a>).
                        By default the recommended approach is to use single quotes for YAML and double quotes for JEXL.
                        <P>
                        Note that the default value should <em>not</em> be URL encoded.
                        <P>
                        The default value can only be used if the argument is optional (or conditional, where it will be used if the condition is false).
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , type = "string"
          , maxLength = 100
          )
  public String getDefaultValueExpression() {
    return defaultValueExpression;
  }

  /**
   * Return the minimum value for the argument.
   * @return the minimum value for the argument. 
   */
  @Schema(description = """
                        <P>The minimum value for the argument, as a string.</P>
                        <P>
                        The minimum value will be converted to the correct data type as it would be if it were received
                        as the argument.
                        Note that the minimum value should <em>not</em> be URL encoded.
                        </P>
                        <P>
                        If an argument is provided that is less than the minimum value (using an appropriate comparison for the
                        datatype, not a string comparison) then it will be refused.
                        </P>
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , type = "string"
          , maxLength = 100
          )
  public String getMinimumValue() {
    return minimumValue;
  }

  /**
   * Return the maximum value for the argument.
   * @return the maximum value for the argument. 
   */
  @Schema(description = """
                        <P>The maximum value for the argument, as a string.</P>
                        <P>
                        The maximum value will be converted to the correct data type as it would be if it were received
                        as the argument.
                        Note that the maximum value should <em>not</em> be URL encoded.
                        </P>
                        <P>
                        If an argument is provided that is greater than the mimum value (using an appropriate comparison for the
                        datatype, not a string comparison) then it will be refused.
                        </P>
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , type = "string"
          , maxLength = 100
          )
  public String getMaximumValue() {
    return maximumValue;
  }

  /**
   * Return a list of values that this Argument may be given.
   * This serves as a hint to the UI and is not validated by the Query Engine.
   * @return a list of values that this Argument may be given.
   */
  @ArraySchema(
          arraySchema = @Schema(
                  description = """
                        <P>A list of possible values that the argument may have.</P>
                        <P>
                        The possible values are not validated, if an invalid value is provided the pipeline will still
                        attempt to run with it.
                        </P>
                        <P>
                        If more than a few values are possible the possibleValuesUrl (or possibleValuesRegex) should be used instead.
                        </P>
                        """
                  , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          ),
          schema = @Schema(
                  implementation = ArgumentValue.class
                   , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          )
  )
  public List<ArgumentValue> getPossibleValues() {
    return possibleValues;
  }

  /**
   * Return a URL that may be called via a GET request to return a list of values that this Argument may be given.
   * 
   * The URL should be called using the same credentials as the Pipeline was (it is expected, but not required, that the URL will be another Pipeline).
   * The URL should return a JSON list that may contain either:
   * &lt;ul>
   * &lt;li>Strings.
   * &lt;li>Objects containing a value field.
   * &lt;li>Objects containing a value field and a label field.
   * &lt;/ul>
   * 
   * The PossibleValuesUrl overrides the PossibleValues, a UI may choose to display the PossibleValues whilst the request to the PossibleValuesUrl is in flight, but the
   * end result should be the values returned by the PossibleValuesUrl.
   * 
   * This serves as a hint to the UI and is not validated by the Query Engine.
   * 
   * @return a URL that may be called via a GET request to return a list of values that this Argument may be given.
   */
  @Schema(description = """
                        <P>A URL that will provide a list of possible values that the argument may have.</P>
                        <P>
                        The URL should be called using the same credentials as the Pipeline was (it is expected, but not required, that the URL will be another Pipeline).
                        </P><P>
                        If the URL is just a path the UI will appended it to the base URL that was used to access the query engine.
                        </P>
                        <P>
                        It is possible to reference other argument values in the query string of the URL.
                        If the query string contains a structure like [&name=:arg] (specifically is contains a match of the regular expression ) then the entire
                        block will be repeated for each value that the argument 'arg' has at the time of the call.
                        If the query string contains the simpler structure &name=:arg (without the brackets) then just the :arg value will be replaced.
                        </P>
                        <P>
                        The URL should return a JSON list that may contain either:
                        <ul>
                        <li>Strings.
                        <li>Objects containing a &quot;value&quot; field.
                        <li>Objects containing a &quot;value&quot; field and a &quot;label&quot; field.
                        </ul>

                        The PossibleValuesUrl overrides the PossibleValues, a UI may choose to display the PossibleValues whilst the request to the PossibleValuesUrl is in flight, but the
                        end result should be the values returned by the PossibleValuesUrl.
                        <P>
                        The possible values are not validated, if an invalid value is provided the pipeline will still
                        attempt to run with it.
                        </P>
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , type = "string"
          , maxLength = 1000
          )
  public String getPossibleValuesUrl() {
    return possibleValuesUrl;
  }

  /**
   * Return a regular expression that the argument value(s) must match.
   * 
   * This <em>is</em> validated at runtime and can be used to help with security.
   * It should also be validated by the UI to prevent the submission of invalid values.
   * 
   * @return a regular expression that the argument value(s) must match.
   */
  @Schema(description = """
                        <P>A regular expression that all values of the argument must match.</P>
                        <P>
                        All values passed in are validated and the Query Engine will fail to run if the values does not match the reguarl expression.
                        </P>
                        <P>
                        At runtime expression will be treated as a standard Java regular expression, but well written UI should also validate
                        values against the expression so Interactive Pipelines should only use expressions that are compatible with JavaScrtip.
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
          , type = "string"
          , maxLength = 200
          )
  public String getPermittedValuesRegex() {
    return permittedValuesRegex;
  }  
  
  /**
   * Optional condition that controls whether the argument will be used, and will appear as an argument in the form.
   * @return an optional condition that controls whether the argument will be used, and will appear as an argument in the form.
   */
  @Schema(description = """
                        <P>Optional condition that controls whether the argument will be used, and will appear as an argument in the form.</P>
                        <P>If the condition is not met the argument will not appear in the pipeline form and it will be as if the argument was not supplied on the command line (even if it is).</p>
                        """
          , requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  public Condition getCondition() {
    return condition;
  }
  

  /**
   * Builder class for Argument objects.
   */
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private DataType type = DataType.String;
    private String name;
    private String title;
    private String prompt;
    private String description;
    private boolean optional = false;
    private boolean hidden = false;
    private boolean multiValued = false;
    private boolean ignored = false;
    private boolean validate = true;
    private List<String> dependsUpon;
    private String defaultValueExpression;
    private String minimumValue;
    private String maximumValue;
    private List<ArgumentValue> possibleValues;
    private String possibleValuesUrl;
    private String permittedValuesRegex;
    private Condition condition;

    private Builder() {
    }

    /**
     * Set the data type of the Argument in the builder.
     * @param value the data type of the Argument.
     * @return this, so that the builder may be used fluently.
     */
    public Builder type(final DataType value) {
      this.type = value;
      return this;
    }

    /**
     * Set the name for the Argument in the builder.
     * @param value the name for the Argument.
     * @return this, so that the builder may be used fluently.
     */
    public Builder name(final String value) {
      this.name = value;
      return this;
    }

    /**
     * Set the title for the Argument in the builder.
     * @param value the title for the Argument.
     * @return this, so that the builder may be used fluently.
     */
    public Builder title(final String value) {
      this.title = value;
      return this;
    }

    /**
     * Set the title for the Argument in the builder.
     * @param value the title for the Argument.
     * @return this, so that the builder may be used fluently.
     */
    public Builder prompt(final String value) {
      this.prompt = value;
      return this;
    }

    /**
     * Set the title for the Argument in the builder.
     * @param value the title for the Argument.
     * @return this, so that the builder may be used fluently.
     */
    public Builder description(final String value) {
      this.description = value;
      return this;
    }

    /**
     * Set the optional flag of the Argument in the builder.
     * Optional arguments will be considered to have their default value, if the default value is not set they will be null.
     * @param value the optional flag of the Argument.
     * @return this, so that the builder may be used fluently.
     */
    public Builder optional(final boolean value) {
      this.optional = value;
      return this;
    }

    /**
     * Set the hidden flag of the Argument in the builder.
     * Hidden arguments will not be presented in the UI and cannot be set via query string parameters.
     * The purpose of hidden arguments is to use the defaultValueExpression to present dynamic data to the query.
     * @param value the hidden flag of the Argument.
     * @return this, so that the builder may be used fluently.
     */
    public Builder hidden(final boolean value) {
      this.hidden = value;
      return this;
    }

    /**
     * Set the multiValued flag of the Argument in the builder.
     * Multi valued arguments must be supported by "in" clauses in the SQL statement.
     * Multi valued argument instances may have multiple values, single valued argument instances must not have multiple values.
     * @param value the multiValued flag of the Argument.
     * @return this, so that the builder may be used fluently.
     */
    public Builder multiValued(final boolean value) {
      this.multiValued = value;
      return this;
    }
    
    /**
     * Set the ignored flag of the Argument in the builder.
     * @param value the ignored flag of the Argument.
     * @return this, so that the builder may be used fluently.
     */
    public Builder ignored(final boolean value) {
      this.ignored = value;
      return this;
    }

    /**
     * Set the validate flag of the Argument in the builder.
     * If validate is set to true enum values and limits on numeric arguments will be checked.
     * @param value the validate flag of the Argument.
     * @return this, so that the builder may be used fluently.
     */
    public Builder validate(final boolean value) {
      this.validate = value;
      return this;
    }

    /**
     * Set the list of arguments that this argument depends upon in the builder.
     * @param value the list of arguments that this argument depends upon.
     * @return this, so that the builder may be used fluently.
     */
    public Builder dependsUpon(final List<String> value) {
      this.dependsUpon = value;
      return this;
    }

    /**
     * Set the default value for the Argument in the builder.
     * @param value the default value for the Argument.
     * @return this, so that the builder may be used fluently.
     */
    public Builder defaultValueExpression(final String value) {
      this.defaultValueExpression = value;
      return this;
    }

    /**
     * Set the minimum value for the Argument in the builder.
     * @param value the minimum value for the Argument.
     * @return this, so that the builder may be used fluently.
     */
    public Builder minimumValue(final String value) {
      this.minimumValue = value;
      return this;
    }

    /**
     * Set the maximum value for the Argument in the builder.
     * @param value the maximum value for the Argument.
     * @return this, so that the builder may be used fluently.
     */
    public Builder maximumValue(final String value) {
      this.maximumValue = value;
      return this;
    }

    /**
     * Set the default value for the Argument in the builder.
     * @param value the default value for the Argument.
     * @return this, so that the builder may be used fluently.
     */
    public Builder possibleValues(final List<ArgumentValue> value) {
      this.possibleValues = value;
      return this;
    }

    /**
     * Set the default value for the Argument in the builder.
     * @param value the default value for the Argument.
     * @return this, so that the builder may be used fluently.
     */
    public Builder possibleValuesUrl(final String value) {
      this.possibleValuesUrl = value;
      return this;
    }
    
    
    /**
     * Set the permitted values regular expression for the argument.
     * @param value the permitted values regular expression for the argument.
     * @return this, so that the builder may be used fluently.
     */
    public Builder permittedValuesRegex(final String value) {
      this.permittedValuesRegex = value;
      return this;
    }
    
    /**
     * Set the {@link Argument#condition} value in the builder.
     * @param value The value for the {@link Argument#condition}.
     * @return this, so that this builder may be used in a fluent manner.
     */
    public Builder condition(final Condition value) {
      this.condition = value;
      return this;
    }

    /**
     * Construct a new Argument object.
     * @return a new Argument object.
     */
    public Argument build() {
      return new uk.co.spudsoft.query.defn.Argument(type, name, title, prompt, description, optional, hidden, multiValued, ignored, validate, dependsUpon, defaultValueExpression, minimumValue, maximumValue, possibleValues, possibleValuesUrl, permittedValuesRegex, condition);
    }
  }

  /**
   * Construct a new Builder object.
   * @return a new Builder object.
   */
  public static Argument.Builder builder() {
    return new Argument.Builder();
  }

  private Argument(DataType type, String name, String title, String prompt, String description
          , boolean optional, boolean hidden, boolean multiValued, boolean ignored, boolean validate
          , List<String> dependsUpon
          , String defaultValueExpression, String minimumValue, String maximumValue
          , List<ArgumentValue> possibleValues, String possibleValuesUrl, String permittedValuesRegex
          , Condition condition
  ) {
    this.type = type;
    this.name = name;
    this.title = title;
    this.prompt = prompt;
    this.description = description;
    this.optional = optional;
    this.hidden = hidden;
    this.multiValued = multiValued;
    this.ignored = ignored;
    this.validate = validate;
    this.dependsUpon = ImmutableCollectionTools.copy(dependsUpon);
    this.defaultValueExpression = defaultValueExpression;
    this.minimumValue = minimumValue;
    this.maximumValue = maximumValue;
    this.possibleValues = ImmutableCollectionTools.copy(possibleValues);
    this.possibleValuesUrl = possibleValuesUrl;
    this.permittedValuesRegex = permittedValuesRegex;
    this.condition = condition;
  }
  
}
