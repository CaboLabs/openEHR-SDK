package  com.cabolabs.openehr.opt

import groovy.util.GroovyTestCase
import com.cabolabs.openehr.formats.OpenEhrJsonParser
import com.cabolabs.openehr.formats.OpenEhrXmlParser
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.validation.*
import com.cabolabs.openehr.opt.manager.*

class RmValidationTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")

   void testValidationFromXmlComposition()
   {
      String path = PS +"canonical_xml"+ PS +"test_all_datatypes.composition.en.xml"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OpenEhrXmlParser()
      Composition c = (Composition)parser.parseXml(xml)

      // TODO: add support for S3 repo
      String opt_repo_path = PS + "opts"
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(opt_repo_path).toURI())
      OptManager opt_manager = OptManager.getInstance(repo)
      //opt_manager.loadAll()

      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(c)

      println report.errors
   }

   void testValidationFromJsonComposition()
   {
      String path = PS +"canonical_json"+ PS +"minimal_action.json"
	   File file = new File(getClass().getResource(path).toURI())
	   String json = file.text
	   def parser = new OpenEhrJsonParser()
	   Composition c = (Composition)parser.parseJson(json)

      // TODO: add support for S3 repo
      String opt_repo_path = PS + "opts"
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(opt_repo_path).toURI())
      OptManager opt_manager = OptManager.getInstance(repo)
      //opt_manager.loadAll()

      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(c)

      println report.errors
   }
}