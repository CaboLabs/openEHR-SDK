package test

import com.cabolabs.openehr.formats.OpenEhrJsonParser
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import groovy.util.GroovyTestCase
import groovy.json.JsonOutput

class OpenEhrJsonParserTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")
   
   void testJsonParserInstruction()
   {
      String path = "resources" + PS +"canonical_json"+ PS +"lab_order.json"
      File file = new File(path)
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)
      
      def out = JsonOutput.toJson(c)
      out = JsonOutput.prettyPrint(out)
      //println out
   }
   
   void testJsonParserObservation()
   {
      String path = "resources" + PS +"canonical_json"+ PS +"lab_results.json"
      File file = new File(path)
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)
      
      def out = JsonOutput.toJson(c)
      out = JsonOutput.prettyPrint(out)
      //println out
   }
   
   void testJsonParserReferralWithParticipations()
   {
      String path = "resources" + PS +"canonical_json"+ PS +"referral.json"
      File file = new File(path)
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)
      
      def out = JsonOutput.toJson(c)
      out = JsonOutput.prettyPrint(out)
      //println out
   }
   
   void testJsonParserAdminEntry()
   {
      String path = "resources" + PS +"canonical_json"+ PS +"admin.json"
      File file = new File(path)
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)
      
      def out = JsonOutput.toJson(c)
      out = JsonOutput.prettyPrint(out)
      println out
   }
   
}
