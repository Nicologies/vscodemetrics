package com.nicologies.vscodemetrics;

import com.nicologies.vscodemetrics.common.CodeMetricConstants;
import com.nicologies.vscodemetrics.common.CodeMetricsVersion;
import com.nicologies.vscodemetrics.common.SettingsDefaultValues;
import com.nicologies.vscodemetrics.common.SettingsKeys;
import jetbrains.buildServer.requirements.Requirement;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeMetricsRunType extends jetbrains.buildServer.serverSide.RunType{
    private final PluginDescriptor _pluginDescriptor;
    public CodeMetricsRunType(final RunTypeRegistry runTypeRegistry, final PluginDescriptor pluginDescriptor){
        _pluginDescriptor = pluginDescriptor;
        runTypeRegistry.registerRunType(this);
    }
    @NotNull
    @Override
    public String getType() {
        return CodeMetricConstants.RunnerType;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Visual Studio Code Metrics";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Visual Studio Code Metrics Plugin";
    }

    @Nullable
    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {
        return new RunTypePropertiesProcessor();
    }

    @Nullable
    @Override
    public String getEditRunnerParamsJspFilePath() {
        return _pluginDescriptor.getPluginResourcesPath("editVsCodeMetricsParams.jsp");
    }

    @Nullable
    @Override
    public String getViewRunnerParamsJspFilePath() {
        return _pluginDescriptor.getPluginResourcesPath("viewVsCodeMetricsParams.jsp");
    }

    @Nullable
    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(SettingsKeys.Files, SettingsDefaultValues.FilesToInclude);
        parameters.put(SettingsKeys.FilesToExclude, SettingsDefaultValues.FilesToExclude);
        parameters.put(SettingsKeys.AdditionalOptions, "");
        parameters.put(SettingsKeys.AdditionalRefDir, "");
        parameters.put(SettingsKeys.IgnoreGeneratedCode, "true");
        parameters.put(SettingsKeys.SearchInGac, "true");
        parameters.put(SettingsKeys.DetectionMode, SettingsDefaultValues.AutoDetection);
        parameters.put(SettingsKeys.Version, CodeMetricsVersion.not_specified.getDisplayName());
        return parameters;
    }
    @NotNull
    @Override
    public List<Requirement> getRunnerSpecificRequirements(@NotNull final Map<String, String> runParameters) {
        return RequirementsUtil.getFxCopRequirements(runParameters);
    }
}
