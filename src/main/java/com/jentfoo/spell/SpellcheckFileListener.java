package com.jentfoo.spell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.languagetool.JLanguageTool;
import org.languagetool.language.English;
import org.languagetool.rules.RuleMatch;
import org.threadly.util.ExceptionUtils;

import com.jentfoo.file.FileListenerInterface;

public class SpellcheckFileListener implements FileListenerInterface {
  private static final String JAVA_EXTENSION = ".java";
  private static final String EMPTY_STR = "";
  private static final String SEMI_STR = ";";
  private static final String LEFT_BRACKET_STR = "{";
  private static final String RIGHT_BRACKET_STR = "}";
  private static final short MIN_WORD_LENGTH = 4;
  private static final String VARIABLE_NAME_PATTERN = "[a-zA-Z][a-zA-Z0-9_]*";
  private static final String DECELERATION_END_PATTERN = " *[=|;]";
  private static final Pattern PRIMITIVE_DECELERATION_PATTERN = Pattern.compile("(short|int|long|float|double|boolean|char|byte|Short|Integer|Long|Float|Double|Boolean|Char|Byte) " + 
                                                                                  VARIABLE_NAME_PATTERN + DECELERATION_END_PATTERN);
  private static final Pattern OBJECT_DECELERATION_PATTERN = Pattern.compile("[A-Z][a-zA-Z0-9]* " + 
                                                                               VARIABLE_NAME_PATTERN + DECELERATION_END_PATTERN);
  private static final Pattern VARIABLE_GROUP_PATTERN = Pattern.compile(" (" + VARIABLE_NAME_PATTERN + ")" + DECELERATION_END_PATTERN);
  
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
    JLanguageTool langTool = new JLanguageTool(new English());
    langTool.activateDefaultPatternRules();
    
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
          List<String> toInspectLines = breakApartLine(currentJavaLine);
          Iterator<String> it = toInspectLines.iterator();
          while (it.hasNext()) {
            String inspectLine = it.next();
            String variable = getVariable(inspectLine);
            if (variable != null) {
              analyzeSpelling(langTool, variable);
            }
          }
          
          currentJavaLine = EMPTY_STR;
        }
      }
    } finally {
      br.close();
    }
  }

  protected static boolean isEndOfJavaLine(String line) {
    return line.endsWith(SEMI_STR) || 
             line.endsWith(LEFT_BRACKET_STR) || 
             line.endsWith(RIGHT_BRACKET_STR);
  }
  
  protected static String minimizeJavaLine(String line) {
    line = line.trim();
    line = line.replaceAll("\r\n", EMPTY_STR);
    line = line.replaceAll("\n", EMPTY_STR);
    
    return line;
  }

  protected static List<String> breakApartLine(String line) {
    StringTokenizer st = new StringTokenizer(line, SEMI_STR);
    List<String> result = new ArrayList<String>(st.countTokens());
    while (st.hasMoreTokens()) {
      result.add(st.nextToken() + SEMI_STR);
    }
    
    return result;
  }
  
  protected static boolean isPrimitiveDecelerationLine(String line) {
    Matcher m = PRIMITIVE_DECELERATION_PATTERN.matcher(line);
    return m.find();
  }
  
  protected static boolean isObjectDecelerationLine(String line) {
    Matcher m = OBJECT_DECELERATION_PATTERN.matcher(line);
    return m.find();
  }

  protected static String getVariable(String inspectLine) {
    if (isPrimitiveDecelerationLine(inspectLine) || 
        isObjectDecelerationLine(inspectLine)) {
      Matcher m = VARIABLE_GROUP_PATTERN.matcher(inspectLine);
      m.find();
      return m.group(1);
    } else {
      return null;
    }
  }
  
  protected static void analyzeSpelling(JLanguageTool langTool, 
                                        String variable) throws IOException {
    for(String word : variable.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")) {
      if (word.length() > MIN_WORD_LENGTH) {
        List<RuleMatch> matches = langTool.check(word);
        Iterator<RuleMatch> it = matches.iterator();
        while (it.hasNext()) {
          RuleMatch rm = it.next();
          // TODO output in a different way
          System.out.println("Potential error at line " +
              rm.getLine() + ": " + rm.getMessage());
          System.out.println("Suggested correction: " +
              rm.getSuggestedReplacements());
        }
      }
    }
  }

  public int getExaminedFileCount() {
    return examinedFiles.get();
  }
}
