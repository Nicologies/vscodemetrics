package com.nicologies.vscodemetrics;

import com.nicologies.vscodemetrics.common.CodeMetricsVersion;
import com.nicologies.vscodemetrics.common.SettingsDefaultValues;
import com.nicologies.vscodemetrics.common.SettingsKeys;
import org.hsqldb.lib.Set;
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
        return SettingsDefaultValues.AutoDetection;
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
}
