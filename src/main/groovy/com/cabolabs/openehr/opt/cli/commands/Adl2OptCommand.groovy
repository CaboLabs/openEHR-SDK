package com.cabolabs.openehr.opt.cli.commands

import com.cabolabs.openehr.opt.cli.services.Adl2OptService
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable

@Command(name = "adl2opt", description = "Generate OPT from ADL archetype")
class Adl2OptCommand implements Callable<Integer> {

   @Option(names = ["-s", "--source"], required = true, description = "Path to ADL file")
   String source

   @Option(names = ["-d", "--dest"], required = true, description = "Destination folder")
   String dest

   @Override
   Integer call() {
      try {
         def sourceFile = new File(source)
         if (!sourceFile.exists() || !sourceFile.isFile()) {
            println "Source path doesn't exist or is not a file: $source"
            return 1
         }

         def destFile = new File(dest)
         if (!destFile.exists() || !destFile.isDirectory()) {
            println "Destination path doesn't exist or is not a folder: $dest"
            return 1
         }

         def outputFile = Adl2OptService.generateOptFromAdl(sourceFile, dest)

         println "OPT generated: ${outputFile.absolutePath}"
         return 0

      } catch (Exception e) {
         println "Error: ${e.message}"
         return 1
      }
   }
}
