/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package uk.co.spudsoft.query.main.testcontainers;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jtalbut
 */
public class OrdinalNames {
  
  private static final Logger logger = LoggerFactory.getLogger(OrdinalNames.class);
  
  private static final String[] ZEROTONINE = {
    null, "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"
  };
  private static final String[] ZEROTONINETEENTH = {
    null, "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "nineth", "tenth"
          , "eleventh", "twelfth", "thirteenth", "fourteenth", "fifteenth", "sixteenth", "seventeenth", "eighteenth", "nineteenth"     
  };
  private static final String[] TENTIES = {
    null, null, "twenty", "thirty", "fourty", "fifty", "sixty", "seventy", "eighty", "ninety"     
  };
  
  public static String nameForNumber(int i) {
    StringBuilder result = new StringBuilder();
    if (i == 0) {
      return "zeroth";
    }
    if (i < 0) {
      result.append("minus ");
      i = 0 - i;
    }
    if (i >= 1000) {
      result.append(ZEROTONINE[i / 1000]);
      result.append(" thousand");
      if (i % 1000 == 0) {
        result.append("th");
        return result.toString();
      }
      i = i - ((i / 1000) * 1000);
      result.append(i < 100 ? " and ": " ");
    }
    if (i >= 100) {
      result.append(ZEROTONINE[i / 100]);
      result.append(" hundred");
      if (i % 100 == 0) {
        result.append("th");
        return result.toString();
      }
      result.append(" and ");
      i = i - ((i / 100) * 100);
    }
    if (i >= 20) {
      int tensOffset = i / 10;
      int tens = tensOffset * 10;
      i = i - tens;
      if (i == 0) {
        result.append(TENTIES[tensOffset].replace("y", "ieth"));
      } else {
        result.append(TENTIES[tensOffset]);
        result.append(" ");
        result.append(ZEROTONINETEENTH[i % 20]);      
      }
    } else {
      result.append(ZEROTONINETEENTH[i % 20]);
    }  
    return result.toString();
  }
  
  @Test
  public void test() {
    for (int i = -10; i < 10000; i += (i < 0 ? 1 : Math.max(1, i/10))) {
      logger.debug("{}: {}", i, nameForNumber(i));
    }
  }
  
}
