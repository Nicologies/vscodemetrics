using System;

namespace MetricsDefinitions
{
    public class ModuleMetric : MetricBase
    {
        public double DepthOfInheritance { get; set; }

        public override MetricBase FormatDecimalPoints()
        {
            base.FormatDecimalPoints();
            DepthOfInheritance = Math.Round(DepthOfInheritance, 2);
            return this;
        }
    }
}