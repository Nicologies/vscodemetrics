package com.nicologies.vscodemetrics;

import com.intellij.openapi.diagnostic.Logger;
import com.nicologies.vscodemetrics.common.ArtifactsUtil;
import com.nicologies.vscodemetrics.common.CodeMetricConstants;
import com.nicologies.vscodemetrics.common.PathUtils;
import com.nicologies.vscodemetrics.common.ProcessInvoker;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.reportTabs.ReportTabUtil;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

public class MetricsReducer extends BuildServerAdapter{
    private static final Logger LOG = jetbrains.buildServer.log.Loggers.SERVER;
    public MetricsReducer(SBuildServer server) {
        server.addListener(this);
    }

    private static HashSet<Long> _generatingReports = new HashSet<Long>();

    @Override
    public void buildArtifactsChanged(@NotNull final SBuild build) {
        boolean xmlResultReady = ReportTabUtil.isAvailable(build,
                ArtifactsUtil.getInternalArtifactPath(CodeMetricConstants.XmlResultFile));
        if(xmlResultReady && !_generatingReports.contains(build.getBuildId())){
            generateHtmlReportAsync(build);
        }
    }

    public static void generateHtmlReportAsync(final SBuild build){
        final long buildId = build.getBuildId();
        final File artifactsDir = build.getArtifactsDirectory();
        synchronized (_generatingReports) {
            _generatingReports.add(buildId);
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                generateHtmlReport(artifactsDir);
                synchronized (_generatingReports) {
                    _generatingReports.remove(buildId);
                }
            }
        });
        t.start();
    }
    private static void generateHtmlReport(File artifactDir){
        try {
            String pluginDir = PathUtils.GetExecutionPath();
            String transformerExe = FilenameUtils.concat(pluginDir, "VsCodeMetricsTransformer.exe");

            String xmlFullPath = new File(artifactDir, ArtifactsUtil.getInternalArtifactPath(CodeMetricConstants.XmlResultFile))
                    .getAbsolutePath();

            File myHtmlReportFile = new File(artifactDir,
                    ArtifactsUtil.getInternalArtifactPath(CodeMetricConstants.ReportFile));

            ProcessBuilder pb = new ProcessBuilder(transformerExe, xmlFullPath, myHtmlReportFile.getAbsolutePath());
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
