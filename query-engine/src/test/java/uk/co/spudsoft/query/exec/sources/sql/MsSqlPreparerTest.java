package uk.co.spudsoft.query.exec.sources.sql;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import inet.ipaddr.IPAddressString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.defn.Argument;
import uk.co.spudsoft.query.defn.DataType;
import uk.co.spudsoft.query.exec.ArgumentInstance;
import uk.co.spudsoft.query.exec.context.PipelineContext;
import uk.co.spudsoft.query.exec.context.RequestContext;

/**
 *
 * @author jtalbut
 */
public class MsSqlPreparerTest {
    
  @Test
  public void testSingleProvidedSingleValuedParameter() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    AbstractSqlPreparer instance = new MsSqlPreparer(pipelineContext);

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of("id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L)));
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id", Boolean.FALSE, argSrc);
    assertEquals("select * from bob where id = @p1", result.query());
    assertEquals(7L, result.args().get(0));
  }
  
  @Test
  public void testTwoProvidedSingleValuedParameters() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    AbstractSqlPreparer instance = new MsSqlPreparer(pipelineContext);

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
            , "name", new ArgumentInstance(Argument.builder().name("name").type(DataType.String).build(), ImmutableList.of("fred"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name = :name", Boolean.TRUE, argSrc);
    assertEquals("select * from bob where id = @p1 and name = @p2", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals("fred", result.args().get(1));
    assertEquals(2, result.args().size());
  }
  
  @Test
  public void testTwoProvidedParametersOneMultiValued() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    AbstractSqlPreparer instance = new MsSqlPreparer(pipelineContext);

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
            , "name", new ArgumentInstance(Argument.builder().name("name").type(DataType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name in (:name)", Boolean.FALSE, argSrc);
    assertEquals("select * from bob where id = @p1 and name in (@p2, @p3)", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals("fred", result.args().get(1));
    assertEquals("bob", result.args().get(2));
    assertEquals(3, result.args().size());
  }
   
  @Test
  public void testOneProvidedParameterOneNotProvided() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    AbstractSqlPreparer instance = new MsSqlPreparer(pipelineContext);

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name = :name", Boolean.TRUE, argSrc);
    assertEquals("select * from bob where id = @p1 and name = @p2", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals(null, result.args().get(1));
    assertEquals(2, result.args().size());
  }

  @Test
  public void testTwoProvidedSingleValuedParametersOneReferencedTwice() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    AbstractSqlPreparer instance = new MsSqlPreparer(pipelineContext);

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
            , "name", new ArgumentInstance(Argument.builder().name("name").type(DataType.String).build(), ImmutableList.of("fred"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name = :name or othername = :name", Boolean.FALSE, argSrc);
    assertEquals("select * from bob where id = @p1 and name = @p2 or othername = @p2", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals("fred", result.args().get(1));
    assertEquals(2, result.args().size());
  }

  @Test
  public void testOneSingleValuedOneMultiValuedParameterReferencedTwice() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    AbstractSqlPreparer instance = new MsSqlPreparer(pipelineContext);

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
            , "name", new ArgumentInstance(Argument.builder().name("name").type(DataType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name in (:name) or othername in (:name)", Boolean.TRUE, argSrc);
    assertEquals("select * from bob where id = @p1 and name in (@p2, @p3) or othername in (@p2, @p3)", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals("fred", result.args().get(1));
    assertEquals("bob", result.args().get(2));
    assertEquals(3, result.args().size());
  }

  @Test
  public void testUnprovidedParameterReferencedTwice() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    AbstractSqlPreparer instance = new MsSqlPreparer(pipelineContext);

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id and name = :name or othername = :name", Boolean.FALSE, argSrc);
    assertEquals("select * from bob where id = @p1 and name = @p2 or othername = @p3", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals(null, result.args().get(1));
    assertEquals(null, result.args().get(2));
    assertEquals(3, result.args().size());
  }

  @Test
  public void testBindTwoSingleValuedParameters() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    AbstractSqlPreparer instance = new MsSqlPreparer(pipelineContext);

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
            , "name", new ArgumentInstance(Argument.builder().name("name").type(DataType.String).build(), ImmutableList.of("fred"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name = :name */", Boolean.TRUE, argSrc);
    assertEquals("select * from bob where id = @p1  and name = @p2 ", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals("fred", result.args().get(1));
    assertEquals(2, result.args().size());
  }
  
  @Test
  public void testBindOneSingleValuedParameterOneMissingParameter() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    AbstractSqlPreparer instance = new MsSqlPreparer(pipelineContext);

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name = :name */", Boolean.FALSE, argSrc);
    assertEquals("select * from bob where id = @p1 ", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals(1, result.args().size());
  }
  
  @Test
  public void testBindOneSingleValuedOneMultiValuedParameter() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    AbstractSqlPreparer instance = new MsSqlPreparer(pipelineContext);

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
            , "name", new ArgumentInstance(Argument.builder().name("name").type(DataType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name in (:name) */", Boolean.TRUE, argSrc);
    assertEquals("select * from bob where id = @p1  and name in (@p2, @p3) ", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals("fred", result.args().get(1));
    assertEquals("bob", result.args().get(2));
    assertEquals(3, result.args().size());
  }
  
  @Test
  public void testBindOneSingleValuedOneMultiValuedParameterReferencedTwice() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    AbstractSqlPreparer instance = new MsSqlPreparer(pipelineContext);

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
            , "name", new ArgumentInstance(Argument.builder().name("name").type(DataType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name in (:name) */ or othername in (:name)", Boolean.FALSE, argSrc);
    assertEquals("select * from bob where id = @p1  and name in (@p2, @p3)  or othername in (@p2, @p3)", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals("fred", result.args().get(1));
    assertEquals("bob", result.args().get(2));
    assertEquals(3, result.args().size());
  }
  
  @Test
  public void testBindOneSingleValuedOneMultiValuedParameterReferencedTwiceInBinds() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    AbstractSqlPreparer instance = new MsSqlPreparer(pipelineContext);

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
            , "name", new ArgumentInstance(Argument.builder().name("name").type(DataType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from bob where id = :id /* BIND and name in (:name) *//*BIND or othername in (:name)*/", Boolean.TRUE, argSrc);
    assertEquals("select * from bob where id = @p1  and name in (@p2, @p3)  or othername in (@p2, @p3)", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals("fred", result.args().get(1));
    assertEquals("bob", result.args().get(2));
    assertEquals(3, result.args().size());
  }
    
  @Test
  public void testRepalceDoubleQuote() {
    RequestContext reqctx = new RequestContext(null, "id", "url", "host", "path", null, null, null, new IPAddressString("127.0.0.1"), null);
    PipelineContext pipelineContext = new PipelineContext("test", reqctx);
    
    AbstractSqlPreparer instance = new MsSqlPreparer(pipelineContext);

    ImmutableMap<String, ArgumentInstance> argSrc = ImmutableMap.of(
            "id", new ArgumentInstance(Argument.builder().name("id").type(DataType.Long).build(), ImmutableList.of(7L))
            , "name", new ArgumentInstance(Argument.builder().name("name").type(DataType.String).multiValued(true).build(), ImmutableList.of("fred", "bob"))
    );
    AbstractSqlPreparer.QueryAndArgs result = instance.prepareSqlStatement("select * from \"bOb\" where id = :id /* BIND and name in (:name) *//*BIND or othername in (:name)*/", Boolean.TRUE, argSrc);
    assertEquals("select * from \"bOb\" where id = @p1  and name in (@p2, @p3)  or othername in (@p2, @p3)", result.query());
    assertEquals(7L, result.args().get(0));
    assertEquals("fred", result.args().get(1));
    assertEquals("bob", result.args().get(2));
    assertEquals(3, result.args().size());
  }
  
}
