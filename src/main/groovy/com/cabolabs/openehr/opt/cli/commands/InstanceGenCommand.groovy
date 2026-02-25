package com.cabolabs.openehr.opt.cli.commands

import com.cabolabs.openehr.opt.cli.services.OptService
import com.cabolabs.openehr.opt.cli.services.InstanceGeneratorService
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable

@Command(name = "ingen", description = "Generate instances from an Operational Template")
class InstanceGenCommand implements Callable<Integer> {

   @Option(names = ["-s", "--source"], required = true, description = "Path to OPT file or folder")
   String source

   @Option(names = ["-d", "--dest"], required = true, description = "Destination folder")
   String dest

   @Option(names = ["-n", "--amount"], defaultValue = "1", description = "Number of instances to generate (default: 1)")
   int amount

   @Option(names = ["-f", "--format"], defaultValue = "json", description = "Output format: json or xml (default: json)")
   String format

   @Option(names = ["-t", "--type"], defaultValue = "locatable", description = "Type: version or locatable (default: locatable)")
   String type

   @Option(names = ["--with-participations"], description = "Add participations (only for COMPOSITION templates)")
   boolean withParticipations

   @Option(names = ["--flavor"], defaultValue = "rm", description = "Data structure flavor: rm or api (default: rm)")
   String flavor

   @Override
   Integer call() {
      try {
         // Validate inputs
         if (amount <= 0) {
            println "Amount must be greater than 0"
            return 1
         }

         if (!['json', 'xml'].contains(format)) {
            println "Format must be 'json' or 'xml', got: $format"
            return 1
         }

         if (!['version', 'locatable'].contains(type)) {
            println "Type must be 'version' or 'locatable', got: $type"
            return 1
         }

         if (!['rm', 'api'].contains(flavor)) {
            println "Flavor must be 'rm' or 'api', got: $flavor"
            return 1
         }

         def destFile = new File(dest)
         if (!destFile.exists() || !destFile.isDirectory()) {
            println "Destination path doesn't exist or is not a folder: $dest"
            return 1
         }

         // Load OPTs
         def opts = OptService.loadFromPath(source)

         // Generate instances
         InstanceGeneratorService.generateInstances(opts, dest, withParticipations, amount, format, type, flavor)

         return 0

      } catch (Exception e) {
         println "Error: ${e.message}"
         return 1
      }
   }
}
