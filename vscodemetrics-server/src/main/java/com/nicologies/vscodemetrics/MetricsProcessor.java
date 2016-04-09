package com.nicologies.vscodemetrics;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.nicologies.vscodemetrics.common.ArtifactsUtil;
import com.nicologies.vscodemetrics.common.CodeMetricConstants;
import com.nicologies.vscodemetrics.common.PathUtils;
import com.nicologies.vscodemetrics.common.ProcessInvoker;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.util.AntPatternFileFinder;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

public class MetricsProcessor extends BuildServerAdapter {
    private static final Logger LOG = jetbrains.buildServer.log.Loggers.SERVER;

    public MetricsProcessor(SBuildServer server) {
        server.addListener(this);
    }

    private static HashSet<Long> _generatingReports = new HashSet<Long>();

    public static void ProcessAsync(final SBuild build) {
        final long buildId = build.getBuildId();
        final File artifactsDir = build.getArtifactsDirectory();
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
                GenReport(metricsZipFile, myHtmlReportFile);
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

    private static void GenReport(File codeMetricsXmlDirOrZip, File outputFile){
        try {
            String pluginDir = PathUtils.GetExecutionPath();
            String transformerExe = FilenameUtils.concat(pluginDir, "MetricsProcessor.exe");

            ProcessBuilder pb = new ProcessBuilder(transformerExe,
                    "-i", codeMetricsXmlDirOrZip.getAbsolutePath(),
                    "-o", outputFile.getAbsolutePath());
            ProcessInvoker invoker = new ProcessInvoker(pb);
            int exitCode = invoker.invoke();
            if(exitCode != 0){
                LOG.error("Exception while generating html report " + invoker.stdErr());
            }
        } catch (Exception ex){
            LOG.error("Exception while generating html report "  + ex.getMessage() + Arrays.toString(ex.getStackTrace()));
        }
    }
}
