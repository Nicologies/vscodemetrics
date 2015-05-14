package com.nicologies.vscodemetrics;

import com.nicologies.vscodemetrics.common.ArtifactsUtil;
import com.nicologies.vscodemetrics.common.CodeMetricConstants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.ViewLogTab;
import jetbrains.buildServer.web.reportTabs.ReportTabUtil;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class CodeMetricsTab extends ViewLogTab {
    public CodeMetricsTab(@NotNull PagePlaces pagePlaces,
                          @NotNull SBuildServer server){
        super("VS Code Metrics", "com.nicologies.vscodemetrics", pagePlaces, server);
        setIncludeUrl("/artifactsViewer.jsp");
    }

    @Override
    protected void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest httpServletRequest,
                             @NotNull SBuild build) {
        model.put("startPage", getAvailableReportPage(build));
    }

    @Override
    protected boolean isAvailable(@NotNull HttpServletRequest request, @NotNull SBuild build) {
        return super.isAvailable(request, build) &&
                ReportTabUtil.isAvailable(build, ArtifactsUtil.getInternalArtifactPath(CodeMetricConstants.ReportFile));
    }

    private String getAvailableReportPage(final SBuild build) {
        final String internalArtifactPath = ArtifactsUtil.getInternalArtifactPath(CodeMetricConstants.ReportFile);
        return ReportTabUtil.isAvailable(build, internalArtifactPath) ? internalArtifactPath : CodeMetricConstants.ReportFile;
    }
}