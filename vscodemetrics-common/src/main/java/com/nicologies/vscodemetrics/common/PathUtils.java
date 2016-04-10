package com.nicologies.vscodemetrics.common;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;

public class PathUtils {
    public static String GetExecutionPath() throws URISyntaxException {
        File jar = new File(PathUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        return jar.getParent();
    }

    public static File GetPluginDataFolder(File pluginDataRootFolder, String branchName, Long buildId){
        return Paths.get(pluginDataRootFolder.getAbsolutePath(), "VsCodeMetrics",
                branchName.replace("<", "").replace(">", ""),
                Long.toString(buildId)).toFile();
    }
}
