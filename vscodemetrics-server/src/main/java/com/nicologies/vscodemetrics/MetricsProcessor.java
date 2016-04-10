package com.nicologies.vscodemetrics;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.nicologies.vscodemetrics.common.ArtifactsUtil;
import com.nicologies.vscodemetrics.common.CodeMetricConstants;
import com.nicologies.vscodemetrics.common.PathUtils;
import com.nicologies.vscodemetrics.common.ProcessInvoker;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.AntPatternFileFinder;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;

public class MetricsProcessor extends BuildServerAdapter {
    private static final Logger LOG = jetbrains.buildServer.log.Loggers.SERVER;
    private final SBuildServer _server;
    private ServerPaths _serverPaths;

    public MetricsProcessor(SBuildServer server, ServerPaths serverPaths) {
        _serverPaths = serverPaths;
        server.addListener(this);
        _server = server;
    }

    private static HashSet<Long> _generatingReports = new HashSet<Long>();

    public void ProcessAsync(final SBuild build) {
        final long buildId = build.getBuildId();
        final File artifactsDir = build.getArtifactsDirectory();
        final String curBranch = build.getBranch().getName();
        SBuild previousBuild = _server.findPreviousBuild(build, new BuildDataFilter(){
            @Override
            public boolean accept(@NotNull SBuild sBuild) {
                return !sBuild.isPersonal() && sBuild.isFinished() && sBuild.getBranch().getName().equals(curBranch);
            }
        });
        File previousBuildMetricsZip = null;
        if(previousBuild != null) {
            previousBuildMetricsZip = GetMetricsPath(previousBuild);
        }
        final File finalPreviousBuildMetricsZip = previousBuildMetricsZip;

        synchronized (_generatingReports) {
            _generatingReports.add(buildId);
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                File metricsZipFile = new File(artifactsDir,
                        ArtifactsUtil.getInternalArtifactPath(CodeMetricConstants.CompressedMetricsFile));
                File myHtmlReportFile = new File(artifactsDir,
                        ArtifactsUtil.getInternalArtifactPath(CodeMetricConstants.ReportFile));
                GenReport(metricsZipFile, myHtmlReportFile, finalPreviousBuildMetricsZip);

                File copyTo = GetMetricsPath(build);
                try {
                    Files.copy(metricsZipFile.toPath(), copyTo.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (_generatingReports) {
                    _generatingReports.remove(buildId);
                }
            }
        });
        t.start();
    }

    @Override
    public void buildFinished(@NotNull final SRunningBuild build) {
        LOG.info("Build is finished");
        AntPatternFileFinder finder = new AntPatternFileFinder(new String[]{CodeMetricConstants.CompressedMetricsFile},
                new String[0], SystemInfo.isFileSystemCaseSensitive);
        boolean xmlResultReady = false;
        try {
            File[] files = finder.findFiles(new File(build.getArtifactsDirectory(), ArtifactsUtil.getInternalArtifactPath("")));
            xmlResultReady = files.length > 0;
        } catch (IOException e) {
            e.printStackTrace();
            LOG.error("Unable to get code analysis result", e);
        }
        if (xmlResultReady && !_generatingReports.contains(build.getBuildId())) {
            ProcessAsync(build);
        }
    }

    private static void GenReport(File codeMetricsXmlDirOrZip, File outputFile, File previousMetricsZipFile){
        try {
            String pluginDir = PathUtils.GetExecutionPath();
            String transformerExe = FilenameUtils.concat(pluginDir, "MetricsProcessor.exe");

            ProcessBuilder pb;
            if(previousMetricsZipFile != null) {
                pb = new ProcessBuilder(transformerExe,
                        "-i", codeMetricsXmlDirOrZip.getAbsolutePath(),
                        "-o", outputFile.getAbsolutePath(),
                        "-p", previousMetricsZipFile.getAbsolutePath());
            }else{
                pb = new ProcessBuilder(transformerExe,
                        "-i", codeMetricsXmlDirOrZip.getAbsolutePath(),
                        "-o", outputFile.getAbsolutePath());
            }
            ProcessInvoker invoker = new ProcessInvoker(pb);
            int exitCode = invoker.invoke();
            if(exitCode != 0){
                LOG.error("Exception while generating html report " + invoker.stdErr());
            }
        } catch (Exception ex){
            LOG.error("Exception while generating html report "  + ex.getMessage() + Arrays.toString(ex.getStackTrace()));
        }
    }

    private File GetMetricsPath(@NotNull SBuild build){
        File pluginDataFolder = PathUtils.GetPluginDataFolder(_serverPaths.getPluginDataDirectory(),
                build.getBranch().getName(), build.getBuildId());
        if(!pluginDataFolder.exists()){
            pluginDataFolder.mkdirs();
        }
        return new File(pluginDataFolder, CodeMetricConstants.CompressedMetricsFile);
    }
}
