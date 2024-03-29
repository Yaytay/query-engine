package uk.co.spudsoft.query.exec.sources.sql;

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
public class PostgreSqlPreparerTest {
  
  @Test
  public void testSingleProvidedSingleValuedParameter() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of("id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7")));
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id", Boolean.FALSE, argSrc);
    assertEquals("select * from bob where id = $1", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals(1, result.args.size());
  }
  
  @Test
  public void testTwoProvidedSingleValuedParameters() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
            , "name", new ArgumentInstance("name", Argument.builder().type(ArgumentType.String).build(), ImmutableList.of("fred"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name = :name", Boolean.TRUE, argSrc);
    assertEquals("select * from bob where id = $1 and name = $2", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals("fred", result.args.get(1));
    assertEquals(2, result.args.size());
  }
  
  @Test
  public void testTwoProvidedParametersOneMultiValued() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
            , "name", new ArgumentInstance("name", Argument.builder().type(ArgumentType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name in (:name)", Boolean.FALSE, argSrc);
    assertEquals("select * from bob where id = $1 and name in ($2, $3)", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals("fred", result.args.get(1));
    assertEquals("bob", result.args.get(2));
    assertEquals(3, result.args.size());
  }
   
  @Test
  public void testOneProvidedParameterOneNotProvided() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name = :name", Boolean.TRUE, argSrc);
    assertEquals("select * from bob where id = $1 and name = $2", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals(null, result.args.get(1));
    assertEquals(2, result.args.size());
  }

  @Test
  public void testTwoProvidedSingleValuedParametersOneReferencedTwice() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
            , "name", new ArgumentInstance("name", Argument.builder().type(ArgumentType.String).build(), ImmutableList.of("fred"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name = :name or othername = :name", Boolean.FALSE, argSrc);
    assertEquals("select * from bob where id = $1 and name = $2 or othername = $2", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals("fred", result.args.get(1));
    assertEquals(2, result.args.size());
  }

  @Test
  public void testOneSingleValuedOneMultiValuedParameterReferencedTwice() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
            , "name", new ArgumentInstance("name", Argument.builder().type(ArgumentType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name in (:name) or othername in (:name)", Boolean.TRUE, argSrc);
    assertEquals("select * from bob where id = $1 and name in ($2, $3) or othername in ($2, $3)", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals("fred", result.args.get(1));
    assertEquals("bob", result.args.get(2));
    assertEquals(3, result.args.size());
  }

  @Test
  public void testUnprovidedParameterReferencedTwice() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name = :name or othername = :name", Boolean.FALSE, argSrc);
    assertEquals("select * from bob where id = $1 and name = $2 or othername = $3", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals(null, result.args.get(1));
    assertEquals(null, result.args.get(2));
    assertEquals(3, result.args.size());
  }

  @Test
  public void testBindTwoSingleValuedParameters() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
            , "name", new ArgumentInstance("name", Argument.builder().type(ArgumentType.String).build(), ImmutableList.of("fred"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name = :name */", Boolean.TRUE, argSrc);
    assertEquals("select * from bob where id = $1  and name = $2 ", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals("fred", result.args.get(1));
    assertEquals(2, result.args.size());
  }
  
  @Test
  public void testBindOneSingleValuedParameterOneMissingParameter() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name = :name */", Boolean.FALSE, argSrc);
    assertEquals("select * from bob where id = $1 ", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals(1, result.args.size());
  }
  
  @Test
  public void testBindOneSingleValuedOneMultiValuedParameter() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
            , "name", new ArgumentInstance("name", Argument.builder().type(ArgumentType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name in (:name) */", Boolean.TRUE, argSrc);
    assertEquals("select * from bob where id = $1  and name in ($2, $3) ", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals("fred", result.args.get(1));
    assertEquals("bob", result.args.get(2));
    assertEquals(3, result.args.size());
  }
  
  @Test
  public void testBindOneSingleValuedOneMultiValuedParameterReferencedTwice() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
            , "name", new ArgumentInstance("name", Argument.builder().type(ArgumentType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name in (:name) */ or othername in (:name)", Boolean.FALSE, argSrc);
    assertEquals("select * from bob where id = $1  and name in ($2, $3)  or othername in ($2, $3)", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals("fred", result.args.get(1));
    assertEquals("bob", result.args.get(2));
    assertEquals(3, result.args.size());
  }
  
  @Test
  public void testBindOneSingleValuedOneMultiValuedParameterReferencedTwiceInBinds() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
            , "name", new ArgumentInstance("name", Argument.builder().type(ArgumentType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name in (:name) *//*BIND or othername in (:name)*/", Boolean.TRUE, argSrc);
    assertEquals("select * from bob where id = $1  and name in ($2, $3)  or othername in ($2, $3)", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals("fred", result.args.get(1));
    assertEquals("bob", result.args.get(2));
    assertEquals(3, result.args.size());
  }
    
  @Test
  public void testRepalceDoubleQuote() {
    AbstractSqlPreparer instance = new PostgreSqlPreparer();

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance("id", Argument.builder().type(ArgumentType.Long).build(), ImmutableList.of("7"))
            , "name", new ArgumentInstance("name", Argument.builder().type(ArgumentType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from \"bOb\" where id = :id /* BIND and name in (:name) *//*BIND or othername in (:name)*/", Boolean.TRUE, argSrc);
    assertEquals("select * from \"bOb\" where id = $1  and name in ($2, $3)  or othername in ($2, $3)", result.query);
    assertEquals(7L, result.args.get(0));
    assertEquals("fred", result.args.get(1));
    assertEquals("bob", result.args.get(2));
    assertEquals(3, result.args.size());
  }
  
}
