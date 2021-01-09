package test

import com.cabolabs.openehr.formats.OpenEhrJsonParser
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import groovy.util.GroovyTestCase
import groovy.json.JsonOutput

class OpenEhrJsonParserTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")
   
   void testJsonParser()
   {
      String path = "resources" + PS +"canonical_json"+ PS +"lab_test.json"
      File file = new File(path)
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)
      
      def out = JsonOutput.toJson(c)
      println out
   }
   
   
}
