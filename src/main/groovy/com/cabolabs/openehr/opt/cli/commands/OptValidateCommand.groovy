package com.cabolabs.openehr.opt.cli.commands

import com.cabolabs.openehr.opt.cli.services.OptService
import com.cabolabs.openehr.opt.instance_validation.XmlValidation
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable

@Command(name = "optval", description = "Validate an Operational Template against XSD")
class OptValidateCommand implements Callable<Integer> {

   @Option(names = ["-s", "--source"], required = true, description = "Path to OPT file")
   String source

   @Override
   Integer call() {
      try {
         def file = new File(source)
         if (!file.exists()) {
            println "File doesn't exist: $source"
            return 1
         }

         def inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream('xsd/OperationalTemplateExtra.xsd')
         def validator = new XmlValidation(inputStream)

         boolean isValid = OptService.validate(validator, file)
         return isValid ? 0 : 1

      } catch (Exception e) {
         println "Error: ${e.message}"
         return 1
      }
   }
}
