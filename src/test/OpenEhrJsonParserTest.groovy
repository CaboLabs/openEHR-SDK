package test

import com.cabolabs.openehr.formats.OpenEhrJsonParser
import groovy.util.GroovyTestCase

class OpenEhrJsonParserTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")
   
   void testJsonParser()
   {
      String path = "resources" + PS +"canonical_json"+ PS +"lab_test.json"
      File file = new File(path)
      String json = file.text
      def parser = new OpenEhrJsonParser()
      parser.parseJson(json)
   }
   
   
}
