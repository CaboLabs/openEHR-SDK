package com.cabolabs.openehr.opt.cli.services

import com.cabolabs.openehr.opt.instance_validation.XmlValidation
import com.cabolabs.openehr.opt.instance_validation.JsonInstanceValidation
import com.cabolabs.openehr.opt.manager.OptManager
import com.cabolabs.openehr.opt.manager.OptRepositoryFSImpl
import com.cabolabs.openehr.formats.OpenEhrXmlParser
import com.cabolabs.openehr.formats.OpenEhrJsonParser
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.validation.RmValidator2

class InstanceValidatorService {

   private static String PS = System.getProperty("file.separator")

   static boolean validateXml(validator, File file) {
      if (!validator.validate(file.text)) {
         println file.name + ' Schema NOT VALID'
         println '====================================='
         validator.errors.each { println it }
         println '====================================='
         println ""
         return false
      }
      println file.name + ' Schema VALID'
      println ""
      return true
   }

   static boolean validateJson(validator, File file) {
      def validationMessages = validator.validate(file.text)
      if (validationMessages.size() > 0) {
         println file.name + ' Schema NOT VALID'
         println '====================================='
         validationMessages.each { println it.message }
         println '====================================='
         println ""
         return false
      }
      println file.name + ' Schema VALID'
      println ""
      return true
   }

   static void validateWithOpt(File file, String ext) {
      def parser = (ext == 'json') ? new OpenEhrJsonParser() : new OpenEhrXmlParser()
      def instance = (ext == 'json') ? parser.parseJson(file.text) : parser.parseLocatable(file.text)
      validateLocatableWithOpt(instance)
   }

   static void validateLocatableWithOpt(Locatable locatable) {
      String optRepoPath = "src" + PS + "main" + PS + "resources" + PS + "opts"
      def repo = new OptRepositoryFSImpl(optRepoPath)
      def optManager = OptManager.getInstance()
      optManager.init(repo)

      def validator = new RmValidator2(optManager)
      def report = validator.dovalidate(locatable, OptManager.DEFAULT_NAMESPACE)

      if (report.hasErrors()) {
         println "Semantic NOT VALID:"
         report.errors.each { error -> println error }
         println ""
      } else {
         println "Semantic VALID"
      }
   }

   static String fileExtension(String path) {
      path.lastIndexOf('.').with { it != -1 ? path.substring(it + 1) : '' }
   }
}
