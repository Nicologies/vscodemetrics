package com.nicologies.vscodemetrics;

import com.intellij.openapi.diagnostic.Logger;
import com.nicologies.vscodemetrics.common.ArtifactsUtil;
import com.nicologies.vscodemetrics.common.CodeMetricConstants;
import com.nicologies.vscodemetrics.common.MetricsTransformer;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.ViewLogTab;
import jetbrains.buildServer.web.reportTabs.ReportTabUtil;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CodeMetricsTab extends ViewLogTab {
    private static final Logger LOG = jetbrains.buildServer.log.Loggers.SERVER;
    private PluginDescriptor _pluginDescriptor;
    public CodeMetricsTab(@NotNull PagePlaces pagePlaces,
                          @NotNull SBuildServer server,
                          @NotNull PluginDescriptor pluginDescriptor){
        super("Code Metrics", "com.nicologies.vscodemetrics", pagePlaces, server);
        _pluginDescriptor = pluginDescriptor;
        setIncludeUrl(_pluginDescriptor.getPluginResourcesPath("/metricsView.jsp"));
    }

    @Override
    protected void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest httpServletRequest,
                             @NotNull SBuild build) {
        if(isHtmlAvailable(build)) {
            File filePath = new File(build.getArtifactsDirectory(), getAvailableReportPage(build));
            String fileContent = null;
            try{
                fileContent = new String(Files.readAllBytes(filePath.toPath()), StandardCharsets.UTF_8);

                String currentFolder = "/repository/download/" + build.getBuildTypeExternalId().toString()
                        +"/" + build.getBuildId() + ":id/" + ArtifactsUtil.getInternalArtifactFolder();

                fileContent = fileContent
                        .replace("${teamcityPluginResourcesPath}", _pluginDescriptor.getPluginResourcesPath())
                        .replace("${currentFolder}", currentFolder);
            } catch (IOException e) {
                LOG.error("unable to read file", e);
            }
            model.put("html", fileContent);
        }

        else{
            model.put("notReady", "<div class=\"attentionComment\" style=\"margin-bottom: 10px\">\n" +
                    "      This page is not automatically refreshed while the build is running.\n" +
                    "      <a class=\"btn btn_mini\" onclick=\"$('buildResults').refresh(null, 'runningBuildRefresh=1'); return false\" href=\"#\">Refresh</a>\n" +
                    "    </div>");// _pluginDescriptor.getPluginResourcesPath("/notReady.jsp"));
        }
    }

    private boolean isHtmlAvailable(@NotNull SBuild build) {
        return ReportTabUtil.isAvailable(build, ArtifactsUtil.getInternalArtifactPath(CodeMetricConstants.ReportFile));
    }

    private String getAvailableReportPage(final SBuild build) {
        return ArtifactsUtil.getInternalArtifactPath(CodeMetricConstants.ReportFile);
    }
    public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }
    protected boolean isAvailable(@NotNull final HttpServletRequest request, @NotNull final SBuild build) {
        Boolean available = isHtmlAvailable(build);
        if(!available) {
            Date finishedDate = build.getFinishDate();
            if (finishedDate != null && getDateDiff(finishedDate, new Date(), TimeUnit.MINUTES) > 1) {
                MetricsTransformer transformer = new MetricsTransformer(jetbrains.buildServer.log.Loggers.SERVER);
                File codeMetricsXmlDir = new File(build.getArtifactsDirectory(), ArtifactsUtil.getInternalArtifactPath(""));
                transformer.generateHtmlReportAsync(build.getBuildId(), codeMetricsXmlDir);
            }
        }
        return available;
    }
}