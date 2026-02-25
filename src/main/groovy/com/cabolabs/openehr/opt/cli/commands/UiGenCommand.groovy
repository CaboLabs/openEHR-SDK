package com.cabolabs.openehr.opt.cli.commands

import com.cabolabs.openehr.opt.cli.services.OptService
import com.cabolabs.openehr.opt.cli.services.UiGeneratorService
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable

@Command(name = "uigen", description = "Generate UI from an Operational Template")
class UiGenCommand implements Callable<Integer> {

   @Option(names = ["-s", "--source"], required = true, description = "Path to OPT file")
   String source

   @Option(names = ["-d", "--dest"], required = true, description = "Destination folder")
   String dest

   @Option(names = ["--bootstrap"], defaultValue = "bs5", description = "Bootstrap version: bs4 or bs5 (default: bs5)")
   String bootstrap

   @Option(names = ["--type"], defaultValue = "full", description = "Generation type: full or form (default: full)")
   String type

   @Override
   Integer call() {
      try {
         // Validate inputs
         if (!['bs4', 'bs5'].contains(bootstrap)) {
            println "Bootstrap version must be 'bs4' or 'bs5', got: $bootstrap"
            return 1
         }

         if (!['full', 'form'].contains(type)) {
            println "Type must be 'full' or 'form', got: $type"
            return 1
         }

         def destFile = new File(dest)
         if (!destFile.exists() || !destFile.isDirectory()) {
            println "Destination path doesn't exist or is not a folder: $dest"
            return 1
         }

         // Load and parse OPT
         def opt = OptService.loadAndParse(source)

         // Generate UI
         int bootstrapVersion = (bootstrap == 'bs4') ? 4 : 5
         boolean fullPage = (type == 'full')
         def outputFile = UiGeneratorService.generateUi(opt, dest, bootstrapVersion, fullPage)

         println "Generated: ${outputFile.absolutePath}"
         return 0

      } catch (Exception e) {
         println "Error: ${e.message}"
         return 1
      }
   }
}
