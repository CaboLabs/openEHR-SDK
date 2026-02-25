package com.cabolabs.openehr.opt.cli.services

import com.cabolabs.openehr.opt.model.OperationalTemplate
import com.cabolabs.openehr.opt.serializer.JsonSerializer
import com.cabolabs.openehr.opt.serializer.OptXmlSerializer
import com.cabolabs.openehr.formats.OpenEhrXmlParser
import com.cabolabs.openehr.formats.OpenEhrJsonParser
import com.cabolabs.openehr.formats.OpenEhrXmlSerializer
import com.cabolabs.openehr.formats.OpenEhrJsonSerializer
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable

class TransformService {

   private static String PS = System.getProperty("file.separator")

   static File transformOptToJson(OperationalTemplate opt, String destPath) {
      def toJson = new JsonSerializer()
      toJson.serialize(opt)

      def dt = new java.text.SimpleDateFormat("yyyyMMddhhmmss").format(new Date())
      def out = new File(destPath + PS + opt.concept.replaceAll(' ', '_') + "_" + dt + "_opt.json")

      def printer = new java.io.PrintWriter(out, 'UTF-8')
      printer.write(toJson.get(true))
      printer.flush()
      printer.close()

      return out
   }

   static File transformLocatable(File source, String destPath) {
      def ext = InstanceValidatorService.fileExtension(source.path)
      
      if (ext == 'xml') {
         return transformXmlToJson(source, destPath)
      } else if (ext == 'json') {
         return transformJsonToXml(source, destPath)
      } else {
         throw new IllegalArgumentException("Extension $ext not supported, the LOCATABLE file should be .json or .xml")
      }
   }

   private static File transformXmlToJson(File source, String destPath) {
      def parser = new OpenEhrXmlParser()
      Locatable locatable = parser.parseLocatable(source.text)

      def serializer = new OpenEhrJsonSerializer()
      String json = serializer.serialize(locatable)

      def dt = new java.text.SimpleDateFormat("yyyyMMddhhmmss").format(new Date())
      def out = new File(destPath + PS + (source.name.replaceAll(' ', '_') - '.xml') + "_" + dt + ".json")

      def printer = new java.io.PrintWriter(out, 'UTF-8')
      printer.write(json)
      printer.flush()
      printer.close()

      return out
   }

   private static File transformJsonToXml(File source, String destPath) {
      def parser = new OpenEhrJsonParser()
      Locatable locatable = parser.parseJson(source.text)

      def serializer = new OpenEhrXmlSerializer()
      String xml = serializer.serialize(locatable)

      def dt = new java.text.SimpleDateFormat("yyyyMMddhhmmss").format(new Date())
      def out = new File(destPath + PS + (source.name.replaceAll(' ', '_') - '.json') + "_" + dt + ".xml")

      def printer = new java.io.PrintWriter(out, 'UTF-8')
      printer.write(xml)
      printer.flush()
      printer.close()

      return out
   }
}
