using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Xml.Serialization;
using CsvHelper;
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
        public double MaintainabilityIndex { get; set; }
        public double CyclomaticComplexity { get; set; }
        public double ClassCoupling { get; set; }
        public int LinesOfCode { get; set; }

        public virtual MetricBase FormatDecimalPoints()
        {
            MaintainabilityIndex = Math.Round(MaintainabilityIndex, 2);
            CyclomaticComplexity = Math.Round(CyclomaticComplexity, 2);
            ClassCoupling = Math.Round(ClassCoupling, 2);
            return this;
        }
    }

    class AssemblyMetric : MetricBase
    {
        public double DepthOfInheritance { get; set; }

        public override MetricBase FormatDecimalPoints()
        {
            base.FormatDecimalPoints();
            DepthOfInheritance = Math.Round(DepthOfInheritance, 2);
            return this;
        }
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
                var modules = new List<AssemblyMetric>();
                var classes = new List<ClassMetric>();
                var methods = new List<MethodMetric>();

                foreach (var module in rawModules)
                {
                    if (module.Metrics.Last().Value == "0")
                    {// line of code is 0
                        continue;
                    }
                    var moduleMetric = new AssemblyMetric
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
                                    MethodName = method.Name,
                                    MaintainabilityIndex = int.Parse(method.Metrics[0].Value),
                                    CyclomaticComplexity = int.Parse(method.Metrics[1].Value),
                                    ClassCoupling = int.Parse(method.Metrics[2].Value),
                                    LinesOfCode = int.Parse(method.Metrics[3].Value),
                                };
                                methods.Add(methodMetric);
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
                            classes.Add(clsMetric);

                            moduleMetric.MaintainabilityIndex += clsMetric.MaintainabilityIndex;
                            moduleMetric.ClassCoupling += clsMetric.ClassCoupling;
                            moduleMetric.CyclomaticComplexity += clsMetric.CyclomaticComplexity;
                            moduleMetric.DepthOfInheritance += clsMetric.DepthOfInheritance;
                            moduleMetric.LinesOfCode += clsMetric.LinesOfCode;
                        };
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
                    modules.Add(moduleMetric);
                }

                var moduleTableRows = new StringBuilder(TableHeaderForModule);
                foreach (var moduleMetric in modules.OrderBy(r => r.MaintainabilityIndex))
                {
                    var moduleMetricsRow = RowTemplateForModule.Inject(moduleMetric.FormatDecimalPoints());
                    moduleTableRows.AppendLine(moduleMetricsRow);
                }
                var mainPage = new StringBuilder(mainHtml);
                mainPage.Replace("{TableBodyOfModules}", moduleTableRows.ToString());

                var worstClasses = classes.OrderBy(c => c.MaintainabilityIndex).Take(100).ToList();
                var tableOfWorstClasses = new StringBuilder(TableHeaderForClass);
                foreach (var cls in worstClasses)
                {
                    var row = RowTemplateForClass.Inject(cls.FormatDecimalPoints());
                    tableOfWorstClasses.AppendLine(row);
                }
                var worstClassesAreInModules = worstClasses.GroupBy(r => r.Module)
                    .OrderByDescending(x => x.Count()).Take(5)
                    .Select(x => $"<li>{x.Key}: {x.Count()} classes</li>");
                var strWorstClassesAreInModules = string.Join("", worstClassesAreInModules);
                mainPage.Replace("{WorstClassesAreInModules}", strWorstClassesAreInModules);
                mainPage.Replace("{TableBodyOfWorstClasses}", tableOfWorstClasses.ToString());
                var worstMethods = methods.OrderBy(c => c.MaintainabilityIndex).Take(100).ToList();
                var tableOfWorstMethods = new StringBuilder(TableHeaderForMethod);
                foreach (var method in worstMethods)
                {
                    var row = RowTemplateForMethod.Inject(method.FormatDecimalPoints());
                    tableOfWorstMethods.AppendLine(row);
                }

                var worstMethodsAreInModules = worstMethods.GroupBy(r => r.Module)
                    .OrderByDescending(x => x.Count()).Take(5)
                    .Select(x => $"<li>{x.Key}: {x.Count()} methods</li>");
                var strWorstMethodsAreInModules = string.Join("", worstMethodsAreInModules);
                mainPage.Replace("{WorstMethodsAreInModules}", strWorstMethodsAreInModules);
                mainPage.Replace("{TableBodyOfWorstMethods}", tableOfWorstMethods.ToString());
                try
                {
                    using (var output = new StreamWriter(args[1]))
                    {
                        output.Write(mainPage);
                    }
                    var dir = Path.GetDirectoryName(args[1]);
                    var csv = Path.Combine(dir, "FullList.csv");
                    using (var stream = new StreamWriter(csv))
                    {
                        var writer = new CsvWriter(stream);
                        writer.WriteHeader<MethodMetric>();
                        foreach (var method in methods)
                        {
                            writer.WriteRecord(method);
                        }
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
