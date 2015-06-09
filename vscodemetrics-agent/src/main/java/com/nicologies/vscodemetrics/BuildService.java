package com.nicologies.vscodemetrics;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.nicologies.vscodemetrics.common.ArtifactsUtil;
import com.nicologies.vscodemetrics.common.CodeMetricConstants;
import com.nicologies.vscodemetrics.common.SettingsKeys;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.agent.runner.BuildServiceAdapter;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import jetbrains.buildServer.util.AntPatternFileFinder;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class BuildService extends BuildServiceAdapter {
  private final ArtifactsWatcher myArtifactsWatcher;
  private final InspectionReporter myInspectionReporter;
  private File myOutputDirectory;
  private File myXmlReportFile;

  public BuildService(final ArtifactsWatcher artifactsWatcher, final InspectionReporter inspectionReporter) {
    myArtifactsWatcher = artifactsWatcher;
    myInspectionReporter = inspectionReporter;

  }

  @Override
  public void afterInitialized() throws RunBuildException {
    super.afterInitialized();

    try {
      myOutputDirectory = FileUtil.createTempFile(getBuildTempDirectory(), "vscodemetrics-output-", "", false);
      if (!myOutputDirectory.mkdirs()) {
        throw new RuntimeException("Unable to create temp output directory " + myOutputDirectory);
      }

      myXmlReportFile = new File(myOutputDirectory, "VsCodeMetrics.xml");
    } catch (IOException e) {
      final String message = "Unable to create temporary file in " +
          getBuildTempDirectory() + " for vscodemetrics: " +
          e.getMessage();

      Logger.getInstance(getClass().getName()).error(message, e);
      throw new RunBuildException(message);
    }
  }

  @Override
  public void beforeProcessStarted() throws RunBuildException {
    getLogger().progressMessage("Running Visual Studio Code Metrics");
  }

  private void importInspectionResults() throws Exception {
    final String workingRoot = getCheckoutDirectory().toString();

    getLogger().progressMessage("Importing inspection results");

    myInspectionReporter.markBuildAsInspectionsBuild();
 //   final FxCopFileProcessor fileProcessor = new FxCopFileProcessor(myXmlReportFile, workingRoot, getLogger(), myInspectionReporter);
 //   fileProcessor.processReport();
  }

  @NotNull
  @Override
  public BuildFinishedStatus getRunResult(final int exitCode) {
    String failMessage = null;

    if (exitCode != 0) {
      final EnumSet<FxCopReturnCode> errors = FxCopReturnCode.decodeReturnCode(exitCode);
      StringBuilder exitCodeStr = new StringBuilder("FxCop return code (" + exitCode + "):");
      for (FxCopReturnCode rc : errors) {
        exitCodeStr.append(" ").append(rc.name());
      }

      getLogger().warning(exitCodeStr.toString());

      if (errors.contains(FxCopReturnCode.BUILD_BREAKING_MESSAGE)) {
        failMessage = "FxCop return code contains 'Build breaking message'\"";
      }

      if (errors.contains(FxCopReturnCode.COMMAND_LINE_SWITCH_ERROR)) {
        failMessage = exitCodeStr.toString();
      }

      if (errors.contains(FxCopReturnCode.ANALYSIS_ERROR) ||
          errors.contains(FxCopReturnCode.ASSEMBLY_LOAD_ERROR) ||
          errors.contains(FxCopReturnCode.ASSEMBLY_REFERENCES_ERROR) ||
          errors.contains(FxCopReturnCode.PROJECT_LOAD_ERROR) ||
          errors.contains(FxCopReturnCode.RULE_LIBRARY_LOAD_ERROR) ||
          errors.contains(FxCopReturnCode.UNKNOWN_ERROR) ||
          errors.contains(FxCopReturnCode.OUTPUT_ERROR)) {
        boolean failOnAnalysisErrors = isParameterEnabled(SettingsKeys.FailOnAnalysisError);

        if (failOnAnalysisErrors) {
          failMessage = exitCodeStr.toString();
        } else {
          getLogger().warning("Analysis errors ignored as 'Fail on analysis errors' option unchecked");
        }
      }
    }

    if (myXmlReportFile.exists()) {
      myArtifactsWatcher.addNewArtifactsPath(myXmlReportFile.getPath() + "=>" + ArtifactsUtil.getInternalArtifactPath(""));

      try {
        importInspectionResults();
      } catch (Exception e) {
        getLogger().error("Exception while importing results: " + e);
        failMessage = "Visual Studio Code Metrics results import error";
      }
    } else {
      if (failMessage == null) {
        failMessage = "Output xml is not found";
      }
    }

    if (failMessage != null) {
      logBuildProblem(BuildProblemData.createBuildProblem(String.valueOf(exitCode), CodeMetricConstants.RunnerType, failMessage));
      return BuildFinishedStatus.FINISHED_WITH_PROBLEMS;
    }

    return BuildFinishedStatus.FINISHED_SUCCESS;
  }

  private boolean isParameterEnabled(final String key) {
    final Map<String, String> runnerParameters = getRunnerParameters();

    return runnerParameters.containsKey(key) && runnerParameters.get(key).equals(Boolean.TRUE.toString());
  }

  @NotNull
  public ProgramCommandLine makeProgramCommandLine() throws RunBuildException {
    final Map<String, String> runParameters = getRunnerParameters();

    List<String> files = new ArrayList<String>();
    try {
      files = matchFiles();
    } catch (IOException e) {
      throw new RunBuildException("I/O error while collecting files", e);
    }

    if (files.size() == 0) {
      throw new RunBuildException("No files matched the pattern");
    }

    final List<String> finalFiles = files;

    final CommandLineBuilder commandLineBuilder = new CommandLineBuilder(runParameters, getBuildParameters().getAllParameters(), myXmlReportFile, getLogger());
    return new ProgramCommandLine() {
      @NotNull
      public String getExecutablePath() throws RunBuildException {
        return commandLineBuilder.getExecutablePath();
      }

      @NotNull
      public String getWorkingDirectory() throws RunBuildException {
        return getCheckoutDirectory().getPath();
      }

      @NotNull
      public List<String> getArguments() throws RunBuildException {
        return commandLineBuilder.getArguments(finalFiles);
      }

      @NotNull
      public Map<String, String> getEnvironment() throws RunBuildException {
        return getBuildParameters().getEnvironmentVariables();
      }
    };
  }

  private List<String> matchFiles() throws IOException {
    final Map<String, String> runParameters = getRunnerParameters();

    final AntPatternFileFinder finder = new AntPatternFileFinder(
      splitFileWildcards(runParameters.get(SettingsKeys.Files)),
      splitFileWildcards(runParameters.get(SettingsKeys.FilesToExclude)),
      SystemInfo.isFileSystemCaseSensitive);
    final File[] files = finder.findFiles(getCheckoutDirectory());

    getLogger().logMessage(DefaultMessagesInfo.createTextMessage("Matched assembly files:"));

    final List<String> result = new ArrayList<String>(files.length);
    for (File file : files) {
      final String relativeName = FileUtil.getRelativePath(getWorkingDirectory(), file);

      result.add(relativeName);
      getLogger().logMessage(DefaultMessagesInfo.createTextMessage("  " + relativeName));
    }

    if (files.length == 0) {
      getLogger().logMessage(DefaultMessagesInfo.createTextMessage("  none"));
    }

    return result;
  }

  private static String[] splitFileWildcards(final String string) {
    if (string != null) {
      final String filesStringWithSpaces = string.replace('\n', ' ').replace('\r', ' ').replace('\\', '/');
      final List<String> split = StringUtil.splitCommandArgumentsAndUnquote(filesStringWithSpaces);
      return split.toArray(new String[split.size()]);
    }

    return new String[0];
  }
}
