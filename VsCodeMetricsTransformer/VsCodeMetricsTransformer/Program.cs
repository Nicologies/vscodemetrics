using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Xml.Serialization;
using CommandLine;
using CommandLine.Text;
using MetricsDefinitions;
using Module = MetricsDefinitions.CodeMetricsReportTargetsTargetModulesModule;
using ClassType = MetricsDefinitions.CodeMetricsReportTargetsTargetModulesModuleNamespacesNamespaceTypesType;
using Member = MetricsDefinitions.CodeMetricsReportTargetsTargetModulesModuleNamespacesNamespaceTypesTypeMembersMember;
using MetricsStorage;

namespace VsCodeMetricsTransformer
{
    class Options
    {
        [Option('i', "input", Required = true,
            HelpText = "Input folder to be processed.")]
        public string InputFile { get; set; }

        [Option('o', "output", Required = true, HelpText = "output file name")]
        public string OutputFile { get; set; }

        [HelpOption]
        public string GetUsage()
        {
            return HelpText.AutoBuild(this,
              current => HelpText.DefaultParsingErrorsHandler(this, current));
        }
    }
    class Program
    {
        private static string _tempDirToUnzipMetricsResults;

        private static void Main(string[] args)
        {
            var options = new Options();
            if (!Parser.Default.ParseArguments(args, options))
            {
                Environment.Exit(1);
            }
            try
            {
                var rawModules = LoadRawMetricsFromFolder(options.InputFile);
                var transformedMetrics = new TransformedMetrics();
                TransformMetrics(rawModules, transformedMetrics);
                SaveFullListOfTheMetricsResults(options.OutputFile, transformedMetrics);
            }

            catch (Exception ex)
            {
                Console.Error.Write(ex.ToString());
                ExitProgram(exitCode: 2);
            }
            ExitProgram(exitCode: 0);
        }

        private static void SaveFullListOfTheMetricsResults(string outputFile, 
            TransformedMetrics transformedMetrics)
        {
            try
            {
                MetricsStorageHelper.Save(transformedMetrics, outputFile);
            }
            catch (Exception ex)
            {
                Console.Error.Write("Failed to transform metrics" + ex);
                ExitProgram(exitCode: 2);
            }
        }

        private static void ExitProgram(int exitCode)
        {
            if (Directory.Exists(_tempDirToUnzipMetricsResults))
            {
                Directory.Delete(_tempDirToUnzipMetricsResults, recursive: true);
            }
            Environment.Exit(exitCode);
        }

        private static void TransformMetrics(List<Module> rawModules, TransformedMetrics transformedMetrics)
        {
            if (rawModules == null) throw new ArgumentNullException(nameof(rawModules));
            foreach (var module in rawModules)
            {
                if (module.Metrics.Last().Value == "0")
                {
// line of code is 0
                    continue;
                }
                
                if (module.Namespaces.Any())
                {
                    foreach (var cls in module.Namespaces.SelectMany(n => n.Types))
                    {
                        foreach (var method in cls.Members.Where(m => !InIgnoreList(m, cls)))
                        {
                            var methodMetric = new MethodMetric()
                            {
                                Module = module.Name,
                                Class = cls.Name,
                                DepthOfInheritance = int.Parse(cls.Metrics[3].Value),
                                MethodName = method.Name,
                                MaintainabilityIndex = int.Parse(method.Metrics[0].Value),
                                CyclomaticComplexity = int.Parse(method.Metrics[1].Value),
                                ClassCoupling = int.Parse(method.Metrics[2].Value),
                                LinesOfCode = int.Parse(method.Metrics[3].Value),
                            };
                            transformedMetrics.Methods.Add(methodMetric);
                        }


                        
                    }
                }
                
            }
        }

        private static List<Module> LoadRawMetricsFromFolder(string metricResultDir)
        {
            var files = new DirectoryInfo(metricResultDir).EnumerateFiles("*VsCodeMetricsReport.xml");
            var rawModules = new List<Module>();
            foreach (var file in files)
            {
                try
                {
                    using (var stream = file.OpenRead())
                    {
                        var xmlSeri = new XmlSerializer(typeof (CodeMetricsReport));
                        var report = xmlSeri.Deserialize(stream) as CodeMetricsReport;
                        var target = report.Targets.FirstOrDefault();
                        var module = target?.Modules.FirstOrDefault();
                        if (module == null)
                        {
                            continue;
                        }
                        rawModules.Add(module);
                    }
                }
                catch (Exception ex)
                {
                    Console.Error.Write($"Failed to parse: {file.Name}" + ex);
                }
            }
            return rawModules;
        }

        private static bool InIgnoreList(Member m, ClassType cls)
        {
            if (m.Name == ("InitializeComponent() : void"))
            {
                return true;
            }
            var isNHibernateMapping = (m.Name.EndsWith("Mapping()") || m.Name.EndsWith("Mappings()"))
                && cls.Name == m.Name.Replace("()", "");
            return isNHibernateMapping;
        }
    }
}
