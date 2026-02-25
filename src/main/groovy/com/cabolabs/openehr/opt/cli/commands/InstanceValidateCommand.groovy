package com.cabolabs.openehr.opt.cli.commands

import com.cabolabs.openehr.opt.cli.services.InstanceValidatorService
import com.cabolabs.openehr.opt.instance_validation.XmlValidation
import com.cabolabs.openehr.opt.instance_validation.JsonInstanceValidation
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable

@Command(name = "inval", description = "Validate XML/JSON instances against schemas")
class InstanceValidateCommand implements Callable<Integer> {

   @Option(names = ["-s", "--source"], required = true, description = "Path to instance file or folder")
   String source

   @Option(names = ["--flavor"], defaultValue = "rm", description = "Data structure flavor: rm or api (default: rm)")
   String flavor

   @Option(names = ["--semantic"], description = "Perform semantic validation against OPT")
   boolean semantic

   @Override
   Integer call() {
      try {
         if (!['rm', 'api'].contains(flavor)) {
            println "Flavor must be 'rm' or 'api', got: $flavor"
            return 1
         }

         def file = new File(source)
         if (!file.exists()) {
            println "Path doesn't exist: $source"
            return 1
         }

         def xmlInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream('xsd/Version.xsd')
         def xmlValidator = new XmlValidation(xmlInputStream)
         def jsonValidator = new JsonInstanceValidation(flavor)

         boolean allValid = true

         if (file.isDirectory()) {
            file.eachFileMatch(~/.*.xml/) { xml ->
               if (InstanceValidatorService.validateXml(xmlValidator, xml) && semantic) {
                  InstanceValidatorService.validateWithOpt(xml, 'xml')
               } else if (!InstanceValidatorService.validateXml(xmlValidator, xml)) {
                  allValid = false
               }
            }

            file.eachFileMatch(~/.*.json/) { json ->
               if (InstanceValidatorService.validateJson(jsonValidator, json) && semantic) {
                  InstanceValidatorService.validateWithOpt(json, 'json')
               } else if (!InstanceValidatorService.validateJson(jsonValidator, json)) {
                  allValid = false
               }
            }
         } else {
            String ext = InstanceValidatorService.fileExtension(source)
            if (ext == 'json') {
               allValid = InstanceValidatorService.validateJson(jsonValidator, file)
               if (allValid && semantic) {
                  InstanceValidatorService.validateWithOpt(file, 'json')
               }
            } else if (ext == 'xml') {
               allValid = InstanceValidatorService.validateXml(xmlValidator, file)
               if (allValid && semantic) {
                  InstanceValidatorService.validateWithOpt(file, 'xml')
               }
            } else {
               println "File extension $ext is not supported, only json and xml are supported"
               return 1
            }
         }

         return allValid ? 0 : 1

      } catch (Exception e) {
         println "Error: ${e.message}"
         return 1
      }
   }
}
