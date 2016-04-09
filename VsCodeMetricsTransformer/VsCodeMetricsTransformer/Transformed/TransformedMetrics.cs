using System.Collections.Generic;

namespace VsCodeMetricsTransformer.Transformed
{
    class TransformedMetrics
    {
        public readonly List<ModuleMetric> Modules = new List<ModuleMetric>();
        public readonly List<ClassMetric> Classes = new List<ClassMetric>();
        public readonly List<MethodMetric> Methods = new List<MethodMetric>();
    }
}