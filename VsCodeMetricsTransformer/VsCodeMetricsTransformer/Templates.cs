using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using VsCodeMetricsTransformer.Transformed;

namespace VsCodeMetricsTransformer
{
    class Templates
    {
        public static readonly string TableHeaderForClass = $@"
<thead>
    <tr>
        <th>{nameof(ModuleMetric.Module)}</th>
        <th>{nameof(ClassMetric.Class)}</th>
        <th>{nameof(ModuleMetric.MaintainabilityIndex)}</th>
        <th>{nameof(ModuleMetric.CyclomaticComplexity)}</th>
        <th>{nameof(ModuleMetric.ClassCoupling)}</th>
        <th>{nameof(ModuleMetric.DepthOfInheritance)}</th>
        <th>{nameof(ModuleMetric.LinesOfCode)}</th>
    </tr>
</thead>
";


        public static readonly string TableHeaderForModule = $@"
<thead>
    <tr>
        <th>{nameof(ModuleMetric.Module)}</th>
        <th>{nameof(ModuleMetric.MaintainabilityIndex)}</th>
        <th>{nameof(ModuleMetric.CyclomaticComplexity)}</th>
        <th>{nameof(ModuleMetric.ClassCoupling)}</th>
        <th>{nameof(ModuleMetric.DepthOfInheritance)}</th>
        <th>{nameof(ModuleMetric.LinesOfCode)}</th>
    </tr>
</thead>
";

        public static readonly string TableHeaderForMethod = $@"
<thead>
    <tr>
        <th>{nameof(ModuleMetric.Module)}</th>
        <th>{nameof(ClassMetric.Class)}</th>
        <th>{nameof(MethodMetric.MethodName)}</th>
        <th>{nameof(ModuleMetric.MaintainabilityIndex)}</th>
        <th>{nameof(ModuleMetric.CyclomaticComplexity)}</th>
        <th>{nameof(ModuleMetric.ClassCoupling)}</th>
        <th>{nameof(ModuleMetric.LinesOfCode)}</th>
    </tr>
</thead>
";

        public static readonly string RowTemplateForModule = $@"
<tr>
    <td>{{{nameof(ModuleMetric.Module)}}}</td>
    <td>{{{nameof(ModuleMetric.MaintainabilityIndex)}}}</td>
    <td>{{{nameof(ModuleMetric.CyclomaticComplexity)}}}</td>
    <td>{{{nameof(ModuleMetric.ClassCoupling)}}}</td>
    <td>{{{nameof(ModuleMetric.DepthOfInheritance)}}}</td>
    <td>{{{nameof(ModuleMetric.LinesOfCode)}}}</td>
</tr>
";
        public static readonly string RowTemplateForClass = $@"
<tr>
    <td>{{{nameof(ModuleMetric.Module)}}}</td>
    <td>{{{nameof(ClassMetric.Class)}}}</td>
    <td>{{{nameof(ModuleMetric.MaintainabilityIndex)}}}</td>
    <td>{{{nameof(ModuleMetric.CyclomaticComplexity)}}}</td>
    <td>{{{nameof(ModuleMetric.ClassCoupling)}}}</td>
    <td>{{{nameof(ModuleMetric.DepthOfInheritance)}}}</td>
    <td>{{{nameof(ModuleMetric.LinesOfCode)}}}</td>
</tr>
";
        public static readonly string RowTemplateForMethod = $@"
<tr>
    <td>{{{nameof(ModuleMetric.Module)}}}</td>
    <td>{{{nameof(ClassMetric.Class)}}}</td>
    <td>{{{nameof(MethodMetric.MethodName)}}}</td>
    <td>{{{nameof(ModuleMetric.MaintainabilityIndex)}}}</td>
    <td>{{{nameof(ModuleMetric.CyclomaticComplexity)}}}</td>
    <td>{{{nameof(ModuleMetric.ClassCoupling)}}}</td>
    <td>{{{nameof(ModuleMetric.LinesOfCode)}}}</td>
</tr>
";
        private readonly static string AssemblyLoc = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location);

        public static string GetMainHtmlTemplate()
        {
            return File.ReadAllText(Path.Combine(AssemblyLoc, "MainPage.html"));
        }
    }
}
