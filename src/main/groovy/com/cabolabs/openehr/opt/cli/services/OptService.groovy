package com.cabolabs.openehr.opt.cli.services

import com.cabolabs.openehr.opt.instance_validation.XmlValidation
import com.cabolabs.openehr.opt.parser.OperationalTemplateParser
import com.cabolabs.openehr.opt.model.OperationalTemplate

class OptService {

   static OperationalTemplate loadAndParse(String path) {
      def optFile = new File(path)
      if (!optFile.exists()) throw new FileNotFoundException(path)

      def inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream('xsd/OperationalTemplateExtra.xsd')
      def validator = new XmlValidation(inputStream)

      if (!validate(validator, optFile)) {
         throw new IllegalArgumentException("OPT validation failed for: $path")
      }

      def text = optFile.getText()
      def parser = new OperationalTemplateParser()
      return parser.parse(text)
   }

   static List<OperationalTemplate> loadFromPath(String path) {
      def opts = []
      def source = new File(path)
      
      if (source.isDirectory()) {
         source.eachFileMatch(~/.*.opt/) { opt_file ->
            opts << loadAndParse(opt_file.path)
         }
      } else {
         opts << loadAndParse(path)
      }
      
      return opts
   }

   static boolean validate(validator, File file) {
      if (!validator.validate(file.text)) {
         println file.name + ' Schema NOT VALID'
         println '====================================='
         validator.errors.each { println it }
         println '====================================='
         return false
      }
      println file.name + ' Schema VALID'
      return true
   }
}
