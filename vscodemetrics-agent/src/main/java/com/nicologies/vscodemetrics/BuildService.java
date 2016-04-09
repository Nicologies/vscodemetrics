package com.nicologies.vscodemetrics;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.nicologies.vscodemetrics.common.*;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import jetbrains.buildServer.agent.runner.*;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import jetbrains.buildServer.util.AntPatternFileFinder;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.apache.commons.io.FilenameUtils;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;

public class BuildService extends BuildServiceAdapter {
    private final ArtifactsWatcher myArtifactsWatcher;
    private File myOutputDirectory;

    public BuildService(final ArtifactsWatcher artifactsWatcher) {
        myArtifactsWatcher = artifactsWatcher;
    }

    @Override
    public void afterInitialized() throws RunBuildException {
        super.afterInitialized();

        try {
            myOutputDirectory = FileUtil.createTempFile(getBuildTempDirectory(), "vscodemetrics-output-", "", false);
            if (!myOutputDirectory.mkdirs()) {
                throw new RuntimeException("Unable to create temp output directory " + myOutputDirectory);
            }
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

        final Map<String, String> runParameters = getRunnerParameters();

        List<File> files;
        try {
            files = matchFiles();
        } catch (Exception e) {
            throw new RunBuildException("Error while collecting files", e);
        }

        if (files.size() == 0) {
            throw new RunBuildException("No files matched the pattern");
        }

        for(File file : files){

            File outputFile = new File(myOutputDirectory, file.getName() + CodeMetricConstants.XmlResultFile);

            final CommandLineBuilder commandLineBuilder = new CommandLineBuilder(runParameters,
                    getBuildParameters().getSystemProperties(), outputFile, getLogger());
            List<String> args = commandLineBuilder.getArguments(file);
            args.add(0, commandLineBuilder.getExecutablePath());
            String argsString = StringUtil.join(" ", args);
            getLogger().message(argsString);
            ProcessBuilder pb = new ProcessBuilder(args);
            ProcessInvoker p = new ProcessInvoker(pb);
            int exitCode = p.invoke();
            if(exitCode != 0){
                getLogger().warning("unable to analyze file " + file);
                getLogger().warning(p.stdErr());
            }
        }
        getLogger().progressMessage("Transforming Code Metrics result to Teamcity");

        MetricsTransformer transformer = new MetricsTransformer(Loggers.AGENT);
        transformer.generateHtmlReport(myOutputDirectory, myOutputDirectory);
    }

    @NotNull
    @Override
    public BuildFinishedStatus getRunResult(final int exitCode) {
        String failMessage = null;

        if (exitCode != 0) {
            final EnumSet<MetricsExeReturnCodes> errors = MetricsExeReturnCodes.decodeReturnCode(exitCode);
            StringBuilder exitCodeStr = new StringBuilder("Metrics.exe return code (" + exitCode + "):");
            for (MetricsExeReturnCodes rc : errors) {
                exitCodeStr.append(" ").append(rc.name());
            }

            getLogger().warning(exitCodeStr.toString());

            if (errors.contains(MetricsExeReturnCodes.COMMAND_LINE_SWITCH_ERROR)) {
                failMessage = exitCodeStr.toString();
            }

            if (errors.contains(MetricsExeReturnCodes.ANALYSIS_ERROR) ||
                    errors.contains(MetricsExeReturnCodes.ASSEMBLY_REFERENCES_ERROR) ||
                    errors.contains(MetricsExeReturnCodes.UNKNOWN_ERROR) ||
                    errors.contains(MetricsExeReturnCodes.OUTPUT_ERROR)) {
                boolean failOnAnalysisErrors = isParameterEnabled(SettingsKeys.FailOnAnalysisError);

                if (failOnAnalysisErrors) {
                    failMessage = exitCodeStr.toString();
                } else {
                    getLogger().warning("Analysis errors ignored as 'Fail on analysis errors' option unchecked");
                }
            }
        }

        AntPatternFileFinder finder = new AntPatternFileFinder(new String[]{"*" + CodeMetricConstants.XmlResultFile },
                new String[0], SystemInfo.isFileSystemCaseSensitive);

        try {
            File[] files = finder.findFiles(myOutputDirectory);
            myArtifactsWatcher.addNewArtifactsPath(new File(myOutputDirectory, "*.xml").getPath()
                    + "=>" + ArtifactsUtil.getInternalArtifactPath(CodeMetricConstants.CompressedMetricFile));
            myArtifactsWatcher.addNewArtifactsPath(new File(myOutputDirectory, "FullList.csv").getPath()
                    + "=>" + ArtifactsUtil.getInternalArtifactPath("FullList.zip"));
            myArtifactsWatcher.addNewArtifactsPath(new File(myOutputDirectory, "*.jsp").getPath()
                    + "=>" + ArtifactsUtil.getInternalArtifactPath(""));
            if (files.length == 0) {
                failMessage = "Output xml is not found";
            }
        } catch (IOException e) {
            e.printStackTrace();
            failMessage = "Output xml is not found";
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
        List<String > args = new Vector<String>();
        args.add("/c");
        args.add("echo");
        args.add("Finished running code metrics");
        return new SimpleProgramCommandLine(getBuildParameters().getEnvironmentVariables(),
                getCheckoutDirectory().getPath(), "cmd", args);
    }

    private List<File> matchFiles() throws Exception {
        final Map<String, String> runParameters = getRunnerParameters();

        final AntPatternFileFinder finder = new AntPatternFileFinder(
                splitFileWildcards(runParameters.get(SettingsKeys.Files)),
                splitFileWildcards(runParameters.get(SettingsKeys.FilesToExclude)),
                SystemInfo.isFileSystemCaseSensitive);
        final File[] files = finder.findFiles(getCheckoutDirectory());

        getLogger().logMessage(DefaultMessagesInfo.createTextMessage("Matched assembly files:"));

        String[] companyNames = splitFileWildcards(runParameters.get(SettingsKeys.CompanyName));
        final List<File> result = new ArrayList<File>(files.length);
        HashSet<String> fileNames = new HashSet<String>();
        for (File file : files) {
            if(fileNames.contains(file.getName())){
                continue;
            }
            fileNames.add(file.getName());
            if(companyNames.length > 0) {
                String compName = getCompanyName(file);
                boolean valid = isCompanyNameMatches(companyNames, compName);
                if(!valid){
                    getLogger().logMessage(
                            DefaultMessagesInfo.createTextMessage(
                                    "   Skipped as company name doesn't match the filter."));
                    continue;
                }
            }
            final String relativeName = FileUtil.getRelativePath(getWorkingDirectory(), file);

            result.add(file);
            getLogger().logMessage(DefaultMessagesInfo.createTextMessage("   Found a matching file: " + relativeName));
        }

        if (files.length == 0) {
            getLogger().logMessage(DefaultMessagesInfo.createTextMessage("  none"));
        }

        return result;
    }

    private boolean isCompanyNameMatches(String[] companyNames, String compName) {
        for(String expectedComp : companyNames){
            if(compName.toLowerCase().startsWith(expectedComp.toLowerCase())){
                return true;
            }
        }
        return false;
    }

    private String getCompanyName(File file) throws Exception{
        getLogger().message("Parsing company name for file: " + file.getAbsolutePath());
        String pluginDir = PathUtils.GetExecutionPath();
        String exe = FilenameUtils.concat(pluginDir, "CompanyNameParser.exe");
        ProcessBuilder pb = new ProcessBuilder(exe, "\"" + file.getCanonicalPath() + "\"");
        ProcessInvoker p = new ProcessInvoker(pb);
        int exitCode = p.invoke();
        if(exitCode != 0){
            getLogger().error(p.stdErr());
            throw new Exception("Unable to get company name for " + file.getAbsolutePath());
        }
        return p.stdOut();
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
