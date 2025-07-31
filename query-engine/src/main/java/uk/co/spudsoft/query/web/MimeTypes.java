/*
 * Copyright (C) 2023 jtalbut
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
package uk.co.spudsoft.query.web;

import io.vertx.core.http.impl.MimeMapping;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for getting the MimeType of files.
 * Basically a wrapper around {@link io.vertx.core.http.impl.MimeMapping#getMimeTypeForExtension(java.lang.String)} with an override for some extensions.
 * @author jtalbut
 */
public class MimeTypes {
  
  private static final Map<String, String> OVERRIDE = new HashMap<>();

  /**
   * Overridden MIME types.
   */
  static {
    OVERRIDE.put("yaml", "application/yaml");
    OVERRIDE.put("yml", "application/yaml");
    OVERRIDE.put("webmanifest", "application/json");
    OVERRIDE.put("js", "application/javascript");
  };

  private MimeTypes() {
  }
  
  /**
   * Get the MimeType for an extension.
   * @param ext The file extension, excluding a leading dot.
   * @return the MimeType for an extension.
   */
  public static String getMimeTypeForExtension(String ext) {
    String result = OVERRIDE.get(ext);
    if (result == null) {
      result = MimeMapping.getMimeTypeForExtension(ext);
    }
    return result;
  }
  
  /**
   * Get the MimeType for a filename.
   * <p>
   * Extracts the extension and then called {@link #getMimeTypeForExtension(java.lang.String)}.
   * @param filename The filename.
   * @return the MimeType for a filename.
   */
  public static String getMimeTypeForFilename(String filename) {
    int li = filename.lastIndexOf('.');
    if (li != -1 && li != filename.length() - 1) {
      String ext = filename.substring(li + 1, filename.length());
      return getMimeTypeForExtension(ext);
    }
    return null;
  }
  
  
}
