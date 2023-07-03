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
 *
 * @author njt
 */
public class MimeTypes {
  
  private static final Map<String, String> OVERRIDE = new HashMap<>();

  static {
    OVERRIDE.put("yaml", "application/yaml");
    OVERRIDE.put("yml", "application/yaml");
    OVERRIDE.put("webmanifest", "application/json");
  };

  private MimeTypes() {
  }
  
  public static String getMimeTypeForExtension(String ext) {
    String result = OVERRIDE.get(ext);
    if (result == null) {
      result = MimeMapping.getMimeTypeForExtension(ext);
    }
    return result;
  }
  public static String getMimeTypeForFilename(String filename) {
    int li = filename.lastIndexOf('.');
    if (li != -1 && li != filename.length() - 1) {
      String ext = filename.substring(li + 1, filename.length());
      return MimeTypes.getMimeTypeForExtension(ext);
    }
    return null;
  }
  
  
}
