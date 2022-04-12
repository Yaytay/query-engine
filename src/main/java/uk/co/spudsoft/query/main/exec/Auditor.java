/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import com.google.common.base.Strings;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.spudsoft.query.main.Audit;
import uk.co.spudsoft.query.main.DataSource;

/**
 *
 * @author jtalbut
 */
public class Auditor {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(Auditor.class);
  private static final String CHANGESET_RESOURCE_PATH = "/db/changelog/query-engine.yaml";
  
  private final Audit configuration;

  public Auditor(Audit audit) {
    this.configuration = audit;
  }
  
  public void prepare() throws Throwable {    
    DataSource dataSource = configuration.getDataSource();
    ResourceAccessor resourceAccessor = getBestResourceAccessor();
    Throwable lastException = null;
    if (!Strings.isNullOrEmpty(dataSource == null ? null : dataSource.getUrl())) {

      for (int retry = 0; configuration.getRetryLimit() < 0 || retry <= configuration.getRetryLimit(); ++retry) {
        logger.debug("Running liquibase, attempt {}", retry);
        try (Connection jdbcConnection = DriverManager.getConnection(dataSource.getUrl()
                , dataSource.getAdminUser() == null ? null : dataSource.getAdminUser().getUsername()
                , dataSource.getAdminUser() == null ? null : dataSource.getAdminUser().getPassword()
        )) {                  
          Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(jdbcConnection));
          if (!Strings.isNullOrEmpty(dataSource.getSchema())) {
            logger.trace("Setting default schema to {}", dataSource.getSchema());
            database.setDefaultSchemaName(dataSource.getSchema());
          }
          Liquibase liquibase = new Liquibase(
                  CHANGESET_RESOURCE_PATH,
                  resourceAccessor,
                  database
          );
          liquibase.update(new Contexts());
          logger.info("Database updated");
          return ;
        } catch (Throwable ex) {
          logger.error("Failed to update database: {}", ex.getMessage());
          lastException = ex;
          if (baseSqlExceptionIsNonexistantDriver(ex)) {
            throw ex;
          }
        }
        try {
          Thread.sleep(configuration.getRetryBaseMs() + retry * configuration.getRetryIncrementMs());
        } catch(InterruptedException ex) {
          logger.warn("Liquibase retry delay interrupted");
        }
      }
      throw lastException;      
    }
  }

  private ResourceAccessor getBestResourceAccessor() throws IOException {
    ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader());
    return resourceAccessor;
  }
  
  private boolean baseSqlExceptionIsNonexistantDriver(Throwable ex) {
    while (ex != null) {
      if (ex instanceof SQLException) {
        SQLException sqlex = (SQLException) ex;        
        String message = sqlex.getMessage();
        if (message != null) {
          if (message.startsWith("No suitable driver")) {
            return true;
          }
        }
      }
      ex = ex.getCause();
    }
    return false;
  }
  
  
}
