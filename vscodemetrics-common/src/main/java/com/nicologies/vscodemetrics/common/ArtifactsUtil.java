package com.nicologies.vscodemetrics.common;

public class ArtifactsUtil {
    public static String getInternalArtifactPath(final String relativePath) {
        return String.format("%s/%s", getInternalArtifactFolder(), relativePath);
    }
    public static String getInternalArtifactFolder(){
        return String.format("%s/%s", ".teamcity", CodeMetricConstants.RunnerType);
    }
}
