package com.nicologies.vscodemetrics.common;

import jetbrains.buildServer.requirements.Requirement;
import jetbrains.buildServer.requirements.RequirementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum CodeMetricsVersion {

    not_specified("not_specified", "Any Detected") {
        @Override
        public Requirement createRequirement() {
            return new Requirement("system." + SettingsKeys.CmdFileVersionProperty, null, RequirementType.EXISTS);
        }
    },
    v12_0("12.0", "12.0"),
    v14_0("14.0", "14.0");

    private final String myTechnicalVersionPrefix;
    private final String myDisplayName;

    CodeMetricsVersion(final String technicalVersionPrefix, final String displayName) {
        myTechnicalVersionPrefix = technicalVersionPrefix;
        myDisplayName = displayName;
    }

    @NotNull
    public String getTechnicalVersionPrefix() {
        return myTechnicalVersionPrefix;
    }

    @NotNull
    public String getDisplayName() {
        return myDisplayName;
    }

    @Nullable
    public Requirement createRequirement() {
        return new Requirement("system." + SettingsKeys.CmdFileVersionProperty, getTechnicalVersionPrefix(), RequirementType.STARTS_WITH);
    }
}
