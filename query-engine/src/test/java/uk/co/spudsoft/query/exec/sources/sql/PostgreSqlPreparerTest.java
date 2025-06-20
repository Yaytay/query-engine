package uk.co.spudsoft.query.exec.sources.sql;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.exec.ArgumentInstance;

/**
 *
 * @author jtalbut
 */
public class PostgreSqlPreparerTest {

  private static final Logger logger = LoggerFactory.getLogger(PostgreSqlPreparerTest.class);

  @Test
  public void testSingleProvidedSingleValuedParameter() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of("id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L)));
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id", Boolean.FALSE, argSrc);
    assertEquals("select * from bob where id = $1", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals(1, result.args().size());
  }

  @Test
  public void testTwoProvidedSingleValuedParameters() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance( Argument.builder().type(DataType.Long).build(), ImmutableList.of(7L))
            , "name", new ArgumentInstance(Argument.builder().name("name").type(DataType.String).build(), ImmutableList.of("fred"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name = :name", Boolean.TRUE, argSrc);
    assertEquals("select * from bob where id = $1 and name = $2", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals("fred", result.args().get(1));
    assertEquals(2, result.args().size());
  }

  @Test
  public void testTwoProvidedParametersOneMultiValued() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
            , "name", new ArgumentInstance(Argument.builder().name("name").type(DataType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name in (:name)", Boolean.FALSE, argSrc);
    assertEquals("select * from bob where id = $1 and name in ($2, $3)", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals("fred", result.args().get(1));
    assertEquals("bob", result.args().get(2));
    assertEquals(3, result.args().size());
  }

  @Test
  public void testOneProvidedParameterOneNotProvided() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name = :name", Boolean.TRUE, argSrc);
    assertEquals("select * from bob where id = $1 and name = $2", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals(null, result.args().get(1));
    assertEquals(2, result.args().size());
  }

  @Test
  public void testTwoProvidedSingleValuedParametersOneReferencedTwice() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
            , "name", new ArgumentInstance(Argument.builder().name("name").type(DataType.String).build(), ImmutableList.of("fred"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name = :name or othername = :name", Boolean.FALSE, argSrc);
    assertEquals("select * from bob where id = $1 and name = $2 or othername = $2", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals("fred", result.args().get(1));
    assertEquals(2, result.args().size());
  }

  @Test
  public void testOneSingleValuedOneMultiValuedParameterReferencedTwice() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
            , "name", new ArgumentInstance(Argument.builder().name("name").type(DataType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name in (:name) or othername in (:name)", Boolean.TRUE, argSrc);
    assertEquals("select * from bob where id = $1 and name in ($2, $3) or othername in ($2, $3)", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals("fred", result.args().get(1));
    assertEquals("bob", result.args().get(2));
    assertEquals(3, result.args().size());
  }

  @Test
  public void testUnprovidedParameterReferencedTwice() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name = :name or othername = :name", Boolean.FALSE, argSrc);
    assertEquals("select * from bob where id = $1 and name = $2 or othername = $3", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals(null, result.args().get(1));
    assertEquals(null, result.args().get(2));
    assertEquals(3, result.args().size());
  }

  @Test
  public void testBindTwoSingleValuedParameters() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
            , "name", new ArgumentInstance(Argument.builder().name("name").type(DataType.String).build(), ImmutableList.of("fred"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name = :name */", Boolean.TRUE, argSrc);
    assertEquals("select * from bob where id = $1  and name = $2 ", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals("fred", result.args().get(1));
    assertEquals(2, result.args().size());
  }

  @Test
  public void testBindOneSingleValuedParameterOneMissingParameter() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name = :name */", Boolean.FALSE, argSrc);
    assertEquals("select * from bob where id = $1 ", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals(1, result.args().size());
  }

  @Test
  public void testBindOneSingleValuedOneMultiValuedParameter() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
            , "name", new ArgumentInstance(Argument.builder().name("name").type(DataType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name in (:name) */", Boolean.TRUE, argSrc);
    assertEquals("select * from bob where id = $1  and name in ($2, $3) ", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals("fred", result.args().get(1));
    assertEquals("bob", result.args().get(2));
    assertEquals(3, result.args().size());
  }

  @Test
  public void testBindOneSingleValuedOneMultiValuedParameterReferencedTwice() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
            , "name", new ArgumentInstance(Argument.builder().name("name").type(DataType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name in (:name) */ or othername in (:name)", Boolean.FALSE, argSrc);
    assertEquals("select * from bob where id = $1  and name in ($2, $3)  or othername in ($2, $3)", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals("fred", result.args().get(1));
    assertEquals("bob", result.args().get(2));
    assertEquals(3, result.args().size());
  }

  @Test
  public void testBindOneSingleValuedOneMultiValuedParameterReferencedTwiceInBinds() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
            , "name", new ArgumentInstance(Argument.builder().name("name").type(DataType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name in (:name) *//*BIND or othername in (:name)*/", Boolean.TRUE, argSrc);
    assertEquals("select * from bob where id = $1  and name in ($2, $3)  or othername in ($2, $3)", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals("fred", result.args().get(1));
    assertEquals("bob", result.args().get(2));
    assertEquals(3, result.args().size());
  }

  @Test
  public void testReplaceDoubleQuote() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
            , "name", new ArgumentInstance(Argument.builder().name("name").type(DataType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from \"bOb\" where id = :id /* BIND and name in (:name) *//*BIND or othername in (:name)*/", Boolean.TRUE, argSrc);
    assertEquals("select * from \"bOb\" where id = $1  and name in ($2, $3)  or othername in ($2, $3)", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals("fred", result.args().get(1));
    assertEquals("bob", result.args().get(2));
    assertEquals(3, result.args().size());
  }

  @Test
  public void testBindVariableTwiceInStmtNotProvided() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "MinCreatedDate", new ArgumentInstance(Argument.builder().name("MinCreatedDate").type(DataType.Date).build(), ImmutableList.of(LocalDate.parse("2022-01-01")))
    );

    String sql = """
                 SELECT
                   *
                 FROM
                   table
                 WHERE
                   1 = 1
                  /* BIND and table.date >= :MinCreatedDate */
                  /* BIND and table.date <= :MaxCreatedDate */
                  /* BIND and (table.name = :Name OR table.othername = :Name) */
                 """;

    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement(sql, Boolean.TRUE, argSrc);
    logger.debug("Result args: {}", result.args());
    assertEquals("""
                 SELECT
                   *
                 FROM
                   table
                 WHERE
                   1 = 1
                   and table.date >= $1
                 """.trim(), result.query().trim());
    assertEquals(1, result.args().size());
  }

  @Test
  public void testBindVariableTwiceInStmtProvided() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "MinCreatedDate", new ArgumentInstance(Argument.builder().name("MinCreatedDate").type(DataType.Date).build(), ImmutableList.of(LocalDate.parse("2022-01-01")))
            , "Name", new ArgumentInstance(Argument.builder().name("Name").type(DataType.String).build(), ImmutableList.of("bob"))
    );

    String sql = """
                 SELECT
                   *
                 FROM
                   table
                 WHERE
                   1 = 1
                  /* BIND and table.date >= :MinCreatedDate */
                  /* BIND and table.date <= :MaxCreatedDate */
                  /* BIND and (table.name = :Name OR table.othername = :Name) */
                 """;

    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement(sql, Boolean.TRUE, argSrc);
    logger.debug("Result args: {}", result.args());

    // The BIND handling removes the entire SQL comment, but not the line that it is on
    // Which means we have a blank line in the output.
    // But the line isn't completely blank - it starts with a space character, so we can't use a text block here
    // Also, there are space characters either side of the additional clauses, so each line ends with a space.
    String expected = "SELECT\n" +
                  "  *\n" +
                  "FROM\n" +
                  "  table\n" +
                  "WHERE\n" +
                  "  1 = 1\n" +
                  "  and table.date >= $1 \n" +
                  " \n" +  
                  "  and (table.name = $2 OR table.othername = $2) \n";

    String query = result.query();
    for (int i = 0; i < query.length(); ++i) {
      if (i < expected.length()) {
        logger.debug("{}: {} ({}) vs {} ({})", i, expected.charAt(i), (int) expected.charAt(i), query.charAt(i), (int) query.charAt(i));
        assertEquals(expected.charAt(i), query.charAt(i), "Character " + i + " does not match (" + (int) expected.charAt(i) + " vs " + (int) query.charAt(i) + ")" );
      } else {
        logger.debug("{}: {} ({})", i, query.charAt(i), (int) query.charAt(i));
      }
    }
    assertEquals(expected.length(), query.length());
    
    assertEquals(expected, query);
    assertEquals(2, result.args().size());
  }

}
