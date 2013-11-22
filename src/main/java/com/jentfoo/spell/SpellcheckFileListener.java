package com.jentfoo.spell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.languagetool.JLanguageTool;
import org.languagetool.language.English;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;
import org.threadly.util.ExceptionUtils;

import com.jentfoo.file.FileListenerInterface;

public class SpellcheckFileListener implements FileListenerInterface {
  private static final String JAVA_EXTENSION = ".java";
  private static final String EMPTY_STR = "";
  private static final String SEMI_STR = ";";
  private static final String COMMA_STR = ",";
  private static final String LEFT_BRACKET_STR = "{";
  private static final String RIGHT_BRACKET_STR = "}";
  private static final short MIN_WORD_LENGTH = 4;
  private static final String VARIABLE_NAME_PATTERN = "[a-zA-Z][a-zA-Z0-9_]*";
  private static final String OBJECT_NAME_PATTERN = "[A-Z].*";
  private static final String DECELERATION_END_PATTERN = " *[=|;|,|)]";
  private static final Pattern PRIMITIVE_DECELERATION_PATTERN = Pattern.compile("(short|int|long|float|double|boolean|char|byte|Short|Integer|Long|Float|Double|Boolean|Char|Byte) " + 
                                                                                  VARIABLE_NAME_PATTERN + DECELERATION_END_PATTERN);
  private static final Pattern OBJECT_DECELERATION_PATTERN = Pattern.compile(OBJECT_NAME_PATTERN + " " + 
                                                                               VARIABLE_NAME_PATTERN + DECELERATION_END_PATTERN);
  private static final Pattern VARIABLE_GROUP_PATTERN = Pattern.compile(" (" + VARIABLE_NAME_PATTERN + ")" + DECELERATION_END_PATTERN);
  private static final Pattern FUNCTION_DECELERATION_PATTERN = Pattern.compile("(public|private|protected) " + VARIABLE_NAME_PATTERN + " " + 
                                                                                 VARIABLE_NAME_PATTERN /*+ "\\(.*\\)"*/);
  private static final Pattern FUNCTION_GROUP_PATTERN = Pattern.compile("(public|private|protected) " + VARIABLE_NAME_PATTERN + " (" + 
                                                                          VARIABLE_NAME_PATTERN + ")");
  
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
  
  private volatile boolean outputted = false;
  
