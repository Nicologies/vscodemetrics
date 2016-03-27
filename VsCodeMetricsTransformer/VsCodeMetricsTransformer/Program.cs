using System;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Xml.Serialization;
using RazorEngine;
using RazorEngine.Templating;
using RawMetric = VsCodeMetricsTransformer.MetricsMetric;
using Target = VsCodeMetricsTransformer.CodeMetricsReportTargetsTarget;
using Module = VsCodeMetricsTransformer.CodeMetricsReportTargetsTargetModulesModule;
using Namespace = VsCodeMetricsTransformer.CodeMetricsReportTargetsTargetModulesModuleNamespacesNamespace;
using Namespaces = VsCodeMetricsTransformer.CodeMetricsReportTargetsTargetModulesModuleNamespacesNamespace;
using ClassType = VsCodeMetricsTransformer.CodeMetricsReportTargetsTargetModulesModuleNamespacesNamespaceTypesType;
using Member = VsCodeMetricsTransformer.CodeMetricsReportTargetsTargetModulesModuleNamespacesNamespaceTypesTypeMembersMember;

namespace VsCodeMetricsTransformer
{
    class Program
    {
        private static void Main(string[] args)
        {
            if (args.Length != 2)
            {
                Environment.Exit(-1);
            }
            try
            {
                var assemblyLoc = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location);
                var template = File.ReadAllText(
                    Path.Combine(assemblyLoc, "MetricsTemplate.cshtml"));
                var files = new DirectoryInfo(args[0]).EnumerateFiles("*VsCodeMetricsReport.xml");
                var combinedRpt = new CodeMetricsReport();
                foreach (var file in files)
                {
                    try
                    {
                        using (var stream = file.OpenRead())
                        {
                            var xmlSeri = new XmlSerializer(typeof (CodeMetricsReport));
                            var report = xmlSeri.Deserialize(stream) as CodeMetricsReport;
                            combinedRpt.Targets.AddRange(report.Targets);
                        }
                    }
                    catch (Exception ex)
                    {
                        Console.Error.Write($"Failed to parse: {file.Name}" + ex);
                    }
                }
                try
                {
                    using (var output = new StreamWriter(args[1]))
                    {
                        const string metricstemplate = "metricsTemplate";
                        output.Write(Engine.Razor.RunCompile(template, metricstemplate,
                            typeof(CodeMetricsReport),
                            combinedRpt));
                    }
                }
                catch (Exception ex)
                {
                    Console.Error.Write("Failed to save metrics data" + ex);
                    try
                    {
                        File.Delete(args[1]);
                    }
                    catch
                    {
                        // ignored
                    }
                    Environment.Exit(-2);
                }
            }

            catch (Exception ex)
            {
                Console.Error.Write(ex.ToString());
                Environment.Exit(-2);
            }
        }

        private static void AmendOverallMetricsToBeAverage(CodeMetricsReport report)
        {
            foreach (var target in report.Targets)
            {
                foreach (var module in target.Modules)
                {
                    var clsCount = module.Namespaces.SelectMany(r => r.Types).Count();
                    foreach (var metric in module.Metrics)
                    {
                        if (metric.Name == "CyclomaticComplexity")
                        {
                            metric.Value = (Convert.ToDouble(metric.Value)/clsCount).ToString("0.00");
                        }
                        else if (metric.Name == "ClassCoupling")
                        {
                            metric.Value = (Convert.ToDouble(metric.Value)/clsCount).ToString("0.00");
                        }
                    }
                }
            }
        }
    }
}
