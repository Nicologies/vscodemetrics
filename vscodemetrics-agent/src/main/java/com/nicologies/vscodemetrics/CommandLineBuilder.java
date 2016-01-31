package com.nicologies.vscodemetrics;

import com.nicologies.vscodemetrics.common.SettingsValues;
import com.nicologies.vscodemetrics.common.SettingsKeys;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.SimpleBuildLogger;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class CommandLineBuilder {
    private final Map<String, String> myRunParameters;
    private final Map<String, String> mySystemParameters;
    private final File myXmlReportFile;
    private final SimpleBuildLogger myLogger;

    public CommandLineBuilder(final Map<String, String> runnerParameters,
                              final Map<String, String> systemParameters,
                              final File xmlReportFile,
                              final SimpleBuildLogger logger) {
        myRunParameters = runnerParameters;
        mySystemParameters = systemParameters;
        myXmlReportFile = xmlReportFile;
        myLogger = logger;
    }

    @NotNull
    public String getExecutablePath() throws RunBuildException {
        String relativePathToMetricsExe;
        final String detectionMode = myRunParameters.get(SettingsKeys.DetectionMode);
        if (detectionMode.equals(SettingsValues.AutoDetection)) {
            relativePathToMetricsExe = mySystemParameters.get(SettingsKeys.RootProperty);
            myLogger.message("Used auto detected VisualStudio Code Metrics home directory");
        } else {
            relativePathToMetricsExe = myRunParameters.get(SettingsKeys.Root);
            myLogger.message("Used custom VisualStudio Code Metrics home directory");
        }

        if (StringUtil.isEmpty(relativePathToMetricsExe)) {
            throw new RunBuildException("Path to VisualStudio Code Metrics is not specified in build settings");
        }

        return new File(relativePathToMetricsExe, SettingsKeys.CmdBinary).getPath();
    }

    @NotNull
    public List<String> getArguments(List<String> files) throws RunBuildException {
        List<String> arguments = new Vector<String>();

        // Search in GAC
        if (isParameterEnabled(myRunParameters, SettingsKeys.SearchInGac)) {
            arguments.add("/gac");
        }

        // Ignore generated code
        if (isParameterEnabled(myRunParameters, SettingsKeys.IgnoreGeneratedCode)) {
            arguments.add("/ignoregeneratedcode");
        }

        // Search in dirs
        final String searchDirsString = myRunParameters.get(SettingsKeys.AdditionalRefDir);
        if (searchDirsString != null) {
            for (String file : StringUtil.splitCommandArgumentsAndUnquote(searchDirsString)) {
                arguments.add("/d:" + file);
            }
        }

        // Additional options
        final String additionalOptions = myRunParameters.get(SettingsKeys.AdditionalOptions);
        if (additionalOptions != null) {
            arguments.addAll(StringUtil.splitCommandArgumentsAndUnquote(additionalOptions));
        }

        // Files to be processed
        if (files != null) {
            for (String file : files) {
                arguments.add("/f:" + file);
            }
        }

        // Output file
        arguments.add("/out:" + myXmlReportFile.getPath());

        return arguments;
    }

    private static boolean isParameterEnabled(final Map<String, String> runParameters, final String key) {
        return runParameters.containsKey(key) && runParameters.get(key)
                .equals(Boolean.TRUE.toString());
    }
}
