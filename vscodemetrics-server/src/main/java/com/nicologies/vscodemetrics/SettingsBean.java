package com.nicologies.vscodemetrics;

import com.nicologies.vscodemetrics.common.CodeMetricsVersion;
import com.nicologies.vscodemetrics.common.SettingsValues;
import com.nicologies.vscodemetrics.common.SettingsKeys;
import org.jetbrains.annotations.NotNull;

public class SettingsBean {
    @NotNull
    public String getFilesKey() {
        return SettingsKeys.Files;
    }

    @NotNull
    public String getFilesExcludeKey() {
        return SettingsKeys.FilesToExclude;
    }

    @NotNull
    public String getSearchDirsKey() {
        return SettingsKeys.AdditionalRefDir;
    }

    @NotNull
    public String getSearchInGacKey() {
        return SettingsKeys.SearchInGac;
    }

    @NotNull
    public String getAdditionalOptionsKey() {
        return SettingsKeys.AdditionalOptions;
    }

    @NotNull
    public String getIgnoreGeneratedCodeKey() {
        return SettingsKeys.IgnoreGeneratedCode;
    }

    @NotNull
    public String getDetectionModeKey() {
        return SettingsKeys.DetectionMode;
    }

    @NotNull
    public String getDetectionModeAuto() {
        return SettingsValues.AutoDetection;
    }

    @NotNull
    public String getRootKey() {
        return SettingsKeys.Root;
    }

    @NotNull
    public String getVersionKey() {
        return SettingsKeys.Version;
    }

    @NotNull
    public CodeMetricsVersion[] getAvailableVersions() {
        return CodeMetricsVersion.values();
    }

    @NotNull
    public String getFailOnAnalysisErrorKey() {
        return SettingsKeys.FailOnAnalysisError;
    }

    @NotNull
    public String getDetectionModeManual() {
        return SettingsValues.ManualDetection;
    }
    @NotNull
    public String getAddtionalOptionsKey() {
        return SettingsKeys.AdditionalOptions;
    }

    @NotNull
    public String getCompanyNameKey(){
        return SettingsKeys.CompanyName;
    }
}
