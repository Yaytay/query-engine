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
package uk.co.spudsoft.query.main.sample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Sample data load for MySQL.
 *
 * @author jtalbut
 */
public class SampleDataLoaderMySQL extends AbstractSampleDataLoader {

  /**
   * Constructor.
   */
  public SampleDataLoaderMySQL() {
  }

  @Override
  public String getName() {
    return "MySQL";
  }

  @Override
  protected String getScript() {
    return "/sampleData/MySQL Test Structures.sql";
  }

  @Override
  public String getIdentifierQuote() {
    return "`";
  }

  @Override
  protected List<String> parseSql(String loadedSql) {
    List<String> sqlList = new ArrayList<>();
    String delimiter = ";";
    int start = 0;
    Pattern delimPat = Pattern.compile("DELIMITER (\\S+)");
    Matcher matcher = delimPat.matcher(loadedSql);
    while (matcher.find()) {
      sqlList.addAll(Arrays.asList(loadedSql.substring(start, matcher.start()).split(delimiter)));
      delimiter = matcher.group(1);
      start = matcher.end() + 1;
    }    
    sqlList.addAll(Arrays.asList(loadedSql.substring(start).split(delimiter)));
    return sqlList;
  }
  
}
