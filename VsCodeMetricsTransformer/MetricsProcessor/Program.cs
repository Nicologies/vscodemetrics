using System;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Text;
using CommandLine;
using CommandLine.Text;
using MetricsDefinitions;

namespace MetricsProcessor
{
    class Options
    {
        [Option('i', "input", Required = true,
            HelpText = "Input zip file to be processed.")]
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
        private static string _tempDirToUnzipMetrics;

        static void Main(string[] args)
        {
            var options = new Options();
            if (!Parser.Default.ParseArguments(args, options))
            {
                Console.Error.Write("Invalid Parameters");
                Environment.Exit(1);
            }
            var metricsResult = options.InputFile;
            if (!UnzipMetrics(metricsResult))
            {
                ExitProgram(exitCode: 1);
            }
            var transformedMetrics= new TransformedMetrics();
            var metricsResultFile = Path.Combine(_tempDirToUnzipMetrics,
                Path.GetFileNameWithoutExtension(options.InputFile) + ".csv");
            using (var stream = new StreamReader(metricsResultFile))
            {
                using (var reader = new CsvHelper.CsvReader(stream))
                {
                    foreach (var methodMetricsInModule in reader.GetRecords<MethodMetric>().GroupBy(r => r.Module))
                    {
                        var moduleMetric = new ModuleMetric
                        {
                            Module = methodMetricsInModule.Key,
                            MaintainabilityIndex = 0,
                            CyclomaticComplexity = 0,
                            ClassCoupling = 0,
                            DepthOfInheritance = 0,
                            LinesOfCode = 0
                        };
                        var clsCount = 0;
                        foreach (var methodMetricInClass in methodMetricsInModule.GroupBy(r => r.Class))
                        {
                            ++clsCount;
                            var clsMetric = new ClassMetric()
                            {
                                Module = moduleMetric.Module,
                                Class = methodMetricInClass.Key,
                                MaintainabilityIndex = 0,
                                CyclomaticComplexity = 0,
                                ClassCoupling = 0,
                                DepthOfInheritance = 0,
                                LinesOfCode = 0
                            };

                            var memberCount = 0;
                            foreach (var methodMetric in methodMetricInClass)
                            {
                                ++memberCount;
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
                                clsMetric.DepthOfInheritance = methodMetricInClass.First().DepthOfInheritance;
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
            }
            var mainHtmlTemplate = new StringBuilder(Templates.GetMainHtmlTemplate());
            MetricsReporter.FillModuleMetrics(transformedMetrics, mainHtmlTemplate);
            MetricsReporter.FillWorstClasses(transformedMetrics, mainHtmlTemplate);
            MetricsReporter.FillWorstMethods(transformedMetrics, mainHtmlTemplate);
            if (!MetricsReporter.WriteToMetricsResult(options.OutputFile, mainHtmlTemplate))
            {
                ExitProgram(exitCode: 1);
            }

            ExitProgram(exitCode: 0);
        }

        private static void ExitProgram(int exitCode)
        {
            if (Directory.Exists(_tempDirToUnzipMetrics))
            {
                Directory.Delete(_tempDirToUnzipMetrics, recursive: true);
            }
            Environment.ExitCode = 1;
        }

        private static bool UnzipMetrics(string metricsResult)
        {
            try
            {
                _tempDirToUnzipMetrics = Path.Combine(Path.GetTempPath(), Path.GetRandomFileName());
                Directory.CreateDirectory(_tempDirToUnzipMetrics);
                ZipFile.ExtractToDirectory(metricsResult, _tempDirToUnzipMetrics);
                return true;
            }
            catch (Exception ex)
            {
                Console.Error.Write($"Unable to unzip {metricsResult}: {ex}");
            }
            return false;
        }
    }
}
