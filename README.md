JavaCodeSpellchecker
====================

This is a tool to spell check written java code.  It verifies spelling in comments, and variable declarations (either with words comprised from camel case or underscores) for correct spelling.

Right now since this depends on other java projects I work on which do not publish artifacts, the build system is only setup for eclipse.

This depends on:
* Threadly (published to maven central, but in this email we just have the code imported into eclipse)
* JFileAnalyzer...personal project of mine which has nice directory crawling logic in it.  This is not published, and must be checked out
* JLanguageTool library and it's dependencies (which are all located in the lib directory) 

I run spell checking for my entrie workspace directory with the following command:
# CD into workspace directory
java -Xmx1024m -Xms128m cp JavaCodeSpellchecker/bin/:JavaCodeSpellchecker/lib/*:Threadly/build/classes/main/:JFileAnalyzer/build/classes/main/ com.jentfoo.spell.JavaCodeSpellchecker ./