  protected void spellCheckJavaFile(File file) throws IOException {
    System.out.println("Checking file: " + file);
    JLanguageTool langTool = new JLanguageTool(new English());
    if (! outputted) {
      outputted = true;
      System.out.println("Rules:");
      Iterator<Rule> it = langTool.getAllActiveRules().iterator();
      while (it.hasNext()) {
        Rule r = it.next();
        System.out.println("\t" + r.getId());
      }
    }
    langTool.disableRule("COMMA_PARENTHESIS_WHITESPACE");
    langTool.disableRule("EN_UNPAIRED_BRACKETS");
    langTool.disableRule("UPPERCASE_SENTENCE_START");
    langTool.disableRule("WHITESPACE_RULE");
    
    BufferedReader br = new BufferedReader(new FileReader(file));
    try {
      int lineCount = 0;
      String currentJavaLine = EMPTY_STR;
      String line;
      boolean inCommentSection = false;
      while ((line = br.readLine()) != null) {
        line = line.trim();
        lineCount++;
        
        if (line.startsWith("//")) {
          //System.out.println("Examining comment line: " + line);
          analyzeCommentSpelling(langTool, line);
        } else {
          if (line.startsWith("/*")) {
            inCommentSection = true;
          }
          currentJavaLine += line;
          if (inCommentSection && currentJavaLine.endsWith("*/")) {
            currentJavaLine = minimizeLine(currentJavaLine);
            //System.out.println("Examining comment line: " + currentJavaLine);
            analyzeCommentSpelling(langTool, currentJavaLine);
            
            currentJavaLine = EMPTY_STR;
          } else if (isEndOfJavaLine(currentJavaLine)) {
            currentJavaLine = minimizeLine(currentJavaLine);
            //System.out.println("Examining code line: " + currentJavaLine);
            List<String> toInspectLines = breakApartLine(currentJavaLine);
            Iterator<String> it = toInspectLines.iterator();
            while (it.hasNext()) {
              String inspectLine = it.next();
              String variable = getVariable(inspectLine);
              if (variable != null) {
                //System.out.println("Inspecting variable: " + variable);
                analyzeVariableSpelling(langTool, variable);
              }
            }
            
            currentJavaLine = EMPTY_STR;
          }
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
  
  protected static String minimizeLine(String line) {
    line = line.trim();
    line = line.replaceAll("\r\n", EMPTY_STR);
    line = line.replaceAll("\n", EMPTY_STR);
    
    return line;
  }

  protected static List<String> breakApartLine(String line) {
    List<String> result = new LinkedList<String>();
    
    breakApartLine(line, result);
    
    return result;
  }

  protected static void breakApartLine(String line, List<String> result) {
    int parenIndex = line.indexOf('(');
    if (parenIndex > 0) {
      if (parenIndex != line.length() - 1) {
        breakApartLine(line.substring(0, parenIndex + 1), result);
        breakApartLine(line.substring(parenIndex + 1), result);
      } else {
        result.add(line);
      }
    } else {
      StringTokenizer st1 = new StringTokenizer(line, SEMI_STR);
      while (st1.hasMoreTokens()) {
        String st1Token = st1.nextToken();
        StringTokenizer st2 = new StringTokenizer(st1Token, COMMA_STR);
        if (st2.countTokens() == 1) {
          result.add(st1Token + SEMI_STR);
        } else {
          while (st2.hasMoreTokens()) {
            result.add(st2.nextToken() + COMMA_STR);
          }
        }
      }
    }
  }
  
  protected static boolean isPrimitiveDecelerationLine(String line) {
    Matcher m = PRIMITIVE_DECELERATION_PATTERN.matcher(line);
    return m.find();
  }
  
  protected static boolean isObjectDecelerationLine(String line) {
    Matcher m = OBJECT_DECELERATION_PATTERN.matcher(line);
    return m.find();
  }
  
  protected static boolean isFunctionDecelerationLine(String line) {
    Matcher m = FUNCTION_DECELERATION_PATTERN.matcher(line);
    return m.find();
  }

  protected static String getVariable(String inspectLine) {
    if (isPrimitiveDecelerationLine(inspectLine) || 
        isObjectDecelerationLine(inspectLine)) {
      Matcher m = VARIABLE_GROUP_PATTERN.matcher(inspectLine);
      m.find();
      return m.group(1);
    } else if (isFunctionDecelerationLine(inspectLine)) {
      Matcher m = FUNCTION_GROUP_PATTERN.matcher(inspectLine);
      m.find();
      String groupStr = m.group(2);
      return groupStr;
    } else {
      return null;
    }
  }
  
  protected static void analyzeVariableSpelling(JLanguageTool langTool, 
                                                String variable) throws IOException {
    for(String word : variable.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")) {
      if (word.length() > MIN_WORD_LENGTH) {
        analyzeSpelling(langTool, word);
      }
    }
  }
  
  protected static void analyzeCommentSpelling(JLanguageTool langTool, 
                                               String commentSection) throws IOException {
    analyzeSpelling(langTool, commentSection);
  }
  
  protected static void analyzeSpelling(JLanguageTool langTool, 
                                        String line) throws IOException {
    List<RuleMatch> matches = langTool.check(line);
    Iterator<RuleMatch> it = matches.iterator();
    while (it.hasNext()) {
      RuleMatch rm = it.next();
      // TODO output in a different way
      System.out.println("Potential error: " + rm.getMessage());
      System.out.println("Suggested correction: " +
          rm.getSuggestedReplacements());
    }
  }

  public int getExaminedFileCount() {
    return examinedFiles.get();
  }
}
