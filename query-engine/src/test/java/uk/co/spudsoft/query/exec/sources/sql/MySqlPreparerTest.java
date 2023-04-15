package uk.co.spudsoft.query.exec.sources.sql;

import uk.co.spudsoft.query.exec.sources.sql.MySqlPreparer;
import uk.co.spudsoft.query.exec.sources.sql.AbstractSqlPreparer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.defn.ArgumentType;
import uk.co.spudsoft.query.exec.ArgumentInstance;

/**
 *
 * @author njt
 */
public class MySqlPreparerTest {
    
  @Test
  public void testSingleProvidedSingleValuedParameter() {
    AbstractSqlPreparer instance = new MySqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of("id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7")));
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id", argSrc);
    assertEquals("select * from bob where id = ?", result.query);
    assertEquals(7L, result.args.get(0));
  }
  
  @Test
  public void testTwoProvidedSingleValuedParameters() {
    AbstractSqlPreparer instance = new MySqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
            , "name", new ArgumentInstance("name", Argument.builder().type(ArgumentType.String).build(), ImmutableList.of("fred"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name = :name", argSrc);
    assertEquals("select * from bob where id = ? and name = ?", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals("fred", result.args.get(1));
    assertEquals(2, result.args.size());
  }
  
  @Test
  public void testTwoProvidedParametersOneMultiValued() {
    AbstractSqlPreparer instance = new MySqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
            , "name", new ArgumentInstance("name", Argument.builder().type(ArgumentType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name in (:name)", argSrc);
    assertEquals("select * from bob where id = ? and name in (?, ?)", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals("fred", result.args.get(1));
    assertEquals("bob", result.args.get(2));
    assertEquals(3, result.args.size());
  }
   
  @Test
  public void testOneProvidedParameterOneNotProvided() {
    AbstractSqlPreparer instance = new MySqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name = :name", argSrc);
    assertEquals("select * from bob where id = ? and name = ?", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals(null, result.args.get(1));
    assertEquals(2, result.args.size());
  }

  @Test
  public void testTwoProvidedSingleValuedParametersOneReferencedTwice() {
    AbstractSqlPreparer instance = new MySqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
            , "name", new ArgumentInstance("name", Argument.builder().type(ArgumentType.String).build(), ImmutableList.of("fred"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name = :name or othername = :name", argSrc);
    assertEquals("select * from bob where id = ? and name = ? or othername = ?", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals("fred", result.args.get(1));
    assertEquals("fred", result.args.get(2));
    assertEquals(3, result.args.size());
  }

  @Test
  public void testOneSingleValuedOneMultiValuedParameterReferencedTwice() {
    AbstractSqlPreparer instance = new MySqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
            , "name", new ArgumentInstance("name", Argument.builder().type(ArgumentType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name in (:name) or othername in (:name)", argSrc);
    assertEquals("select * from bob where id = ? and name in (?, ?) or othername in (?, ?)", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals("fred", result.args.get(1));
    assertEquals("bob", result.args.get(2));
    assertEquals("fred", result.args.get(3));
    assertEquals("bob", result.args.get(4));
    assertEquals(5, result.args.size());
  }

  @Test
  public void testUnprovidedParameterReferencedTwice() {
    AbstractSqlPreparer instance = new MySqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name = :name or othername = :name", argSrc);
    assertEquals("select * from bob where id = ? and name = ? or othername = ?", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals(null, result.args.get(1));
    assertEquals(null, result.args.get(2));
    assertEquals(3, result.args.size());
  }

  @Test
  public void testBindTwoSingleValuedParameters() {
    AbstractSqlPreparer instance = new MySqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
            , "name", new ArgumentInstance("name", Argument.builder().type(ArgumentType.String).build(), ImmutableList.of("fred"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name = :name */", argSrc);
    assertEquals("select * from bob where id = ?  and name = ? ", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals("fred", result.args.get(1));
    assertEquals(2, result.args.size());
  }
  
  @Test
  public void testBindOneSingleValuedParameterOneMissingParameter() {
    AbstractSqlPreparer instance = new MySqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name = :name */", argSrc);
    assertEquals("select * from bob where id = ? ", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals(1, result.args.size());
  }
  
  @Test
  public void testBindOneSingleValuedOneMultiValuedParameter() {
    AbstractSqlPreparer instance = new MySqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
            , "name", new ArgumentInstance("name", Argument.builder().type(ArgumentType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name in (:name) */", argSrc);
    assertEquals("select * from bob where id = ?  and name in (?, ?) ", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals("fred", result.args.get(1));
    assertEquals("bob", result.args.get(2));
    assertEquals(3, result.args.size());
  }
  
  @Test
  public void testBindOneSingleValuedOneMultiValuedParameterReferencedTwice() {
    AbstractSqlPreparer instance = new MySqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
            , "name", new ArgumentInstance("name", Argument.builder().type(ArgumentType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name in (:name) */ or othername in (:name)", argSrc);
    assertEquals("select * from bob where id = ?  and name in (?, ?)  or othername in (?, ?)", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals("fred", result.args.get(1));
    assertEquals("bob", result.args.get(2));
    assertEquals("fred", result.args.get(3));
    assertEquals("bob", result.args.get(4));
    assertEquals(5, result.args.size());
  }
  
  @Test
  public void testBindOneSingleValuedOneMultiValuedParameterReferencedTwiceInBinds() {
    AbstractSqlPreparer instance = new MySqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
            , "name", new ArgumentInstance("name", Argument.builder().type(ArgumentType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name in (:name) *//*BIND or othername in (:name)*/", argSrc);
    assertEquals("select * from bob where id = ?  and name in (?, ?)  or othername in (?, ?)", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals("fred", result.args.get(1));
    assertEquals("bob", result.args.get(2));
    assertEquals("fred", result.args.get(3));
    assertEquals("bob", result.args.get(4));
    assertEquals(5, result.args.size());
  }
  
}
