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
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * The Pipeline is the fundamental unit of processing in QueryEngine.
 * A single Pipeline takes data from a single {@link Source}, passes it through any number of {@link Processor}s and finally delivers it to a {@link Format}.
 * The {@link Processor}s themselves may pull in data from other {@link Source}s.
 * @author jtalbut
 */
@JsonDeserialize(builder = Pipeline.Builder.class)
@Schema(description = """
                      <P>The Pipeline is the fundamental unit of processing in QueryEngine.</P>
                      <P>
                       A single Pipeline takes data from a single Source, passes it through any number of Processors and finally delivers it to a Format.
                       The Processors within a Pipeline may pull in data from other Sources.
                      </P>
                      <P>
                      A Source usually requires an Endpoint to tell it where to get the data from.
                      This separation allows the same query to be used against multiple databases (potentially dynamically defined).
                      </P>
                      <P>
                      A minimal Pipeline, therefore, must consist of at least a Source and a Format, and usually an Endpoint (unless using the Test Source).
                      </P>
                      <P>
                      Pipelines may be considered either Interactive or Non-Interactive.
                      The user of an Interactive Pipeline always runs the Pipeline via a form, and does not need to consider the actual URL being used at all.
                      A Non-Interactive Pipeline is either used programatically or by being configured in some client system (such as PowerBI).
                      A Non-Interactive Pipeline can be distinguished by the user having to know or construct the URL for it at some point.
                      The distinctino is irrelevant to the Query Engine itself, but can help when configuring Pipelines.
                      """
)
public class Pipeline extends SourcePipeline {
  
  private final String title;
  private final String description;
  private final Condition condition;
  private final RateLimitRule rateLimitRule;
  private final ConcurrencyRule concurrencyRule;
  private final ImmutableList<Argument> arguments;
  private final ImmutableMap<String, Endpoint> sourceEndpoints;
  private final ImmutableList<DynamicEndpoint> dynamicEndpoints;
  private final ImmutableList<Format> formats;  

  @Override
  public void validate() {
    super.validate();
    if (condition != null) {
      condition.validate();
    }
    if (rateLimitRule != null) {
      rateLimitRule.validate();
    }
    if (concurrencyRule != null) {
      concurrencyRule.validate();
    }
    if (formats.isEmpty()) {
      throw new IllegalArgumentException("No formats specified in pipeline");
    } else {
      Set<String> destNames = new HashSet<>();
      for (Format fmt : formats) {        
        if (!destNames.add(fmt.getName())) {
          throw new IllegalArgumentException("Two formats have the same name (" + fmt.getName() + ")");
        }
      }
      formats.forEach(Format::validate);
    }
    
    sourceEndpoints.forEach((k, v) -> v.validate());
    if (arguments != null && !arguments.isEmpty()) {
      Set<String> argNames = new HashSet<>();
      for (Argument arg : arguments) {
        if (!argNames.add(arg.getName())) {
          throw new IllegalArgumentException("Two arguments have the same name (" + arg.getName() + ")");
        }
      }

      for (Argument arg : arguments) {
        arg.validate();
        List<String> dependsUpon = arg.getDependsUpon();
        if (dependsUpon != null && !dependsUpon.isEmpty()) {
          for (String depend : dependsUpon) {
            if (!Strings.isNullOrEmpty(depend) && !argNames.contains(depend)) {
              throw new IllegalArgumentException("Argument " + arg.getName() + " depends upon argument " + depend + " but there is no such argument");
            }
          }
        }
      }
    }
  }
  
  @Schema(description = """
                        <P>
                        The title of the Pipeline that will be used in the UI in preference to the filename.
                        </P>
                        <P>
                        The title is optional, but should usually be provided, particularly for Interactive Pipelines.
                        </P>
                        """
          , maxLength = 100
  )
  public String getTitle() {
    return title;
  }

