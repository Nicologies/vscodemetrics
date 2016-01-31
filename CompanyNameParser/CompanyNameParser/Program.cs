using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace CompanyNameParser
{
    class Program
    {
        static void Main(string[] args)
        {
            if (args.Length != 1)
            {
                Environment.Exit(-1);
            }
            try
            {
                var verInfo = FileVersionInfo.GetVersionInfo(args[0]);
                Console.Write(verInfo.CompanyName);
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine("Error: " + ex);
            }
        }
    }
}
