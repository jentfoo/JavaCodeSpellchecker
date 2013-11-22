package com.jentfoo.spell;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class SpellcheckFileListenerTest {
  @Test
  public void breakApartLineTest() {
    List<String> result;
    
    String firstStr = "int foo = 3;";
    String testStr = firstStr;
    result = SpellcheckFileListener.breakApartLine(testStr);
    assertEquals(1, result.size());
    assertEquals(firstStr, result.get(0));
    
    String secondStr = "short bar = 10;";
    testStr += secondStr;
    result = SpellcheckFileListener.breakApartLine(testStr);
    assertEquals(2, result.size());
    assertEquals(firstStr, result.get(0));
    assertEquals(secondStr, result.get(1));
  }
  
  @Test
  public void isDecelerationLinePrimitiveTrueTest() {
    assertTrue(SpellcheckFileListener.isPrimitiveDecelerationLine("short foo = 3;"));
    assertTrue(SpellcheckFileListener.isPrimitiveDecelerationLine("short foo=3;"));
    assertTrue(SpellcheckFileListener.isPrimitiveDecelerationLine("short foo=(short)3;"));
    assertTrue(SpellcheckFileListener.isPrimitiveDecelerationLine("int foo=bar;"));
    assertTrue(SpellcheckFileListener.isPrimitiveDecelerationLine("Double foo=bar;"));
    assertTrue(SpellcheckFileListener.isPrimitiveDecelerationLine("short foo;"));
  }
  
  @Test
  public void isDecelerationLinePrimitiveFalseTest() {
    assertFalse(SpellcheckFileListener.isPrimitiveDecelerationLine(""));
    assertFalse(SpellcheckFileListener.isPrimitiveDecelerationLine("(short)foo;"));
  }
  
  @Test
  public void isDecelerationLineObjectTrueTest() {
    assertTrue(SpellcheckFileListener.isObjectDecelerationLine("Map<String, Integer> foo;"));
    assertTrue(SpellcheckFileListener.isObjectDecelerationLine("FooBar foo;"));
    assertTrue(SpellcheckFileListener.isObjectDecelerationLine("FooBar foo = null"));
  }
  
  @Test
  public void isDecelerationLineObjectFalseTest() {
    assertFalse(SpellcheckFileListener.isObjectDecelerationLine("FooBar foo"));
    assertFalse(SpellcheckFileListener.isObjectDecelerationLine("FooBar;"));
  }
  
  @Test
  public void getVariableTest() {
    // primitives
    assertEquals("foo", SpellcheckFileListener.getVariable("short foo = 3;"));
    assertEquals("foo", SpellcheckFileListener.getVariable("short foo=3;"));
    assertEquals("foo", SpellcheckFileListener.getVariable("short foo=(short)3;"));
    assertEquals("foo", SpellcheckFileListener.getVariable("int foo=bar;"));
    assertEquals("foo", SpellcheckFileListener.getVariable("Double foo=bar;"));
    assertEquals("foo", SpellcheckFileListener.getVariable("short foo;"));
    assertEquals("foo", SpellcheckFileListener.getVariable("short foo,"));
    // objects
    assertEquals("foo", SpellcheckFileListener.getVariable("FooBar foo;"));
    assertEquals("foo", SpellcheckFileListener.getVariable("FooBar foo = null"));
    // functions
    assertEquals("testFunction", SpellcheckFileListener.getVariable("public void testFunction"));
  }
}
