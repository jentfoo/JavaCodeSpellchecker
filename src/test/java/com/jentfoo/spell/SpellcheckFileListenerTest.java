package com.jentfoo.spell;

import static org.junit.Assert.*;

import org.junit.Test;

public class SpellcheckFileListenerTest {
  @Test
  public void isDecelerationLinePrimitiveTrueTest() {
    assertTrue(SpellcheckFileListener.isDecelerationLine("short foo = 3;"));
    assertTrue(SpellcheckFileListener.isDecelerationLine("short foo=3;"));
    assertTrue(SpellcheckFileListener.isDecelerationLine("short foo=(short)3;"));
    assertTrue(SpellcheckFileListener.isDecelerationLine("int foo=bar;"));
    assertTrue(SpellcheckFileListener.isDecelerationLine("Double foo=bar;"));
    assertTrue(SpellcheckFileListener.isDecelerationLine("short foo;"));
  }
  
  @Test
  public void isDecelerationLinePrimitiveFalseTest() {
    assertFalse(SpellcheckFileListener.isDecelerationLine(""));
    assertFalse(SpellcheckFileListener.isDecelerationLine("(short)foo;"));
  }
  
  @Test
  public void isDecelerationLineObjectTrueTest() {
    assertTrue(SpellcheckFileListener.isDecelerationLine("FooBar foo;"));
    assertTrue(SpellcheckFileListener.isDecelerationLine("FooBar foo = null"));
  }
  
  @Test
  public void isDecelerationLineObjectFalseTest() {
    assertFalse(SpellcheckFileListener.isDecelerationLine("FooBar foo"));
    assertFalse(SpellcheckFileListener.isDecelerationLine("FooBar;"));
  }
}
