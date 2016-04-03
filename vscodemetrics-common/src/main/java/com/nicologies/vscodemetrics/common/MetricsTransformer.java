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

    private static HashSet<Long> _generatingReports = new HashSet<Long>();

    public void generateHtmlReportAsync(final long buildId, final File codeMetricsXmlDir){
        synchronized (_generatingReports) {
            _generatingReports.add(buildId);
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                generateHtmlReport(codeMetricsXmlDir);
                synchronized (_generatingReports) {
                    _generatingReports.remove(buildId);
                }
            }
        });
        t.start();
    }
    public void generateHtmlReport(File codeMetricsXmlDir){
        try {
            String pluginDir = PathUtils.GetExecutionPath();
            String transformerExe = FilenameUtils.concat(pluginDir, "VsCodeMetricsTransformer.exe");

            File myHtmlReportFile = new File(codeMetricsXmlDir, CodeMetricConstants.ReportFile);

            ProcessBuilder pb = new ProcessBuilder(transformerExe, codeMetricsXmlDir.getAbsolutePath(),
                    myHtmlReportFile.getAbsolutePath());
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
