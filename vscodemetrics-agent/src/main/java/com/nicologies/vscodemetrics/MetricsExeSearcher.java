package com.nicologies.vscodemetrics;

import com.nicologies.vscodemetrics.common.SettingsKeys;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.util.PEReader.PEUtil;
import jetbrains.buildServer.util.PEReader.PEVersion;
import jetbrains.buildServer.util.StringUtil;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class MetricsExeSearcher {

    @NotNull
    private static final Logger LOG = Logger.getLogger(MetricsExeSearcher.class);

    private static final Collection<String> KNOWN_VS_PATH_PARAM_NAMES = Arrays.asList("VS2015_Path", "VS2013_Path");

    @NotNull
    public static final String FXCOP_RELATIVE_PATH = "..\\..\\Team Tools\\Static Analysis Tools\\FxCop\\";

    @NotNull
    public static final String METRICS_EXE_RELATIVE_PATH = FXCOP_RELATIVE_PATH + SettingsKeys.CmdBinary;

    public MetricsExeSearcher() {
    }

    public void search(@NotNull final BuildAgentConfiguration config) {
        //TODO: introduce .net properties searcher in open api and use it here
        if (!config.getSystemInfo().isWindows()) return;

        // search config params
        String metricsExePath = config.getBuildParameters().getSystemProperties().get(SettingsKeys.RootProperty);

        if (StringUtil.isEmptyOrSpaces(metricsExePath)) {
            final Map<String, String> configurationParameters = config.getConfigurationParameters();
            for (String paramName : KNOWN_VS_PATH_PARAM_NAMES) {
                if (!StringUtil.isEmptyOrSpaces(metricsExePath)) continue;
                try {
                    metricsExePath = searchCodeMetricsInVSInstallation(configurationParameters, paramName);
                } catch (IOException e) {
                    LOG.warn("Error while searching for Metrics.exe: " + e.toString());
                    LOG.debug("Error while searching for Metrics.exe", e);
                }
            }
        }

        if (StringUtil.isNotEmpty(metricsExePath)) {
            setupEnvironment(config, metricsExePath);
        }
    }

    private void setupEnvironment(final BuildAgentConfiguration config, final String metricsDir) {
        final File metricsExeCmd = new File(metricsDir, SettingsKeys.CmdBinary);
        PEVersion fileVersion = PEUtil.getFileVersion(metricsExeCmd);
        if (fileVersion != null) {
            config.addSystemProperty(SettingsKeys.CmdFileVersionProperty, fileVersion.toString());
            LOG.info("Found Metrics file version: " + fileVersion);
        }
        config.addSystemProperty(SettingsKeys.RootProperty, metricsDir);
    }

    @Nullable
    private String searchCodeMetricsInVSInstallation(@NotNull final Map<String, String> configurationParameters, @NotNull final String vsPathParamName) throws IOException {
        if (!configurationParameters.containsKey(vsPathParamName)) {
            LOG.info(vsPathParamName + " configuration parameter was not found");
            return null;
        }
        final String vsPath = configurationParameters.get(vsPathParamName);
        if (vsPath == null || StringUtil.isEmptyOrSpaces(vsPath)) {
            LOG.info(vsPathParamName + " configuration parameter value is empty");
            return null;
        }
        final File devenvExeHome = new File(vsPath);
        if (!devenvExeHome.exists()) {
            LOG.warn("VS home directory was found in the agent configuration but it does not exist on disk at path: \"" + devenvExeHome.getAbsolutePath() + "\"");
            return null;
        }
        final File metricsExe = new File(devenvExeHome, METRICS_EXE_RELATIVE_PATH);
        if (!metricsExe.exists()) {
            LOG.info("Metrics.exe was not found in VS installation directory at path: \"" + metricsExe.getAbsolutePath() + "\"");
            return null;
        }
        LOG.info("Metrics.exe found at path \"" + metricsExe.getAbsolutePath() + "\"");
        return metricsExe.getParentFile().getCanonicalPath();
    }
}
