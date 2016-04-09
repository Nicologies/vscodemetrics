using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Text;
using System.Xml.Serialization;
using CsvHelper;
using MetricsDefinitions;
using Module = MetricsDefinitions.CodeMetricsReportTargetsTargetModulesModule;
using ClassType = MetricsDefinitions.CodeMetricsReportTargetsTargetModulesModuleNamespacesNamespaceTypesType;
using Member = MetricsDefinitions.CodeMetricsReportTargetsTargetModulesModuleNamespacesNamespaceTypesTypeMembersMember;

namespace VsCodeMetricsTransformer
{
    class Program
    {
        private static string _tempDirToUnzipMetricsResults;

        private static void Main(string[] args)
        {
            VerifyArguments(args);
            try
            {
                var metricResultDir = ExtractMetricsResultsIfZipped(args[0]);
                var rawModules = LoadRawMetricsFromFolder(metricResultDir);
                var transformedMetrics = new TransformedMetrics();
                TransformMetrics(rawModules, transformedMetrics);
                var mainHtmlTemplate = new StringBuilder(Templates.GetMainHtmlTemplate());
                MetricsReporter.FillModuleMetrics(transformedMetrics, mainHtmlTemplate);
                MetricsReporter.FillWorstClasses(transformedMetrics, mainHtmlTemplate);
                MetricsReporter.FillWorstMethods(transformedMetrics, mainHtmlTemplate);
                var folderToSaveFile = args[1];
                if (!MetricsReporter.WriteToMetricsResult(folderToSaveFile, mainHtmlTemplate))
                {
                    ExitProgram(exitCode: 1);
                }
                SaveFullListOfTheMetricsResults(folderToSaveFile, transformedMetrics);
            }

            catch (Exception ex)
            {
                Console.Error.Write(ex.ToString());
                ExitProgram(exitCode: 2);
            }
            ExitProgram(exitCode: 0);
        }

        private static string ExtractMetricsResultsIfZipped(string metricsResultFileOrFolder)
        {
            _tempDirToUnzipMetricsResults = Path.Combine(Path.GetTempPath(), Path.GetRandomFileName());
            Directory.CreateDirectory(_tempDirToUnzipMetricsResults);
            var metricResultDir = _tempDirToUnzipMetricsResults;
            if (metricsResultFileOrFolder.EndsWith(".zip"))
            {
                ZipFile.ExtractToDirectory(metricsResultFileOrFolder, _tempDirToUnzipMetricsResults);
            }
            else
            {
                metricResultDir = metricsResultFileOrFolder;
            }
            return metricResultDir;
        }

        private static void VerifyArguments(string[] args)
        {
            if (args.Length != 2)
            {
                Environment.Exit(-1);
            }
        }

        private static void SaveFullListOfTheMetricsResults(string folderToSaveFile, 
            TransformedMetrics transformedMetrics)
        {
            try
            {
                var dir = Path.GetDirectoryName(folderToSaveFile);
                var csv = Path.Combine(dir, "FullList.csv");
                using (var stream = new StreamWriter(csv))
                {
                    var writer = new CsvWriter(stream);
                    writer.WriteHeader<MethodMetric>();
                    foreach (var method in transformedMetrics.Methods)
                    {
                        writer.WriteRecord(method);
                    }
                }
            }
            catch (Exception ex)
            {
                Console.Error.Write("Failed to save full list csv" + ex);
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

        private static void TransformMetrics(List<CodeMetricsReportTargetsTargetModulesModule> rawModules, TransformedMetrics transformedMetrics)
        {
            foreach (var module in rawModules)
            {
                if (module.Metrics.Last().Value == "0")
                {
// line of code is 0
                    continue;
                }
                var moduleMetric = new ModuleMetric
                {
                    Module = module.Name,
                    MaintainabilityIndex = 0,
                    CyclomaticComplexity = 0,
                    ClassCoupling = 0,
                    DepthOfInheritance = 0,
                    LinesOfCode = 0
                };
                var clsCount = 0;
                if (module.Namespaces.Any())
                {
                    foreach (var cls in module.Namespaces.SelectMany(n => n.Types))
                    {
                        ++clsCount;
                        var clsMetric = new ClassMetric()
                        {
                            Module = module.Name,
                            Class = cls.Name,
                            MaintainabilityIndex = 0,
                            CyclomaticComplexity = 0,
                            ClassCoupling = 0,
                            DepthOfInheritance = int.Parse(cls.Metrics[3].Value),
                            LinesOfCode = 0
                        };

                        var memberCount = 0;
                        foreach (var method in cls.Members.Where(m => !InIgnoreList(m, cls)))
                        {
                            memberCount++;
                            var methodMetric = new MethodMetric()
                            {
                                Module = module.Name,
                                Class = cls.Name,
                                DepthOfInheritance = clsMetric.DepthOfInheritance,
                                MethodName = method.Name,
                                MaintainabilityIndex = int.Parse(method.Metrics[0].Value),
                                CyclomaticComplexity = int.Parse(method.Metrics[1].Value),
                                ClassCoupling = int.Parse(method.Metrics[2].Value),
                                LinesOfCode = int.Parse(method.Metrics[3].Value),
                            };
                            transformedMetrics.Methods.Add(methodMetric);
                            clsMetric.MaintainabilityIndex += methodMetric.MaintainabilityIndex;
                            clsMetric.CyclomaticComplexity += methodMetric.CyclomaticComplexity;
                            clsMetric.ClassCoupling += methodMetric.ClassCoupling;
                            clsMetric.LinesOfCode += methodMetric.LinesOfCode;
                        }

                        if (memberCount != 0)
                        {
                            clsMetric.MaintainabilityIndex /= memberCount;
                            clsMetric.CyclomaticComplexity /= memberCount;
                            clsMetric.ClassCoupling /= memberCount;
                        }
                        else
                        {
                            clsMetric.MaintainabilityIndex = 100;
                        }
                        transformedMetrics.Classes.Add(clsMetric);

                        moduleMetric.MaintainabilityIndex += clsMetric.MaintainabilityIndex;
                        moduleMetric.ClassCoupling += clsMetric.ClassCoupling;
                        moduleMetric.CyclomaticComplexity += clsMetric.CyclomaticComplexity;
                        moduleMetric.DepthOfInheritance += clsMetric.DepthOfInheritance;
                        moduleMetric.LinesOfCode += clsMetric.LinesOfCode;
                    }
                }
                if (clsCount != 0)
                {
                    moduleMetric.MaintainabilityIndex /= clsCount;
                    moduleMetric.ClassCoupling /= clsCount;
                    moduleMetric.CyclomaticComplexity /= clsCount;
                    moduleMetric.DepthOfInheritance /= clsCount;
                }
                else
                {
                    moduleMetric.MaintainabilityIndex = 100;
                }
                transformedMetrics.Modules.Add(moduleMetric);
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
