package com.jentfoo.spell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
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
  private static final int MAX_TEXT_FILE_SIZE = 1024 * 1024 * 100;
  private static final boolean IGNORE_MARKUP_FILES = true;
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
  
  private final boolean scanNonJavaFiles;
  private final AtomicInteger examinedJavaFiles;
  
  public SpellcheckFileListener(boolean scanNonJavaFiles) {
    this.scanNonJavaFiles = scanNonJavaFiles;
    examinedJavaFiles = new AtomicInteger(0);
  }
  
  public int getExaminedJavaFileCount() {
    return examinedJavaFiles.get();
  }

  @Override
  public void handleFile(File file) {
    try {
      StringBuilder fileResult = null;
      if (file.getName().endsWith(JAVA_EXTENSION)) {
        fileResult = spellCheckJavaFile(file);
        
        examinedJavaFiles.incrementAndGet();
      } else if (scanNonJavaFiles && 
                 isTextFile(file) && 
                 file.length() < MAX_TEXT_FILE_SIZE) {
        fileResult = spellCheckTextFile(file);
      }
      
      if (fileResult != null && fileResult.length() > 0) {
        outputResults(fileResult.toString(), file);
      }
    } catch (Exception e) {
      throw ExceptionUtils.makeRuntime(e);
    }
  }
  
  private void outputResults(String potentialErrors, File file) {
    synchronized (this) {
      System.out.println("Potential errors for file: " + file);
      System.out.println(potentialErrors);
    }
  }
  
  private static JLanguageTool buildJLangTool() throws IOException {
    JLanguageTool langTool = new JLanguageTool(new English());
    langTool.activateDefaultPatternRules();
    
    langTool.disableRule("COMMA_PARENTHESIS_WHITESPACE");
    langTool.disableRule("EN_UNPAIRED_BRACKETS");
    langTool.disableRule("UPPERCASE_SENTENCE_START");
    langTool.disableRule("WHITESPACE_RULE");
    langTool.disableRule("DOUBLE_PUNCTUATION");
    langTool.disableRule("ENGLISH_WORD_REPEAT_RULE");
    langTool.disableRule("ENGLISH_WORD_REPEAT_BEGINNING_RULE");
    langTool.disableRule("EN_QUOTES");
    langTool.disableRule("DT_JJ_NO_NOUN");
    langTool.disableRule("THREE_NN");
    langTool.disableRule("HE_VERB_AGR");
    langTool.disableRule("PERIOD_OF_TIME");
    langTool.disableRule("BEEN_PART_AGREEMENT");
    //langTool.disableRule("");
    
    return langTool;
  }
  
  private StringBuilder spellCheckTextFile(File file) throws IOException {
    JLanguageTool langTool = buildJLangTool();
    
    String fileString = getFileString(file, Integer.MAX_VALUE);
    List<RuleMatch> matches = langTool.check(fileString);
    
    if (matches.isEmpty()) {
      return new StringBuilder(0);
    }
    StringBuilder result = new StringBuilder(32 * matches.size());
    
    Iterator<RuleMatch> it = matches.iterator();
    while (it.hasNext()) {
      RuleMatch rm = it.next();
      result.append("line:").append(rm.getLine())
            .append(",column:").append(rm.getColumn())
            .append(" - ").append(rm.getMessage())
            //.append("...").append(rm.getRule().getId())
            .append('\n');
      result.append("Suggested correction: ")
            .append(rm.getSuggestedReplacements());
      if (it.hasNext()) {
        result.append('\n');
      } else {
        break;
      }
    }
    
    return result;
  }
  
  private static String getFileString(File f, int maxSize) throws IOException {
    int size = (int)Math.min(maxSize, f.length());
    byte[] data = new byte[size];
    FileInputStream in = new FileInputStream(f);
    try {
      in.read(data);
    } finally {
      in.close();
    }
    return new String(data, "ISO-8859-1");
  }

  private boolean isTextFile(File f) throws IOException {
    String fileName = f.getName();
    // eliminate some common file names
    if (IGNORE_MARKUP_FILES && 
          (fileName.endsWith(".html") || 
             fileName.endsWith(".xml") || 
             fileName.endsWith(".class"))) {
      return false;
    } else if (! f.exists()) {
      return false;
    }
    
    String s = getFileString(f, 1000);
    // will delete all text signs
    String s2 = s.replaceAll("[a-zA-Z0-9ßöäü\\.\\*!\"§\\$\\%&/()=\\?@~'#:,;\\"+
                               "+><\\|\\[\\]\\{\\}\\^°²³\\\\ \\n\\r\\t_\\-`´âêîô"+
                               "ÂÊÔÎáéíóàèìòÁÉÍÓÀÈÌÒ©‰¢£¥€±¿»«¼½¾™ª]", "");

    // percentage of text signs in the text
    double d = (double)(s.length() - s2.length()) / (double)(s.length());
    if (d > 0.95) {
      if (IGNORE_MARKUP_FILES) {
        // is a text document, now make sure it is not markup
        String s3 = s2.replaceAll("[<|>]", "");
        d = (double)(s2.length() - s3.length()) / (double)(s2.length());
        if (d > 0.95) {
          return true;
        } else {
          //System.out.println("File is markup: " + f);
          return false;
        }
      } else {
        return true;
      }
    } else {
      return false;
    }
  }
  
  protected StringBuilder spellCheckJavaFile(File file) throws IOException {
    StringBuilder fileResult = new StringBuilder(32);
    JLanguageTool langTool = buildJLangTool();
    
    BufferedReader br = new BufferedReader(new FileReader(file));
    try {
      int lineCount = 0;
      String currentJavaLine = null;
      int currentJavaLineCount = 0;
      String line;
      boolean inCommentSection = false;
      while ((line = br.readLine()) != null) {
        line = line.trim();
        lineCount++;
        
        if (line.length() == 0) {
          continue;
        }
        
        StringBuilder lineResult = null;
        if (line.startsWith("//")) {
          lineResult = analyzeCommentSpelling(langTool, line);
          currentJavaLineCount = 1;
        } else {
          if (line.startsWith("/*")) {
            inCommentSection = true;
          }
          if (currentJavaLine == null) {
            currentJavaLine = line;
            currentJavaLineCount = 1;
          } else {
            currentJavaLine += line;
            currentJavaLineCount++;
          }
          if (inCommentSection && currentJavaLine.endsWith("*/")) {
            currentJavaLine = minimizeLine(currentJavaLine);
            lineResult = analyzeCommentSpelling(langTool, currentJavaLine);
            
            currentJavaLine = null;
          } else if (isEndOfJavaLine(currentJavaLine)) {
            currentJavaLine = minimizeLine(currentJavaLine);
            List<String> toInspectLines = breakApartLine(currentJavaLine);
            boolean hadVariables = false;
            Iterator<String> it = toInspectLines.iterator();
            while (it.hasNext()) {
              String inspectLine = it.next();
              String variable = getVariable(inspectLine);
              if (variable != null) {
                hadVariables = true;
                StringBuilder newResult = analyzeVariableSpelling(langTool, variable);
                if (lineResult == null) {
                  lineResult = newResult;
                } else {
                  if (lineResult.length() > 0 && newResult.length() > 0) {
                    lineResult.append('\n');
                  }
                  lineResult.append(newResult);
                }
              }
            }
            
            if (! hadVariables) {
              /*if (lineResult == null) {
                lineResult = new StringBuilder();
              }
              lineResult.append("Not analyzing line: ").append(currentJavaLine);*/
            }
            
            currentJavaLine = null;
          }
        }
        
        if (lineResult != null && lineResult.length() != 0) {
          lineResult.insert(0, '\t');
          int newLineIndex = -1;
          while ((newLineIndex = lineResult.indexOf("\n", newLineIndex)) != -1) {
            lineResult.insert(newLineIndex + 1, '\t');
            newLineIndex += 2;  // add two to handle insertion
          }
          
          if (fileResult.length() != 0) {
            fileResult.append('\n');
          }
          if (currentJavaLineCount > 1) {
            fileResult.append("-- lines: ")
                      .append(lineCount - currentJavaLineCount + 1)
                      .append(" to ")
                      .append(lineCount);
          } else {
            fileResult.append("-- line: ")
                      .append(lineCount);
          }
          fileResult.append('\n');
          fileResult.append(lineResult);
        }
      }
    } finally {
      br.close();
    }
    
    return fileResult;
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
  
  protected static StringBuilder analyzeVariableSpelling(JLanguageTool langTool, 
                                                         String variable) throws IOException {
    StringBuilder result = new StringBuilder(32);
    
    for(String word : variable.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")) {
      if (word.length() > MIN_WORD_LENGTH) {
        result.append(analyzeSpelling(langTool, word));
      }
    }
    
    return result;
  }
  
  protected static StringBuilder analyzeCommentSpelling(JLanguageTool langTool, 
                                                        String commentSection) throws IOException {
    commentSection = commentSection.replaceAll("(/\\*|//|\\*/)", "");
    commentSection = commentSection.trim();
    
    return analyzeSpelling(langTool, commentSection);
  }
  
  protected static StringBuilder analyzeSpelling(JLanguageTool langTool, 
                                                 String line) throws IOException {
    List<RuleMatch> matches = langTool.check(line);
    
    StringBuilder result;
    if (matches.isEmpty()) {
      result = new StringBuilder(0);
    } else {
      result = new StringBuilder(32);
    }
    
    Iterator<RuleMatch> it = matches.iterator();
    while (it.hasNext()) {
      RuleMatch rm = it.next();
      result.append(rm.getMessage())
            /*.append(" - ").append(rm.getRule().getId())*/
            .append('\n');
      result.append("Suggested correction: ")
            .append(rm.getSuggestedReplacements());
      if (it.hasNext()) {
        result.append('\n');
      } else {
        break;
      }
    }
    
    return result;
  }
}
