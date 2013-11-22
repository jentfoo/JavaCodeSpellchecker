package com.jentfoo.spell;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.threadly.concurrent.PriorityScheduledExecutor;
import org.threadly.concurrent.TaskPriority;

import com.jentfoo.file.FileCrawler;
import com.jentfoo.file.HiddenFileFilter;

public class JavaCodeSpellchecker {
  public static void main(String args[]) throws IOException {
    if (args.length == 0) {
      System.err.println("Must provide at least one valid source path to inspect");
      System.exit(1);
    }
    
    List<File> examineDirectories = new ArrayList<File>(args.length);
    for (String path : args) {
      File toInspectPath = new File(path);
      if (! toInspectPath.exists()) {
        throw new IllegalArgumentException("Path does not exist: " + path);
      } else if (! toInspectPath.isDirectory()) {
        throw new IllegalArgumentException("Path is not a directory: " + path);
      }
      
      examineDirectories.add(toInspectPath);
    }
    
    int threadCount = 1;//Runtime.getRuntime().availableProcessors();
    final PriorityScheduledExecutor scheduler = new PriorityScheduledExecutor(threadCount, threadCount, Long.MAX_VALUE, 
                                                                              TaskPriority.High, 1000, false);
    scheduler.execute(new Runnable() {
      @Override
      public void run() {
        scheduler.prestartAllCoreThreads();
      }
    });
    try {
      FileCrawler fc = new FileCrawler(scheduler, 5000, -1);
      
      fc.addFilter(new HiddenFileFilter());
      
      SpellcheckFileListener sfl = new SpellcheckFileListener();
      fc.addListener(sfl);
      
      // blocks till computation is done
      fc.crawlDirectories(examineDirectories);
      
      if (sfl.getExaminedFileCount() == 0) {
        System.err.println("No java files found under directories: " + examineDirectories);
      } else {
        System.out.println("\nDONE!!");
      }
    } finally {
      scheduler.shutdown();
    }
  }
}