  @Schema(description = """
                        <P>
                        A description of the Pipeline that will be used in the UI to provide information to the user.
                        </P>
                        <P>
                        The description is optional, but should always be provided.
                        </P>
                        <P>
                        The description is optional should be kept relatively short as it will be included, in full, in the parameter gathering form for Interactive Pipelines.
                        </P>
                        """
          , implementation = String.class
  )
  public String getDescription() {
    return description;
  }

  @Schema(description = """
                        <P>
                        A condition that constrains who can use the Pipeline.
                        </P>
                        """
            , maxLength = 100
            , implementation = String.class
            , externalDocs = @ExternalDocumentation(description = "Conditions", url = "")
    )
  public Condition getCondition() {
    return condition;
  }

  @Schema(description = """
                        <P>
                        A rate limit rule constrains how frequently pipelines can be run.
                        </P>
                        """
            , implementation = RateLimitRule.class
    )
  public RateLimitRule getRateLimitRule() {
    return rateLimitRule;
  }    

  @Schema(description = """
                        <P>
                        A concurrency rule prevents the pipeline from being run concurrently.
                        </P>
                        """
            , implementation = RateLimitRule.class
    )
  public ConcurrencyRule getConcurrencyRule() {
    return concurrencyRule;
  }
    
  @ArraySchema(
          schema = @Schema(
                  implementation = Argument.class
                  , description = """
                          <P>Declared arguments to the Pipeline.</P>
                          <P>
                          Pipelines can receive arguments via the HTTP query string.
                          Any arguments may be provided and may be processed by the templates of the pipeline, even if they are not
                          declared here.
                          Declare all arguments here, otherwise no-one will know that they exist unless they read the pipeline definition.
                          </P>
                          <P>
                          The order in which Arguments are defined here is relevant as it affects the order in which they will be displayed for
                          Interactive Pipelines.
                          The order in which Arguments are provided in the query string is only relevant if an Argument can take multiple values (in which
                          case they will be presented to the query in the order that they appear in the query string, regardless of any other arguments appearing
                          between them).
                          </P>
                          """
          )
          , minItems = 0
          , uniqueItems = true
  )
  public List<Argument> getArguments() {
    return arguments;
  }

  @Schema(
          type = "object"
          , description = """
                          <P>
                          Endpoints are the actual providers of data to the Pipeline.
                          Most Sources (all except the TestSource) work through an Endpoint.
                          </P>
                          <P>
                          The segregation between Source and Endpoint allows a single Source to work with multiple Endpoints.
                          </P>
                          """
  )
  public Map<String, Endpoint> getSourceEndpoints() {
    return sourceEndpoints;
  }

  @ArraySchema(
          schema = @Schema(
                  implementation = DynamicEndpoint.class
                  , description = """
                                  <P>Sub-Pipelines that can be run prior to the main Pipeline in order to generate more SourceEndpoints.</P>
                                  <P>
                                  The expected use is for the source to query a database that contains connection strings (in vertx format, not JDBC format)
                                  based on information contained in the request (usually extracted from a JWT).
                                  In this way a single pipeline can support multiple databases based upon request content.
                                  </P>
                                  <P>
                                  Most of the properties of the DynamicEndpointSource have default values and any fields that do not exist in the
                                  results stream from the source pipeline will be silently ignored, so the DynamicEndpointSource usually requires minimal configuration.
                                  </P>
                                  <P>
                                  If generated endpoints have a condition they will be silently dropped unless the condition is met.
                                  All remaining endpoints generated by the DynamicEndpointSource will be added to the endpoints usable by the outer query in the order they are returned by the source.
                                  If endpoints do not have unique keys this does mean that later ones will overwrite earlier ones.
                                  </P>
                                  <P>
                                  The original endpoints that existed before the DynamicEndpointSource do not have special protection
                                  , if the DynamicEndpointSource generates endpoints with the same key as existing endpoints they will be overwritten.
                                  </P>
                                  """
          )
          , minItems = 0
          , uniqueItems = true
  )
  public List<DynamicEndpoint> getDynamicEndpoints() {
    return dynamicEndpoints;
  }
  
