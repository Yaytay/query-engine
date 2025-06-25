/*
 * Copyright (C) 2025 jtalbut
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

import com.google.common.base.Strings;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnectOptions;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Abstract base class for sample data loaders with file-based locking to prevent
 * concurrent database preparation conflicts in parallel test execution.
 *
 * @author jtalbut
 */
public abstract class AbstractSampleDataLoader implements SampleDataLoader {

  private static final Logger logger = LoggerFactory.getLogger(AbstractSampleDataLoader.class);

  /**
   * Protected constructor.
   */
  protected AbstractSampleDataLoader() {
  }
  
  /**
   * Get the SQL script resource path used to generate test structures.
   * @return the SQL script resource path used to generate test structures.
   */
  protected abstract String getScript();
  
  /**
   * Opportunity for a SampleDataLoader to split the loaded string into multiple parts each executed in their own transaction.
   * 
   * If the script is not to be split this should just return a List with a single entry (which is the default behaviour).
   * 
   * @param loadedSql The entire loaded script.
   * @return The loaded script split into parts.
   */
  protected List<String> parseSql(String loadedSql) {
    return Arrays.asList(loadedSql);
  }
  
  @Override
  @SuppressFBWarnings("SQL_INJECTION_VERTX")
  public Future<Void> prepareTestDatabase(Vertx vertx, String url, String username, String password) {
    // Create a unique lock key based on the database connection details
    String lockKey = createLockKey(url, username);
    Path lockFile = getLockFilePath(lockKey);
    
    return vertx.executeBlocking(() -> {
      // Try to acquire the lock
      if (!acquireDatabaseLock(lockFile)) {
        // Another process is already preparing this database, wait for it
        waitForDatabasePreparation(lockFile);
      }
      return null;
    }).compose(v -> {
      try {
        // Prepare the database
        SqlConnectOptions connectOptions = SqlConnectOptions.fromUri(url);
        connectOptions.setUser(username);
        connectOptions.setPassword(password);
        Pool pool = Pool.pool(vertx, connectOptions, new PoolOptions().setMaxSize(3));
        
        String sql;
        try (InputStream strm = AbstractSampleDataLoader.class.getResourceAsStream(getScript())) {
          if (strm == null) {
            throw new RuntimeException("SQL script not found: " + getScript());
          }
          sql = new String(strm.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Throwable ex) {
          throw new RuntimeException("Failed to load SQL script: " + getScript(), ex);
        }
        
        List<String> sqlInChunks = parseSql(sql);

        return executeSql(pool, sqlInChunks.iterator())
                .andThen(ar -> {
                  releaseDatabaseLock(lockFile);
                });
        
      } catch (Exception ex) {
        logger.warn("Failed to prepare {} test database: ", getName(), ex);
        releaseDatabaseLock(lockFile);
        throw new RuntimeException(ex);
      }
    }).mapEmpty();
  }
  
  private String createLockKey(String url, String username) {
    // Create a unique key based on URL and username
    // Remove password and other sensitive info for the key
    String cleanUrl = url.replaceAll("password=[^&]*", "password=***");
    return DigestUtils.sha256Hex(cleanUrl + ":" + username);
  }
  
  private Path getLockFilePath(String lockKey) {
    Path targetDir = Paths.get("target", "database-locks");
    try {
      Files.createDirectories(targetDir);
    } catch (IOException e) {
      throw new RuntimeException("Failed to create lock directory", e);
    }
    return targetDir.resolve("db-prep-" + lockKey + ".lock");
  }
  
  private boolean acquireDatabaseLock(Path lockFile) {
    try {
      Files.createFile(lockFile);
      
      // Schedule cleanup
      lockFile.toFile().deleteOnExit();
      
      // Write process info for debugging
      String processInfo = ManagementFactory.getRuntimeMXBean().getName() + " at " + Instant.now().toString();
      Files.write(lockFile, processInfo.getBytes(StandardCharsets.UTF_8));
      
      logger.debug("Acquired database preparation lock: {}", lockFile);
      return true;
      
    } catch (FileAlreadyExistsException e) {
      logger.debug("Database preparation lock already exists: {}", lockFile);
      return false;
    } catch (IOException e) {
      throw new RuntimeException("Failed to acquire database lock", e);
    }
  }
  
  private Void waitForDatabasePreparation(Path lockFile) {
    logger.debug("Waiting for database preparation to complete: {}", lockFile);
    
    int maxWaitSeconds = 60; // Maximum wait time
    int checkIntervalMs = 500; // Check every 500ms
    int attempts = maxWaitSeconds * 1000 / checkIntervalMs;
    
    for (int i = 0; i < attempts; i++) {
      if (!Files.exists(lockFile)) {
        logger.debug("Database preparation completed: {}", lockFile);
        return null;
      }
      
      try {
        Thread.sleep(checkIntervalMs);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Interrupted while waiting for database preparation", e);
      }
    }
    
    // Check if lock file is stale (older than 2 minutes)
    try {
      if (Files.exists(lockFile)) {
        FileTime lockTime = Files.getLastModifiedTime(lockFile);
        if (Instant.now().minusSeconds(120).isAfter(lockTime.toInstant())) {
          logger.warn("Removing stale database lock file: {}", lockFile);
          Files.deleteIfExists(lockFile);
          return null;
        }
      }
    } catch (IOException e) {
      logger.warn("Failed to check lock file staleness: ", e);
    }
    
    throw new RuntimeException("Timeout waiting for database preparation to complete");
  }
  
  private void releaseDatabaseLock(Path lockFile) {
    try {
      Files.deleteIfExists(lockFile);
      logger.debug("Released database preparation lock: {}", lockFile);
    } catch (IOException e) {
      logger.warn("Failed to release database lock: ", e);
    }
  }
  
  @SuppressFBWarnings("SQL_INJECTION_VERTX")
  private Future<Void> executeSql(Pool pool, Iterator<String> iter) {    
    if (iter.hasNext()) {
      String stmt = iter.next().trim();      
      if (Strings.isNullOrEmpty(stmt)) {
        return executeSql(pool, iter);
      } else {
        return pool.<RowSet<Row>>withTransaction(conn -> executeSqlStmt(conn, stmt))
                .compose(rs -> executeSql(pool, iter));
      }
    } else {
      logger.info("{}: All SQL executed", getName());
      return Future.succeededFuture();
    }
  }
  
  @SuppressFBWarnings("SQL_INJECTION_VERTX")
  private Future<RowSet<Row>> executeSqlStmt(SqlConnection conn, String sql) {
    return conn.query(sql).execute();
  }
  
}
