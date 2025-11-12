/*
 * Copyright (C) 2025 njt
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
package uk.co.spudsoft.query.exec.sources.jdbc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.microsoft.sqlserver.jdbc.SQLServerStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.defn.SourceJdbc;

/**
 *
 * @author jtalbut
 */
public class JdbcReadStreamTest {
  
  @Nested
  @DisplayName("report(long) tests")
  class ReportTests {

    @Test
    void whenLessThan10k_thenNeverReport() {
      assertFalse(JdbcReadStream.report(0));
      assertFalse(JdbcReadStream.report(1));
      assertFalse(JdbcReadStream.report(9_999));
    }

    @Test
    void whenBetween10kAnd1M_thenReportEvery10k() {
      assertTrue(JdbcReadStream.report(10_000));
      assertFalse(JdbcReadStream.report(10_001));
      assertTrue(JdbcReadStream.report(20_000));
      assertTrue(JdbcReadStream.report(990_000));
      assertFalse(JdbcReadStream.report(999_999));
    }

    @Test
    void whenBetween1MAnd10M_thenReportEvery100k() {
      assertTrue(JdbcReadStream.report(1_000_000));
      assertFalse(JdbcReadStream.report(1_050_000));
      assertTrue(JdbcReadStream.report(1_100_000));
      assertTrue(JdbcReadStream.report(9_900_000));
      assertFalse(JdbcReadStream.report(9_999_999));
    }

    @Test
    void when10MAndAbove_thenReportEvery1M() {
      assertTrue(JdbcReadStream.report(10_000_000));
      assertFalse(JdbcReadStream.report(10_500_000));
      assertTrue(JdbcReadStream.report(11_000_000));
      assertTrue(JdbcReadStream.report(100_000_000));
    }
  }

  @Nested
  @DisplayName("setFetchSize(...) tests")
  class SetFetchSizeTests {

    private SourceJdbc mockDef(int fetchSize, boolean replaceDoubleQuotes) {
      SourceJdbc def = mock(SourceJdbc.class);
      when(def.getJdbcFetchSize()).thenReturn(fetchSize);
      when(def.getReplaceDoubleQuotes()).thenReturn(replaceDoubleQuotes);
      when(def.getProcessingBatchSize()).thenReturn(128); // not used here, just safe default
      return def;
    }

    @Test
    void whenExplicitFetchSizeConfigured_thenUseIt() throws SQLException {
      SourceJdbc def = mockDef(5000, false);
      PreparedStatement ps = mock(PreparedStatement.class);

      JdbcReadStream.setFetchSize(def, "jdbc:postgresql://x", ps);

      verify(ps).setFetchSize(5000);
      verify(ps, never()).isWrapperFor(SQLServerStatement.class);
      verify(ps, never()).unwrap(SQLServerStatement.class);
    }

    @Test
    void whenNegativeFetchSizeAndMySql_thenUseStreamingMinValue() throws SQLException {
      SourceJdbc def = mockDef(-1, false);
      PreparedStatement ps = mock(PreparedStatement.class);

      JdbcReadStream.setFetchSize(def, "jdbc:mysql://localhost/db", ps);

      verify(ps).setFetchSize(Integer.MIN_VALUE);
      verify(ps, never()).isWrapperFor(SQLServerStatement.class);
      verify(ps, never()).unwrap(SQLServerStatement.class);
    }

    @Test
    void whenNegativeFetchSizeAndSqlServerWrapper_thenAdaptiveBuffering() throws SQLException {
      SourceJdbc def = mockDef(-1, false);
      PreparedStatement ps = mock(PreparedStatement.class);

      // Simulate wrapper behavior
      when(ps.isWrapperFor(SQLServerStatement.class)).thenReturn(true);
      SQLServerStatement sqlStmt = mock(SQLServerStatement.class);
      when(ps.unwrap(SQLServerStatement.class)).thenReturn(sqlStmt);

      JdbcReadStream.setFetchSize(def, "jdbc:sqlserver://host;databaseName=db", ps);

      verify(ps).isWrapperFor(SQLServerStatement.class);
      verify(ps).unwrap(SQLServerStatement.class);
      verify(sqlStmt).setResponseBuffering("adaptive");
      verify(ps, never()).setFetchSize(anyInt());
    }

    @Test
    void whenNegativeFetchSizeAndOtherDriver_thenDefault1000() throws SQLException {
      SourceJdbc def = mockDef(-1, false);
      PreparedStatement ps = mock(PreparedStatement.class);

      when(ps.isWrapperFor(SQLServerStatement.class)).thenReturn(false);

      JdbcReadStream.setFetchSize(def, "jdbc:postgresql://localhost/db", ps);

      verify(ps).isWrapperFor(SQLServerStatement.class);
      verify(ps).setFetchSize(1000);
      verify(ps, never()).unwrap(SQLServerStatement.class);
    }

    @Test
    void whenSqlServerWrapperThrows_thenPropagateSQLException() throws SQLException {
      SourceJdbc def = mockDef(-1, false);
      PreparedStatement ps = mock(PreparedStatement.class);

      when(ps.isWrapperFor(SQLServerStatement.class)).thenReturn(true);
      when(ps.unwrap(SQLServerStatement.class)).thenThrow(new SQLException("unwrap failed"));

      assertThrows(SQLException.class, () ->
          JdbcReadStream.setFetchSize(def, "jdbc:sqlserver://h", ps)
      );
    }
  }
}