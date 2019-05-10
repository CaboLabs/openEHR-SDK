package com.cabolabs.openehr.opt.instance_generator

import de.odysseus.staxon.json.JsonXMLConfig
import de.odysseus.staxon.json.JsonXMLConfigBuilder
import de.odysseus.staxon.json.JsonXMLOutputFactory

import com.cabolabs.openehr.opt.model.*

import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLEventWriter
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamException

class JsonInstanceGenerator {

   def JsonInstanceGenerator()
   {
   }

   String generateJSONVersionStringFromOPT(OperationalTemplate opt, boolean pretty_print = false, boolean addParticipations = false)
   {
      // Uses the XML generator then transforms the XML to JSON
      def xmlGen = new XmlInstanceGenerator()
      def xml = xmlGen.generateXMLVersionStringFromOPT(opt, addParticipations)
      return xmlToJson(xml, pretty_print)
   }

   String generateJSONCompositionStringFromOPT(OperationalTemplate opt, boolean pretty_print = false, boolean addParticipations = false)
   {
      // Uses the XML generator then transforms the XML to JSON
      def xmlGen = new XmlInstanceGenerator()
      def xml = xmlGen.generateXMLCompositionStringFromOPT(opt, addParticipations)
      return xmlToJson(xml, pretty_print)
   }

   static String xmlToJson(String xml_text, boolean pretty_print = false)
   {
      InputStream input = new ByteArrayInputStream(xml_text.getBytes())
      ByteArrayOutputStream output = new ByteArrayOutputStream()
      JsonXMLConfig config = new JsonXMLConfigBuilder()
         .autoArray(true)
         .autoPrimitive(true)
         .prettyPrint(pretty_print)
         .build()

      try
      {
         /*
          * Create reader (XML).
          */
         XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(input)
         /*
          * Create writer (JSON).
          */
         XMLEventWriter writer = new JsonXMLOutputFactory(config).createXMLEventWriter(output)
         //writer.toString();
         /*
          * Copy events from reader to writer.
          */
         writer.add(reader)

         /*
          * Close reader/writer.
          */
         reader.close()
         writer.close()

         return output.toString()
      }
      finally
      {
         /*
          * As per StAX specification, XMLEventReader/Writer.close() doesn't close
          * the underlying stream.
          */
         // This executes even the return of the try is executed.
         output.close()
         input.close()
      }
   }
}
