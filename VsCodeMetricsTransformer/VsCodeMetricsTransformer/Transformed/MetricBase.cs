using System;

namespace VsCodeMetricsTransformer.Transformed
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
}