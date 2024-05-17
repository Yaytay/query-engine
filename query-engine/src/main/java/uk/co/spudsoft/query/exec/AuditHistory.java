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
package uk.co.spudsoft.query.exec;

import com.google.common.collect.ImmutableList;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import uk.co.spudsoft.query.main.ImmutableCollectionTools;

/**
 * Information about requests made to the query engine by a single user.
 * @author jtalbut
 */
@Schema(
        description = """
                      Information about requests made to the query engine by a single user.
                      """
)
public class AuditHistory {
  
  private final long firstRow;  
  private final long totalRows;
  private final ImmutableList<AuditHistoryRow> rows;

  /**
   * Constructor.
   * 
   * @param firstRow The number (not ID) of the first row.
   * @param totalRows  The number of rows that the current user has.
   * @param rows The rows of data.
   */
  public AuditHistory(long firstRow, long totalRows, List<AuditHistoryRow> rows) {
    this.firstRow = firstRow;
    this.totalRows = totalRows;
    this.rows = ImmutableCollectionTools.copy(rows);
  }

  /**
   * Get the index of the first row (out of all those for the current user) present in this dataset.
   * <P>
   * This should equal the skipsRows argument passed in the request for history.
   * @return the index of the first row (out of all those for the current user) present in this dataset.
   */
  @Schema(
          description = """
                        <P>The index of the first row (out of all those for the current user) present in this dataset.</P>
                        <P>This should equal the skipsRows argument passed in the request for history.</P>
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public long getFirstRow() {
    return firstRow;
  }

  /**
   * Get the total number of history records that the current user has.
   * @return the total number of history records that the current user has.
   */
  @Schema(
          description = """
                        <P>The total number of history records that the current user has.</P>
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public long getTotalRows() {
    return totalRows;
  }

  /**
   * Get details of specific requests to the query engine for the current user.
   * <P>
   * The number of entries in this array should be no greater than the maxRows argument passed in the request for history.
   * @return details of specific requests to the query engine for the current user.
   */
  @Schema(
          description = """
                        <P>Details of specific requests to the query engine for the current user.</P>
                        <P>The number of entries in this array should be no greater than the maxRows argument passed in the request for history.</P>
                        """
          , requiredMode = Schema.RequiredMode.REQUIRED
  )
  public List<AuditHistoryRow> getRows() {
    return rows;
  }
  
}
