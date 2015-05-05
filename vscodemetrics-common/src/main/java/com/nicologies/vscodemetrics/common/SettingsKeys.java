package com.nicologies.vscodemetrics.common;

public interface SettingsKeys {
    String Files = "vscodemetrics.files";
    String FilesToExclude = "vscodemetrics.files_exclude";
    String AdditionalRefDir= "vscodemetrics.additional_ref_dir";
    String SearchInGac = "vscodemetrics.search_in_gac";
    String AdditionalOptions = "vscodemetrics.additional_options";
    String IgnoreGeneratedCode = "vscodemetrics.ignore_gen_code";
    String FailOnAnalysisError = "vscodemetrics.fail_on_analysis_error";
    String DetectionMode = "vscodemetrics.detection_mode";

    String RootProperty = "system.VsCodeMetricsRoot";
    String CmdFileVersionProperty = "system.vsCodeMetricsCmdFileVersion";

    String Root = "vscodemetrics.root";
    String CmdBinary = "metrics.exe";
    String Version = "vscodemetrics.version";
}
