package  com.cabolabs.openehr.opt

import groovy.util.GroovyTestCase
import com.cabolabs.openehr.formats.OpenEhrJsonParserQuick
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
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)
      //opt_manager.loadAll()

      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(c, OptManager.DEFAULT_NAMESPACE)

      report.errors.each { error ->
         println "1: "+ error
      }
   }

   void testValidationFromXmlComposition2()
   {
      String path = PS +"canonical_xml"+ PS +"test_all_datatypes.composition.en.xml"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OpenEhrXmlParser() // TODO: create a quick parser that doesn't calculate paths
      Composition c = (Composition)parser.parseXml(xml)

      // TODO: add support for S3 repo
      String opt_repo_path = PS + "opts"
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(opt_repo_path).toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)
      //opt_manager.loadAll()

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(c, OptManager.DEFAULT_NAMESPACE)

      report.errors.each { error ->
         println "2: "+ error
      }
   }


   void testValidationFromJsonComposition()
   {
      String path = PS +"canonical_json"+ PS +"minimal_action.json"
	   File file = new File(getClass().getResource(path).toURI())
	   String json = file.text
	   def parser = new OpenEhrJsonParserQuick()
	   Composition c = (Composition)parser.parseJson(json)

      // TODO: add support for S3 repo
      String opt_repo_path = PS + "opts"
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(opt_repo_path).toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)
      //opt_manager.loadAll()

      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(c, OptManager.DEFAULT_NAMESPACE)

      report.errors.each { error ->
         println "3: "+ error
      }
   }

    void testValidationFromJsonComposition2()
   {
      String path = PS +"canonical_json"+ PS +"minimal_action.json"
	   File file = new File(getClass().getResource(path).toURI())
	   String json = file.text
	   def parser = new OpenEhrJsonParserQuick()
	   Composition c = (Composition)parser.parseJson(json)

      // TODO: add support for S3 repo
      String opt_repo_path = PS + "opts"
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(opt_repo_path).toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)
      //opt_manager.loadAll()

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(c, OptManager.DEFAULT_NAMESPACE)

      report.errors.each { error ->
         println "4: "+ error
      }
   }

   void testValidationFromJsonCompositionAllDatatypes()
   {
      String path = PS +"canonical_json"+ PS +"test_all_datatypes_en.json"
	   File file = new File(getClass().getResource(path).toURI())
	   String json = file.text
	   def parser = new OpenEhrJsonParserQuick()
	   Composition c = (Composition)parser.parseJson(json)

      // TODO: add support for S3 repo
      String opt_repo_path = PS + "opts"
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(opt_repo_path).toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)
      //opt_manager.loadAll()

      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(c, OptManager.DEFAULT_NAMESPACE)

      report.errors.each { error ->
         println '5: '+error
      }
   }

   void testValidationFromJsonCompositionAllDatatypes2()
   {
      String path = PS +"canonical_json"+ PS +"test_all_datatypes_en.json"
	   File file = new File(getClass().getResource(path).toURI())
	   String json = file.text
	   def parser = new OpenEhrJsonParserQuick()
	   Composition c = (Composition)parser.parseJson(json)

      // TODO: add support for S3 repo
      String opt_repo_path = PS + "opts"
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(opt_repo_path).toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)
      //opt_manager.loadAll()

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(c, OptManager.DEFAULT_NAMESPACE)

      report.errors.each { error ->
         println '6: '+ error
      }
   }

   void testValidationFromJsonCompositionInvalidCardinalitiesA()
   {
      String path = PS +"rm_validation"+ PS +"0_alternative_types.en.v1_000052_1.json"
	   File file = new File(getClass().getResource(path).toURI())
	   String json = file.text
	   def parser = new OpenEhrJsonParserQuick()
	   Composition c = (Composition)parser.parseJson(json)

      // TODO: add support for S3 repo
      String opt_repo_path = PS + "rm_validation"
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(opt_repo_path).toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)
      //opt_manager.loadAll()

      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(c, "")

      report.errors.each { error ->
         println '7: '+ error
      }
   }

   void testValidationFromJsonCompositionInvalidCardinalitiesA2()
   {
      String path = PS +"rm_validation"+ PS +"0_alternative_types.en.v1_000052_1.json"
	   File file = new File(getClass().getResource(path).toURI())
	   String json = file.text
	   def parser = new OpenEhrJsonParserQuick()
	   Composition c = (Composition)parser.parseJson(json)

      // TODO: add support for S3 repo
      String opt_repo_path = PS + "rm_validation"
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(opt_repo_path).toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)
      //opt_manager.loadAll()

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(c, "")

      report.errors.each { error ->
         println '8: '+ error
      }
   }

   void testValidationFromJsonCompositionInvalidCardinalitiesB()
   {
      String path = PS +"rm_validation"+ PS +"10_alternative_types.en.v1_000010_1.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick()
      Composition c = (Composition)parser.parseJson(json)

      // TODO: add support for S3 repo
      String opt_repo_path = PS + "rm_validation"
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(opt_repo_path).toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)
      //opt_manager.loadAll()

      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(c, "")

      report.errors.each { error ->
         println '9: '+ error
      }
   }


   void testValidationFromJsonCompositionInvalidCardinalitiesB2()
   {
      String path = PS +"rm_validation"+ PS +"10_alternative_types.en.v1_000010_1.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick()
      Composition c = (Composition)parser.parseJson(json)

      // TODO: add support for S3 repo
      String opt_repo_path = PS + "rm_validation"
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(opt_repo_path).toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)
      //opt_manager.loadAll()

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(c, "")

      report.errors.each { error ->
         println '10: '+ error
      }
   }
}