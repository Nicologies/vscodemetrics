package com.nicologies.vscodemetrics;

import com.nicologies.vscodemetrics.common.CodeMetricsVersion;
import com.nicologies.vscodemetrics.common.SettingsValues;
import com.nicologies.vscodemetrics.common.SettingsKeys;
import jetbrains.buildServer.requirements.Requirement;
import jetbrains.buildServer.requirements.RequirementType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RequirementsUtil {
    @NotNull
    public static List<Requirement> getMetricsRequirements(final Map<String, String> runParameters) {
        final List<Requirement> list = new ArrayList<Requirement>();
        final String detectionMode = runParameters.get(SettingsKeys.DetectionMode);
        if (detectionMode != null && detectionMode.equals(SettingsValues.AutoDetection)) {
            list.add(new Requirement("system." + SettingsKeys.RootProperty, null, RequirementType.EXISTS));

            final String specifiedMetricsVersion = runParameters.get(SettingsKeys.Version);
            if (specifiedMetricsVersion == null) {
                list.add(CodeMetricsVersion.not_specified.createRequirement());
            } else {
                for (CodeMetricsVersion version : CodeMetricsVersion.values()) {
                    if (version.getTechnicalVersionPrefix().equals(specifiedMetricsVersion)) {
                        final Requirement requirement = version.createRequirement();
                        if (requirement != null) list.add(requirement);
                        break;
                    }
                }
            }
        }
        return list;
    }
}
