/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 *
 * @author jtalbut
 */
public class MainTest {
  
  @Test
  public void testMainExitOnRun() {
    Main main = new Main();
    main.testMain(new String[]{
            "exitOnRun"
    });
  }
  
  @Test
  public void testMain() throws IOException {
    Main.main(new String[]{
    });
  }
  
}
