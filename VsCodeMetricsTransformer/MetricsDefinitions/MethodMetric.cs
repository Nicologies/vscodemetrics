using System;
using System.Collections.Generic;

namespace MetricsDefinitions
{
    public class MethodMetric : ClassMetric
    {
        public class MethodMetricComparer : IEqualityComparer<MethodMetric>
        {
            public bool Equals(MethodMetric x, MethodMetric y)
            {
                return x.MethodName == y.MethodName
                       && x.Class == y.Class
                       && x.Module == y.Module;
            }

            public int GetHashCode(MethodMetric obj)
            {
                return (obj.MethodName + obj.Class + obj.Module).GetHashCode();
            }
        }
        public string MethodName { get; set; }
    }
}