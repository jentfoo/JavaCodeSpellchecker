package com.jentfoo.spell;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import com.jentfoo.file.FileListenerInterface;

public class SpellcheckFileListener implements FileListenerInterface {
  private static final String JAVA_EXTENSION = ".java";
  
  private final AtomicInteger examinedFiles = new AtomicInteger(0);

  @Override
  public void handleFile(File file) {
    if (! file.getName().endsWith(JAVA_EXTENSION)) {
      // ignore none-java files
      return;
    }
    
    examinedFiles.incrementAndGet();
    
    spellCheckJavaFile(file);
  }
  
  private void spellCheckJavaFile(File file) {
    // TODO Auto-generated method stub
    
  }

  public int getExaminedFileCount() {
    return examinedFiles.get();
  }
}