  @ArraySchema(
          schema = @Schema(
                  implementation = Format.class
                  , description = """
                                  <P>The output formats that this Pipeline supports.</P>
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
          )
          , minItems = 1
          , uniqueItems = true
  )
  public List<Format> getFormats() {
    return formats;
  }
  
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends SourcePipeline.Builder<Pipeline.Builder> {

    private String title;
    private String description;
    private Condition condition;
    private RateLimitRule rateLimitRule;
    private ConcurrencyRule concurrencyRule;
    private List<Argument> arguments;
    private Map<String, Endpoint> sourceEndpoints;
    private List<DynamicEndpoint> dynamicEndpoints;
    private List<Format> formats;

    private Builder() {
    }
    @Override
    public Pipeline build() {
      return new Pipeline(title, description, condition, rateLimitRule, concurrencyRule, arguments, sourceEndpoints, source, dynamicEndpoints, processors, formats);
    }

    public Builder source(final Source value) {
      this.source = value;
      return this;
    }

    /**
     * Set the condition on the Endpoint in the builder.
     * @param value the condition on the Endpoint.
     * @return this, so that the builder may be used fluently.
     */
    public Builder processors(final List<Processor> value) {
      this.processors = value;
      return this;
    }
    
    public Builder title(final String value) {
      this.title = value;
      return this;
    }

    public Builder description(final String value) {
      this.description = value;
      return this;
    }

    public Builder condition(final Condition value) {
      this.condition = value;
      return this;
    }

    public Builder rateLimitRule(final RateLimitRule value) {
      this.rateLimitRule = value;
      return this;
    }

    public Builder concurrencyRule(final ConcurrencyRule value) {
      this.concurrencyRule = value;
      return this;
    }

    public Builder arguments(final List<Argument> value) {
      this.arguments = value;
      return this;
    }

    public Builder sourceEndpoints(final Map<String, Endpoint> value) {
      this.sourceEndpoints = value;
      return this;
    }

    public Builder dynamicEndpoints(final List<DynamicEndpoint> value) {
      this.dynamicEndpoints = value;
      return this;
    }

    public Builder formats(final List<Format> value) {
      this.formats = value;
      return this;
    }
  }

  public static Pipeline.Builder builder() {
    return new Pipeline.Builder();
  }

  private Pipeline(String title, String description, Condition condition, RateLimitRule rateLimitRule, ConcurrencyRule concurrencyRule, List<Argument> arguments, Map<String, Endpoint> sourceEndpoints, Source source, List<DynamicEndpoint> dynamicEndpoints, List<Processor> processors, List<Format> formats) {
    super(source, processors);
    this.title = title;
    this.description = description;
    this.condition = condition;
    this.rateLimitRule = rateLimitRule;
    this.concurrencyRule = concurrencyRule;
    this.arguments = ImmutableCollectionTools.copy(arguments);
    Set<String> usedNames = new HashSet<>();
    for (Argument arg : this.arguments) {
      arg.validate();
      if (usedNames.contains(arg.getName())) {
        throw new IllegalArgumentException("Multiple arguments have the name \"" + arg.getName() + "\", names must be unique");
      }
      usedNames.add(arg.getName());
    }
    this.sourceEndpoints = ImmutableCollectionTools.copy(sourceEndpoints);
    this.dynamicEndpoints = ImmutableCollectionTools.copy(dynamicEndpoints);
    this.formats = ImmutableCollectionTools.copy(formats);
    usedNames.clear();
    for (Format dest : this.formats) {
      dest.validate();
      if (usedNames.contains(dest.getName())) {
        throw new IllegalArgumentException("Multiple arguments have the name \"" + dest.getName() + "\", names must be unique");
      }
      usedNames.add(dest.getName());
    }
  }  
  
}
