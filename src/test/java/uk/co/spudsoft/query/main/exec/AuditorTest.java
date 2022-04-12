/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.exec;

import org.junit.jupiter.api.Test;
import uk.co.spudsoft.query.main.Audit;
import uk.co.spudsoft.query.main.DataSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author jtalbut
 */
public class AuditorTest {

  @Test
  public void testBadDriver() {
    Auditor auditor = new Auditor(
            Audit.builder()
                    .dataSource(
                            DataSource.builder()
                                    .url("jdbc:nonexistant:wibble")
                                    .build()
                    )
                    .retryBaseMs(10000)
                    .retryIncrementMs(10000)
                    .build()
    );
    long start = System.currentTimeMillis();
    try {
      auditor.prepare();
      fail("Expected to throw");
    } catch(Throwable ex) {      
      assertThat(System.currentTimeMillis() - start, lessThan(1000L));
    }
  }

  @Test
  public void testBadUrl() {
    Auditor auditor = new Auditor(
            Audit.builder()
                    .dataSource(
                            DataSource.builder()
                                    .url("jdbc:postgresql://nonexistant/")
                                    .build()
                    )
                    .retryBaseMs(100)
                    .retryIncrementMs(10)
                    .retryLimit(10)
                    .build()
    );
    long start = System.currentTimeMillis();
    try {
      auditor.prepare();
      fail("Expected to throw");
    } catch(Throwable ex) {      
      assertThat(System.currentTimeMillis() - start, greaterThan(1000L));
    }
  }

}
