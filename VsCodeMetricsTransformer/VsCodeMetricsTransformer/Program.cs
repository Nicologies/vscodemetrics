using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
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
                using (var stream = File.OpenRead(args[0]))
                {
                    var xmlSeri = new XmlSerializer(typeof (CodeMetricsReport));
                    var report = xmlSeri.Deserialize(stream) as CodeMetricsReport;
                    try
                    {
                        using (var output = new StreamWriter(args[1]))
                        {
                            AmendOverallMetricsToBeAverage(report);
                            const string metricstemplate = "metricsTemplate";
                            output.Write(Engine.Razor.RunCompile(template, metricstemplate,
                                typeof (CodeMetricsReport),
                                report));
                        }
                    }
                    catch (Exception ex)
                    {
                        Console.Error.Write("Failed to render html" + ex);
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
