package com.nicologies.vscodemetrics.common;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

public class MetricsTransformer{
    private Logger _logger;

    public MetricsTransformer(Logger logger){

        this._logger = logger;
    }

    public void Transform(File codeMetricsXmlDirOrZip, File outputFile){
        try {
            String pluginDir = PathUtils.GetExecutionPath();
            String transformerExe = FilenameUtils.concat(pluginDir, "VsCodeMetricsTransformer.exe");

            ProcessBuilder pb = new ProcessBuilder(transformerExe,
                    "-i", codeMetricsXmlDirOrZip.getAbsolutePath(),
                    "-o", outputFile.getAbsolutePath());
            ProcessInvoker invoker = new ProcessInvoker(pb);
            int exitCode = invoker.invoke();
            if(exitCode != 0){
                _logger.error("Exception while generating html report " + invoker.stdErr());
            }
        } catch (Exception ex){
            _logger.error("Exception while generating html report "  + ex.getMessage() + Arrays.toString(ex.getStackTrace()));
        }
    }
}
