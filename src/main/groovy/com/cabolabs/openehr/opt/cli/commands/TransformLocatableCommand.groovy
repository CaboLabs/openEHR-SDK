package com.cabolabs.openehr.opt.cli.commands

import com.cabolabs.openehr.opt.cli.services.TransformService
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable

@Command(name = "locatable", description = "Transform Locatable between XML and JSON")
class TransformLocatableCommand implements Callable<Integer> {

   @Option(names = ["-s", "--source"], required = true, description = "Path to Locatable file (.xml or .json)")
   String source

   @Option(names = ["-d", "--dest"], required = true, description = "Destination folder")
   String dest

   @Override
   Integer call() {
      try {
         def sourceFile = new File(source)
         if (!sourceFile.exists() || sourceFile.isDirectory()) {
            println "Source path doesn't exist or is not a file: $source"
            return 1
         }

         def destFile = new File(dest)
         if (!destFile.exists() || !destFile.isDirectory()) {
            println "Destination path doesn't exist or is not a folder: $dest"
            return 1
         }

         def outputFile = TransformService.transformLocatable(sourceFile, dest)

         println "Created: ${outputFile.name}"
         return 0

      } catch (Exception e) {
         println "Error: ${e.message}"
         return 1
      }
   }
}
