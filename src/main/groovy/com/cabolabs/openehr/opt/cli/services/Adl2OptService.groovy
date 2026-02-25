package com.cabolabs.openehr.opt.cli.services

import com.cabolabs.openehr.opt.opt_generator.AdlToOpt
import com.cabolabs.openehr.opt.serializer.OptXmlSerializer
import org.openehr.am.archetype.Archetype
import se.acode.openehr.parser.ADLParser

class Adl2OptService {

   private static String PS = System.getProperty("file.separator")

   static File generateOptFromAdl(File adlFile, String destPath) {
      def archetype = loadArchetype(adlFile)
      def adlToOpt = new AdlToOpt()
      def opt = adlToOpt.generateOpt(archetype)

      def toXml = new OptXmlSerializer(true)
      String optString = toXml.serialize(opt)

      def dt = new java.text.SimpleDateFormat("yyyyMMddhhmmss").format(new Date())
      def outPath = destPath + PS + (opt.templateId.replaceAll(' ', '_').toLowerCase() + "_" + dt + '.opt')
      def outFile = new File(outPath)
      outFile << optString

      return outFile
   }

   private static Archetype loadArchetype(File adl) {
      ADLParser parser
      try {
         parser = new ADLParser(adl)
      } catch (IOException e) {
         throw new RuntimeException("Error creating ADL parser: " + e.message, e)
      }

      Archetype archetype
      try {
         archetype = parser.archetype()
      } catch (Exception e) {
         throw new RuntimeException("Error parsing archetype: " + e.message, e)
      }

      return archetype
   }
}
