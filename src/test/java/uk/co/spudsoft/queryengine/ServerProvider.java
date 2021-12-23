/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.queryengine;

import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.MountableFile;

/**
 *
 * @author jtalbut
 */
public class ServerProvider {
  
  @SuppressWarnings("constantname")
  private static final Logger logger = LoggerFactory.getLogger(ServerProvider.class);

  public static final String MSSQL_IMAGE_NAME = "mcr.microsoft.com/mssql/server:2019-latest";
  public static final String MYSQL_IMAGE_NAME = "mysql:8.0";
  public static final String PGSQL_IMAGE_NAME = "postgres:14.1-alpine";
  
  private static final Object lock = new Object();
  private static Network network;
  private static GenericContainer mssqlserver;
  private static GenericContainer mysqlserver;
  private static GenericContainer pgsqlserver;
  
  public static final String MSSQL_PASSWORD = UUID.randomUUID().toString();
 
  public static Network getNetwork() {
    synchronized(lock) {
      if (network == null) {
        network = Network.newNetwork();        
      }
    }
    return network;
  }
  
  public static GenericContainer getMsSqlContainer() {
    synchronized(lock) {
      if (network == null) {
        network = Network.newNetwork();        
      }
      long start = System.currentTimeMillis();
      if (mssqlserver == null) {        
        mssqlserver = new GenericContainer(MSSQL_IMAGE_NAME)
                .withEnv("ACCEPT_EULA", "Y")
                .withEnv("SA_PASSWORD", MSSQL_PASSWORD)
                .withExposedPorts(1433)
                .withNetwork(network)
                ;
      }
      if (!mssqlserver.isRunning()) {
        mssqlserver.start();
        logger.info("Started test instance of Microsoft SQL Server with ports {} in {}s"
                , mssqlserver.getExposedPorts().stream().map(p -> Integer.toString((Integer) p) + ":" + Integer.toString(mssqlserver.getMappedPort((Integer) p))).collect(Collectors.toList())
                , (System.currentTimeMillis() - start) / 1000.0
        );
      }
    }
    return mssqlserver;
  }

  public static GenericContainer getMySqlContainer() {
    synchronized(lock) {
      if (network == null) {
        network = Network.newNetwork();        
      }
      long start = System.currentTimeMillis();
      if (mysqlserver == null) {
        mysqlserver = new GenericContainer(MYSQL_IMAGE_NAME)
                .withEnv("MYSQL_ROOT_PASSWORD", MSSQL_PASSWORD)
                .withExposedPorts(3306)
                .withNetwork(network)
                ;
      }
      if (!mysqlserver.isRunning()) {
        mysqlserver.start();
        logger.info("Started test instance of MySQL with ports {} in {}s"
                , mysqlserver.getExposedPorts().stream().map(p -> Integer.toString((Integer) p) + ":" + Integer.toString(mysqlserver.getMappedPort((Integer) p))).collect(Collectors.toList())
                , (System.currentTimeMillis() - start) / 1000.0
        );
      }
    }
    return mysqlserver;
  }

  public static GenericContainer getPgSqlContainer() {
    synchronized(lock) {
      if (network == null) {
        network = Network.newNetwork();        
      }
      long start = System.currentTimeMillis();
      if (pgsqlserver == null) {
        pgsqlserver = new GenericContainer(PGSQL_IMAGE_NAME)
                .withEnv("POSTGRES_PASSWORD", MSSQL_PASSWORD)
                .withExposedPorts(5432)
                .withNetwork(network)
                ;
      }
      if (!pgsqlserver.isRunning()) {
        pgsqlserver.start();
        logger.info("Started test instance of PostgreSQL with ports {} in {}s"
                , pgsqlserver.getExposedPorts().stream().map(p -> Integer.toString((Integer) p) + ":" + Integer.toString(pgsqlserver.getMappedPort((Integer) p))).collect(Collectors.toList())
                , (System.currentTimeMillis() - start) / 1000.0
        );
      }
    }
    return pgsqlserver;
  }

  
}
