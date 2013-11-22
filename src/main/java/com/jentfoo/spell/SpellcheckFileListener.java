package com.jentfoo.spell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.threadly.util.ExceptionUtils;

import com.jentfoo.file.FileListenerInterface;

public class SpellcheckFileListener implements FileListenerInterface {
  private static final String JAVA_EXTENSION = ".java";
  private static final String EMPTY_STR = "";
  
  private final AtomicInteger examinedFiles = new AtomicInteger(0);

  @Override
  public void handleFile(File file) {
    if (! file.getName().endsWith(JAVA_EXTENSION)) {
      // ignore none-java files
      return;
    }
    
    examinedFiles.incrementAndGet();
    
    try {
      spellCheckJavaFile(file);
    } catch (Exception e) {
      throw ExceptionUtils.makeRuntime(e);
    }
  }
  
  protected void spellCheckJavaFile(File file) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(file));
    try {
      int lineCount = 0;
      String currentJavaLine = EMPTY_STR;
      String line;
      while ((line = br.readLine()) != null) {
        lineCount++;
        
        currentJavaLine += line;
        if (isEndOfJavaLine(currentJavaLine)) {
          currentJavaLine = minimizeJavaLine(currentJavaLine);
          if (isDecelerationLine(currentJavaLine)) {
            analyzeSpelling(currentJavaLine);
          }
          
          currentJavaLine = EMPTY_STR;
        }
      }
    } finally {
      br.close();
    }
  }

  protected static boolean isEndOfJavaLine(String line) {
    return line.endsWith(";") || line.endsWith("{") || line.endsWith("}");
  }
  
  protected static String minimizeJavaLine(String line) {
    line = line.trim();
    line = line.replaceAll("\r\n", EMPTY_STR);
    line = line.replaceAll("\n", EMPTY_STR);
    
    return line;
  }
  
  private static final String VARIABLE_NAME_PATTERN = "[a-zA-Z][a-zA-Z0-9_]*";
  private static final String DECELERATION_END_PATTERN = " *[=|;]";
  private static final Pattern PRIMITIVE_DECELERATION_PATTERN = Pattern.compile("(short|int|long|float|double|boolean|char|byte|Short|Integer|Long|Float|Double|Boolean|Char|Byte) " + 
                                                                                  VARIABLE_NAME_PATTERN + DECELERATION_END_PATTERN);
  private static final Pattern OBJECT_DECELERATION_PATTERN = Pattern.compile("[A-Z][a-zA-Z0-9]* " + 
                                                                               VARIABLE_NAME_PATTERN + DECELERATION_END_PATTERN);
  
  protected static boolean isDecelerationLine(String line) {
    Matcher m = PRIMITIVE_DECELERATION_PATTERN.matcher(line);
    if (m.find()) {
      //System.out.println("prim:" + m.group());
      return true;
    }
    
    m = OBJECT_DECELERATION_PATTERN.matcher(line);
    if (m.find()) {
      //System.out.println("obj: " + m.group());
      return true;
    }
    
    return false;
  }
  
  protected static void analyzeSpelling(String line) {
    
  }

  public int getExaminedFileCount() {
    return examinedFiles.get();
  }
}
