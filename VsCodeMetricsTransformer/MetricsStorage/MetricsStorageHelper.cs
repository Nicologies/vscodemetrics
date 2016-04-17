using System;
using System.IO;
using System.IO.Compression;
using System.Linq;
using CsvHelper;
using MetricsDefinitions;

namespace MetricsStorage
{
    public class MetricsStorageHelper
    {
        public static void Save(TransformedMetrics metrics, string zipFilePath)
        {
            using (var stream = new StreamWriter(zipFilePath))
            {
                var writer = new CsvWriter(stream);
                writer.WriteHeader<MethodMetric>();
                foreach (var method in metrics.Methods)
                {
                    writer.WriteRecord(method);
                }
            }
        }
        public static TransformedMetrics LoadMetrics(string metricsZipFile)
        {
            var tempDirToUnzipMetrics = Path.Combine(Path.GetTempPath(), Path.GetRandomFileName());
            Directory.CreateDirectory(tempDirToUnzipMetrics);
            try
            {
                if (!UnzipMetrics(metricsZipFile, tempDirToUnzipMetrics))
                {
                    return null;
                }
                var transformedMetrics = new TransformedMetrics();
                var metricsResultFile = Path.Combine(tempDirToUnzipMetrics,
                    Path.GetFileNameWithoutExtension(metricsZipFile) + ".csv");
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
                return transformedMetrics;
            }
            finally
            {
                Directory.Delete(tempDirToUnzipMetrics, recursive: true);
            }
        }

        private static bool UnzipMetrics(string metricsResult, string unZipTo)
        {
            try
            {
                ZipFile.ExtractToDirectory(metricsResult, unZipTo);
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
