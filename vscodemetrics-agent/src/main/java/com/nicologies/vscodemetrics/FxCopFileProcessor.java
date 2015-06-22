package com.nicologies.vscodemetrics;

import jetbrains.buildServer.agent.SimpleBuildLogger;
import jetbrains.buildServer.agent.inspections.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Stack;

public class FxCopFileProcessor {

  private final Stack<String> myMessageInspectionId = new Stack<String>();
  private final File myFxCopReport;
  private final String mySourceFilePrefixLower;
  private final InspectionReporter myReporter;

  private int myErrorsCount, myWarningsCount;

  public FxCopFileProcessor(@NotNull final File fxcopReport,
                            @NotNull final String sourceFilePrefix,
                            @NotNull final SimpleBuildLogger logger,
                            @NotNull final InspectionReporter reporter) {
    myFxCopReport = fxcopReport;
    mySourceFilePrefixLower = sourceFilePrefix.toLowerCase().replace('\\', '/');
    myReporter = reporter;
  }

  public void processReport() throws IOException {
    handleFile();
  }

  private void handleFile() throws IOException {
    Reader reader = new BufferedReader(
      new InputStreamReader(new FileInputStream(myFxCopReport), "UTF8"));

    try {
        handleChildren();
    } finally {
      reader.close();
    }
  }

  private Method getHandlerMethod(final String tagName) throws NoSuchMethodException {
    return getClass().getDeclaredMethod("handle" + tagName + "Tag");
  }

  private void handleChildren() {
  }

  private String getMessage(final String nodeName) {
    return "Error while processing FxCop report '" + myFxCopReport.getAbsolutePath() + "', tag " + nodeName;
  }

  private InspectionSeverityValues convertLevel(String level) {
    if (level.contains("Error")) {
      return InspectionSeverityValues.ERROR;
    }

    if (level.contains("Warning")) {
      return InspectionSeverityValues.WARNING;
    }

    return InspectionSeverityValues.INFO;
  }

  private String reformatInOneLine(@NotNull final String source) {
    return source.replace("\r", "").replace("\n", " ").replaceAll("\\s+", " ").trim();
  }

  public int getErrorsCount() {
    return myErrorsCount;
  }

  public int getWarningsCount() {
    return myWarningsCount;
  }
}
