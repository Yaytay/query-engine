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

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Field that an audit request can be sorted by.
 * @author jtalbut
 */
@Schema(
        description = """
                      Field that an audit request can be sorted by.
                      """
)
public enum AuditHistorySortOrder {
  
  /**
   * Sort the audit history by {@link AuditHistoryRow#timestamp}.
   */
  timestamp,
  /**
   * Sort the audit history by {@link AuditHistoryRow#id}.
   */
  id,
  /**
   * Sort the audit history by {@link AuditHistoryRow#path}.
   */
  path,
  /**
   * Sort the audit history by {@link AuditHistoryRow#host}.
   */
  host,
  /**
   * Sort the audit history by {@link AuditHistoryRow#issuer}.
   */
  issuer,
  /**
   * Sort the audit history by {@link AuditHistoryRow#subject}.
   */
  subject,
  /**
   * Sort the audit history by {@link AuditHistoryRow#username}.
   */
  username,
  /**
   * Sort the audit history by {@link AuditHistoryRow#name}.
   */
  name,
  /**
   * Sort the audit history by {@link AuditHistoryRow#responseCode}.
   */
  responseCode,
  /**
   * Sort the audit history by {@link AuditHistoryRow#responseRows}.
   */
  responseRows,
  /**
   * Sort the audit history by {@link AuditHistoryRow#responseSize}.
   */
  responseSize,
  /**
   * Sort the audit history by {@link AuditHistoryRow#responseStreamStart}.
   */
  responseStreamStart,
  /**
   * Sort the audit history by {@link AuditHistoryRow#responseDuration}.
   */
  responseDuration,
}
