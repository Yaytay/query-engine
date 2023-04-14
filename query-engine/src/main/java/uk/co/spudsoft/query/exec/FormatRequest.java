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
package uk.co.spudsoft.query.exec;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Values parsed from the request relating to the format to be used.
 * 
 * The Format to use in a Pipeline is chosen using the following rules:
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
 * @author njt
 */
public class FormatRequest {

  private static final Logger logger = LoggerFactory.getLogger(FormatRequest.class);
  
  private String name;
  private String extension;
  private ImmutableList<MediaType> accept;

  public String getName() {
    return name;
  }

  public String getExtension() {
    return extension;
  }

  public List<MediaType> getAccept() {
    return accept;
  }

  public static List<MediaType> parseAcceptHeader(final String value) {
    List<MediaType> types = new ArrayList<>();
    String[] parts = value.split(",");
    for (String part : parts) {
      try {
        types.add(MediaType.parse(part.trim()));
      } catch (Throwable ex) {
        logger.trace("Failed to parse accept type \"{}\": ", part, ex);
      }
    }
    types.sort((MediaType o1, MediaType o2) -> compareMediaTypePriorities(o1, o2));
    return types;
  }
    
  @SuppressFBWarnings(value = {"EI_EXPOSE_REP2"}, justification = "Builder class should result in all instances being immutable when object is built")
  public static class Builder {

    private String name;
    private String extension;
    private List<MediaType> accept = Collections.emptyList();

    private Builder() {
    }

    public Builder name(final String value) {
      this.name = value;
      return this;
    }

    public Builder extension(final String value) {
      this.extension = value;
      return this;
    }

    public Builder accept(final List<MediaType> value) {
      this.accept = value;
      return this;
    }
    
    public Builder accept(final String value) {
      if (Strings.isNullOrEmpty(value)) {
        this.accept = Collections.emptyList();
        return this;
      }
      List<MediaType> types = parseAcceptHeader(value);
      this.accept = types;
      return this;
    }

    public FormatRequest build() {
      return new uk.co.spudsoft.query.exec.FormatRequest(name, extension, accept);
    }
  }

  public static FormatRequest.Builder builder() {
    return new FormatRequest.Builder();
  }

  private FormatRequest(final String name, final String extension, final List<MediaType> accept) {
    this.name = name;
    this.extension = extension;
    List<MediaType> sortableTypes = new ArrayList<>(accept);
    sortableTypes.sort((MediaType o1, MediaType o2) -> compareMediaTypePriorities(o1, o2));
    this.accept = ImmutableList.copyOf(sortableTypes);
  }

  private static final String MT_WILD = "*";
  
  private static double parseQ(String value) {
    try {
      return Double.parseDouble(value);
    } catch (Throwable ex) {
      logger.warn("Attempt to parse q value of \"{}\" failed: ", value, ex);
      return 0.0;
    }
  }
  
  /**
   * Comparator for MediaTypes based on priority and wildness.
   * Priority (q parameter) takes precedence, amongst types of equal q the least wild takes precedence.
   * Summary: return 1 if o2 is better than o1, return -1 is o1 is better than o2.
   * @param o1 The first MediaType to compare.
   * @param o2 The second MediaType to compare.
   * @return > 0 if o1 has a lower precedence than o2, &lt; 0 if o2 has a lower precedence than o1, and 0 otherwise. 
   */
  public static int compareMediaTypePriorities(MediaType o1, MediaType o2) {
    List<String> q1s = o1.parameters().get("q");
    List<String> q2s = o2.parameters().get("q");

    double q1 = q1s.isEmpty() ? 1.0 : parseQ(q1s.get(0));
    double q2 = q2s.isEmpty() ? 1.0 : parseQ(q2s.get(0));

    if (q1 > q2) {
      return -1;
    } else if (q2 > q1) {
      return 1;
    } 

    if (MT_WILD.equals(o1.type()) && !MT_WILD.equals(o2.type())) {
      return 1;
    } else if (MT_WILD.equals(o2.type()) && !MT_WILD.equals(o1.type())) {
      return -1;
    }

    if (MT_WILD.equals(o1.subtype()) && !MT_WILD.equals(o2.subtype())) {
      return 1;
    } else if (MT_WILD.equals(o2.subtype()) && !MT_WILD.equals(o1.subtype())) {
      return -1;
    }

    return 0;
  }
      
  
}
