using System;
using System.Linq;
using System.Text;
using CommandLine;
using CommandLine.Text;
using MetricsDefinitions;
using MetricsStorage;

namespace MetricsProcessor
{
    class Options
    {
        [Option('i', "input", Required = true,
            HelpText = "Input zip file to be processed.")]
        public string MetricsZip { get; set; }

        [Option('o', "output", Required = true, HelpText = "output file name")]
        public string OutputFile { get; set; }

        [Option('p', "previous", Required = false, HelpText = "Previous Metrics to Compare. Zip file")]
        public string PreviousMetricsZip { get; set; }

        [HelpOption]
        public string GetUsage()
        {
            return HelpText.AutoBuild(this,
              current => HelpText.DefaultParsingErrorsHandler(this, current));
        }
    }

    class Program
    {
        static void Main(string[] args)
        {
            var options = new Options();
            if (!Parser.Default.ParseArguments(args, options))
            {
                Console.Error.Write("Invalid Parameters");
                Environment.Exit(1);
            }

            var transformedMetrics = MetricsStorageHelper.LoadMetrics(options.MetricsZip);
            if (transformedMetrics == null)
            {
                Environment.Exit(1);
            }
            var mainHtmlTemplate = new StringBuilder(Templates.GetMainHtmlTemplate());
            MetricsReporter.FillModuleMetrics(transformedMetrics, mainHtmlTemplate);
            MetricsReporter.FillWorstClasses(transformedMetrics, mainHtmlTemplate);
            MetricsReporter.FillWorstMethods(transformedMetrics, mainHtmlTemplate);
            var hasNewMethods = false;
            if (!string.IsNullOrWhiteSpace(options.PreviousMetricsZip))
            {
                var previousMetrics = MetricsStorageHelper.LoadMetrics(options.PreviousMetricsZip);
                if (previousMetrics != null)
                {
                    var newMethods = transformedMetrics.Methods.Except(previousMetrics.Methods,
                        new MethodMetric.MethodMetricComparer()).ToList();
                    if (newMethods.Any())
                    {
                        hasNewMethods = true;
                        MetricsReporter.FillWorstNewMethods(newMethods, mainHtmlTemplate);
                    }
                }
            }
            if (!hasNewMethods)
            {
                MetricsReporter.SetVisibilityOfWorstNewMethods(mainHtmlTemplate, visible: false);
            }

            if (!MetricsReporter.WriteToMetricsResult(options.OutputFile, mainHtmlTemplate))
            {
                Environment.Exit(1);
            }
        }
    }
}
