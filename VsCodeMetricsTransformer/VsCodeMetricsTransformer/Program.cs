using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Xml.Serialization;
using RawMetric = VsCodeMetricsTransformer.MetricsMetric;
using Target = VsCodeMetricsTransformer.CodeMetricsReportTargetsTarget;
using Module = VsCodeMetricsTransformer.CodeMetricsReportTargetsTargetModulesModule;
using Namespace = VsCodeMetricsTransformer.CodeMetricsReportTargetsTargetModulesModuleNamespacesNamespace;
using Namespaces = VsCodeMetricsTransformer.CodeMetricsReportTargetsTargetModulesModuleNamespacesNamespace;
using ClassType = VsCodeMetricsTransformer.CodeMetricsReportTargetsTargetModulesModuleNamespacesNamespaceTypesType;
using Member = VsCodeMetricsTransformer.CodeMetricsReportTargetsTargetModulesModuleNamespacesNamespaceTypesTypeMembersMember;

namespace VsCodeMetricsTransformer
{
    class MetricBase
    {
        public string Module { get; set; }
        public string MaintainabilityIndex { get; set; }
        public string CyclomaticComplexity { get; set; }
        public string ClassCoupling { get; set; }
        public string LinesOfCode { get; set; }
    }

    class AssemblyMetric : MetricBase
    {
        public string DepthOfInheritance { get; set; }
    }

    class ClassMetric : AssemblyMetric
    {
        public string Class { get; set; }
    }

    class MethodMetric : MetricBase
    {
        public string Class { get; set; }
        public string MethodName { get; set; }
    }

    class Program
    {
        private static readonly string TableHeaderForClass = $@"
<thead>
    <tr>
        <th>{nameof(AssemblyMetric.Module)}</th>
        <th>{nameof(ClassMetric.Class)}</th>
        <th>{nameof(AssemblyMetric.MaintainabilityIndex)}</th>
        <th>{nameof(AssemblyMetric.CyclomaticComplexity)}</th>
        <th>{nameof(AssemblyMetric.ClassCoupling)}</th>
        <th>{nameof(AssemblyMetric.DepthOfInheritance)}</th>
        <th>{nameof(AssemblyMetric.LinesOfCode)}</th>
    </tr>
</thead>
";

        private static readonly string TableHeaderForModule = $@"
<thead>
    <tr>
        <th>{nameof(AssemblyMetric.Module)}</th>
        <th>{nameof(AssemblyMetric.MaintainabilityIndex)}</th>
        <th>{nameof(AssemblyMetric.CyclomaticComplexity)}</th>
        <th>{nameof(AssemblyMetric.ClassCoupling)}</th>
        <th>{nameof(AssemblyMetric.DepthOfInheritance)}</th>
        <th>{nameof(AssemblyMetric.LinesOfCode)}</th>
    </tr>
</thead>
";

        private static readonly string TableHeaderForMethod = $@"
<thead>
    <tr>
        <th>{nameof(AssemblyMetric.Module)}</th>
        <th>{nameof(ClassMetric.Class)}</th>
        <th>{nameof(MethodMetric.MethodName)}</th>
        <th>{nameof(AssemblyMetric.MaintainabilityIndex)}</th>
        <th>{nameof(AssemblyMetric.CyclomaticComplexity)}</th>
        <th>{nameof(AssemblyMetric.ClassCoupling)}</th>
        <th>{nameof(AssemblyMetric.LinesOfCode)}</th>
    </tr>
</thead>
";

