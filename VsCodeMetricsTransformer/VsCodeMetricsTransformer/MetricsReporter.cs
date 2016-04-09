using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using MetricsDefinitions;

namespace VsCodeMetricsTransformer
{
    class MetricsReporter
    {
        public static void FillWorstMethods(TransformedMetrics transformedMetrics, StringBuilder template)
        {
            var worstMethods = transformedMetrics.Methods.OrderBy(c => c.MaintainabilityIndex).Take(100).ToList();
            var tableOfWorstMethods = new StringBuilder(Templates.TableHeaderForMethod);
            foreach (var method in worstMethods)
            {
                var row = Templates.RowTemplateForMethod.Inject(method.FormatDecimalPoints());
                tableOfWorstMethods.AppendLine(row);
            }

            var worstMethodsAreInModules = worstMethods.GroupBy(r => r.Module)
                .OrderByDescending(x => x.Count()).Take(5)
                .Select(x => $"<li>{x.Key}: {x.Count()} methods</li>");
            var strWorstMethodsAreInModules = String.Join("", worstMethodsAreInModules);
            template.Replace("{WorstMethodsAreInModules}", strWorstMethodsAreInModules);
            template.Replace("{TableBodyOfWorstMethods}", tableOfWorstMethods.ToString());
        }

        public static void FillWorstClasses(TransformedMetrics transformedMetrics, StringBuilder template)
        {
            var worstClasses = transformedMetrics.Classes.OrderBy(c => c.MaintainabilityIndex).Take(100).ToList();
            var tableOfWorstClasses = new StringBuilder(Templates.TableHeaderForClass);
            foreach (var cls in worstClasses)
            {
                var row = Templates.RowTemplateForClass.Inject(cls.FormatDecimalPoints());
                tableOfWorstClasses.AppendLine(row);
            }
            var worstClassesAreInModules = worstClasses.GroupBy(r => r.Module)
                .OrderByDescending(x => x.Count()).Take(5)
                .Select(x => $"<li>{x.Key}: {x.Count()} classes</li>");
            var strWorstClassesAreInModules = String.Join("", worstClassesAreInModules);
            template.Replace("{WorstClassesAreInModules}", strWorstClassesAreInModules);
            template.Replace("{TableBodyOfWorstClasses}", tableOfWorstClasses.ToString());
        }

        public static void FillModuleMetrics(TransformedMetrics transformedMetrics, StringBuilder template)
        {
            var moduleTableRows = new StringBuilder(Templates.TableHeaderForModule);
            foreach (var moduleMetric in transformedMetrics.Modules.OrderBy(r => r.MaintainabilityIndex))
            {
                var moduleMetricsRow = Templates.RowTemplateForModule.Inject(moduleMetric.FormatDecimalPoints());
                moduleTableRows.AppendLine(moduleMetricsRow);
            }
            template.Replace("{TableBodyOfModules}", moduleTableRows.ToString());
        }

        public static bool WriteToMetricsResult(string folderToSaveFile, StringBuilder template)
        {
            try
            {
                using (var output = new StreamWriter(folderToSaveFile))
                {
                    output.Write(template);
                }
                return true;
            }
            catch (Exception ex)
            {
                Console.Error.Write("Failed to save metrics data" + ex);
                try
                {
                    File.Delete(folderToSaveFile);
                }
                catch
                {
                    // ignored
                }
            }
            return false;
        }
    }
}
