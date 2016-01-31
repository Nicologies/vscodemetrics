package com.nicologies.vscodemetrics;

import com.nicologies.vscodemetrics.common.SettingsValues;
import com.nicologies.vscodemetrics.common.SettingsKeys;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.util.PropertiesUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class RunTypePropertiesProcessor implements PropertiesProcessor {
    public Collection<InvalidProperty> process(Map<String, String> properties) {
        List<InvalidProperty> result = new Vector<InvalidProperty>();

        if (!properties.get(SettingsKeys.DetectionMode).equals(SettingsValues.AutoDetection)) {
            final String root = properties.get(SettingsKeys.Root);
            if (PropertiesUtil.isEmptyOrNull(root)) {
                result.add(new InvalidProperty(SettingsKeys.Root, "VisualStudio Code Metrics installation root must be specified"));
            }
        }

        if(PropertiesUtil.isEmptyOrNull(properties.get(SettingsKeys.Files))){
            result.add(new InvalidProperty(SettingsKeys.Files, "Files to analyze cannot be empty."));
        }

        return new Vector<InvalidProperty>();
    }
}
