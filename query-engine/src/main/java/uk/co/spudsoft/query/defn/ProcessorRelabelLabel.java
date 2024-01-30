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
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * @author jtalbut
 */
@JsonDeserialize(builder = ProcessorRelabelLabel.Builder.class)
@Schema(description = """
                      Processor that removes fields from the output.
                      """
)
public class ProcessorRelabelLabel {
  
  private final String sourceLabel;
  private final String newLabel;

  public void validate() {
    if (Strings.isNullOrEmpty(sourceLabel)) {
      throw new IllegalArgumentException("No source field name provided for relabel");
    }
    if (Strings.isNullOrEmpty(newLabel)) {
      throw new IllegalArgumentException("No new field name provided for relabel");
    }
  }
  
  public String getSourceLabel() {
    return sourceLabel;
  }

  public String getNewLabel() {
    return newLabel;
  }

  public static class Builder {

    private String sourceLabel;
    private String newLabel;

    private Builder() {
    }

    public Builder sourceLabel(final String value) {
      this.sourceLabel = value;
      return this;
    }

    public Builder newLabel(final String value) {
      this.newLabel = value;
      return this;
    }

    public ProcessorRelabelLabel build() {
      return new uk.co.spudsoft.query.defn.ProcessorRelabelLabel(sourceLabel, newLabel);
    }
  }

  public static ProcessorRelabelLabel.Builder builder() {
    return new ProcessorRelabelLabel.Builder();
  }

  private ProcessorRelabelLabel(final String sourceLabel, final String newLabel) {
    this.sourceLabel = sourceLabel;
    this.newLabel = newLabel;
  }
  
  
  
}
