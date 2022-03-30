/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package uk.co.spudsoft.query.main.defn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author jtalbut
 */
public class ProcessorGroupConcatTest {
  
  @Test
  public void testGetType() {
    ProcessorGroupConcat instance = ProcessorGroupConcat.builder().build();
    assertEquals(ProcessorType.GROUP_CONCAT, instance.getType());
  }

  @Test
  public void testSetType() {
    ProcessorGroupConcat instance = ProcessorGroupConcat.builder().type(ProcessorType.GROUP_CONCAT).build();
    assertEquals(ProcessorType.GROUP_CONCAT, instance.getType());
    try {
      ProcessorGroupConcat.builder().type(ProcessorType.LIMIT).build();
      fail("Expected IllegalArgumentException");
    } catch(IllegalArgumentException ex) {
    }
  }

  @Test
  public void testIsInnerJoin() {
    ProcessorGroupConcat instance = ProcessorGroupConcat.builder().innerJoin(true).build();
    assertEquals(true, instance.isInnerJoin());
  }

  @Test
  public void testGetParentIdColumn() {
    ProcessorGroupConcat instance = ProcessorGroupConcat.builder().parentIdColumn("parentId").build();
    assertEquals("parentId", instance.getParentIdColumn());
  }

  @Test
  public void testGetChildIdColumn() {
    ProcessorGroupConcat instance = ProcessorGroupConcat.builder().childIdColumn("childId").build();
    assertEquals("childId", instance.getChildIdColumn());
  }

  @Test
  public void testGetChildValueColumn() {
    ProcessorGroupConcat instance = ProcessorGroupConcat.builder().childValueColumn("childValue").build();
    assertEquals("childValue", instance.getChildValueColumn());
  }

  @Test
  public void testGetParentValueColumn() {
    ProcessorGroupConcat instance = ProcessorGroupConcat.builder().parentValueColumn("parentValue").build();
    assertEquals("parentValue", instance.getParentValueColumn());
  }

  @Test
  public void testGetDelimiter() {
    ProcessorGroupConcat instance = ProcessorGroupConcat.builder().delimiter("delimiter").build();
    assertEquals("delimiter", instance.getDelimiter());
  }

}