        private static readonly string RowTemplateForModule = $@"
<tr>
    <td>{{{nameof(AssemblyMetric.Module)}}}</td>
    <td>{{{nameof(AssemblyMetric.MaintainabilityIndex)}}}</td>
    <td>{{{nameof(AssemblyMetric.CyclomaticComplexity)}}}</td>
    <td>{{{nameof(AssemblyMetric.ClassCoupling)}}}</td>
    <td>{{{nameof(AssemblyMetric.DepthOfInheritance)}}}</td>
    <td>{{{nameof(AssemblyMetric.LinesOfCode)}}}</td>
</tr>
";
        private static readonly string RowTemplateForClass = $@"
<tr>
    <td>{{{nameof(AssemblyMetric.Module)}}}</td>
    <td>{{{nameof(ClassMetric.Class)}}}</td>
    <td>{{{nameof(AssemblyMetric.MaintainabilityIndex)}}}</td>
    <td>{{{nameof(AssemblyMetric.CyclomaticComplexity)}}}</td>
    <td>{{{nameof(AssemblyMetric.ClassCoupling)}}}</td>
    <td>{{{nameof(AssemblyMetric.DepthOfInheritance)}}}</td>
    <td>{{{nameof(AssemblyMetric.LinesOfCode)}}}</td>
</tr>
";
        private static readonly string RowTemplateForMethod = $@"
<tr>
    <td>{{{nameof(AssemblyMetric.Module)}}}</td>
    <td>{{{nameof(ClassMetric.Class)}}}</td>
    <td>{{{nameof(MethodMetric.MethodName)}}}</td>
    <td>{{{nameof(AssemblyMetric.MaintainabilityIndex)}}}</td>
    <td>{{{nameof(AssemblyMetric.CyclomaticComplexity)}}}</td>
    <td>{{{nameof(AssemblyMetric.ClassCoupling)}}}</td>
    <td>{{{nameof(AssemblyMetric.LinesOfCode)}}}</td>
</tr>
";
        private static void Main(string[] args)
        {
            if (args.Length != 2)
            {
                Environment.Exit(-1);
            }
            try
            {
                var assemblyLoc = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location);
                var mainHtml = File.ReadAllText(Path.Combine(assemblyLoc, "MainPage.html"));
                var files = new DirectoryInfo(args[0]).EnumerateFiles("*VsCodeMetricsReport.xml");
                var modules = new List<Module>();
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
                            modules.Add(module);
                        }
                    }
                    catch (Exception ex)
                    {
                        Console.Error.Write($"Failed to parse: {file.Name}" + ex);
                    }
                }
                var moduleTableRows = new StringBuilder(TableHeaderForModule);
                var classes = new List<ClassMetric>();
                var methods = new List<MethodMetric>();
                foreach (var module in modules.OrderBy(m => int.Parse(m.Metrics[0].Value)))
                {
                    var moduleMetric = new AssemblyMetric
                    {
                        Module = module.Name,
                        MaintainabilityIndex = module.Metrics[0].Value,
                        CyclomaticComplexity = module.Metrics[1].Value,
                        ClassCoupling = module.Metrics[2].Value,
                        DepthOfInheritance = module.Metrics[3].Value,
                        LinesOfCode = module.Metrics[4].Value
                    };
                    var clsCount = 0;
                    if (module.Namespaces.Any())
                    {
                        foreach (var cls in module.Namespaces.SelectMany(n => n.Types))
                        {
                            ++clsCount;
                            classes.Add(new ClassMetric()
                            {
                                Module = module.Name,
                                Class = cls.Name,
                                MaintainabilityIndex = cls.Metrics[0].Value,
                                CyclomaticComplexity = cls.Metrics[1].Value,
                                ClassCoupling = cls.Metrics[2].Value,
                                DepthOfInheritance = cls.Metrics[3].Value,
                                LinesOfCode = cls.Metrics[4].Value,
                            });
                            foreach (var method in cls.Members)
                            {
                                methods.Add(new MethodMetric()
                                {
                                    Module=module.Name,
                                    Class = cls.Name,
                                    MethodName = method.Name,
                                    MaintainabilityIndex = method.Metrics[0].Value,
                                    CyclomaticComplexity = method.Metrics[1].Value,
                                    ClassCoupling = method.Metrics[2].Value,
                                    LinesOfCode = method.Metrics[3].Value,
                                });
                            }
                        };
                    }
                    if (clsCount != 0)
                    {
                        moduleMetric.ClassCoupling = (Convert.ToDouble(moduleMetric.ClassCoupling) / clsCount)
                            .ToString("0.00");

                        moduleMetric.CyclomaticComplexity =
                            (Convert.ToDouble(moduleMetric.CyclomaticComplexity) / clsCount).ToString("0.00");
                    }
                    var moduleMetricsRow = RowTemplateForModule.Inject(moduleMetric);
                    moduleTableRows.AppendLine(moduleMetricsRow);
                }
                var mainPage = new StringBuilder(mainHtml);
                mainPage.Replace("{TableBodyOfModules}", moduleTableRows.ToString());

                var worstClasses = classes.OrderBy(c => int.Parse(c.MaintainabilityIndex)).Take(50);
                var tableOfWorstClasses = new StringBuilder(TableHeaderForClass);
                foreach (var cls in worstClasses)
                {
                    var row = RowTemplateForClass.Inject(cls);
                    tableOfWorstClasses.AppendLine(row);
                }
                mainPage.Replace("{TableBodyOfWorstClasses}", tableOfWorstClasses.ToString());
                var worstMethods = methods.OrderBy(c => int.Parse(c.MaintainabilityIndex)).Take(100);
                var tableOfWorstMethods = new StringBuilder(TableHeaderForMethod);
                foreach (var method in worstMethods)
                {
                    var row = RowTemplateForMethod.Inject(method);
                    tableOfWorstMethods.AppendLine(row);
                }
                mainPage.Replace("{TableBodyOfWorstMethods}", tableOfWorstMethods.ToString());
                try
                {
                    using (var output = new StreamWriter(args[1]))
                    {
                        output.Write(mainPage);
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
    }
}
