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
      String path = "/canonical_xml/test_all_datatypes.composition.en.xml"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OpenEhrXmlParser()
      Composition c = (Composition)parser.parseLocatable(xml)

      // TODO: add support for S3 repo
      String opt_repo_path = "/opts"
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

   /*
   void testValidationFromXmlComposition2()
   {
      String path = "/canonical_xml/test_all_datatypes.composition.en.xml"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OpenEhrXmlParser() // TODO: create a quick parser that doesn't calculate paths
      Composition c = (Composition)parser.parseLocatable(xml)

      // TODO: add support for S3 repo
      String opt_repo_path = "/opts"
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
      String path = "/canonical_json/minimal_action.json"
	   File file = new File(getClass().getResource(path).toURI())
	   String json = file.text
	   def parser = new OpenEhrJsonParserQuick()
	   Composition c = (Composition)parser.parseJson(json)

      // TODO: add support for S3 repo
      String opt_repo_path = "/opts"
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
      String path = "/canonical_json/minimal_action.json"
	   File file = new File(getClass().getResource(path).toURI())
	   String json = file.text
	   def parser = new OpenEhrJsonParserQuick()
	   Composition c = (Composition)parser.parseJson(json)

      // TODO: add support for S3 repo
      String opt_repo_path = "/opts"
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
      String path = "/canonical_json/test_all_datatypes_en.json"
	   File file = new File(getClass().getResource(path).toURI())
	   String json = file.text
	   def parser = new OpenEhrJsonParserQuick()
	   Composition c = (Composition)parser.parseJson(json)

      // TODO: add support for S3 repo
      String opt_repo_path = "/opts"
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
      String path = "/canonical_json/test_all_datatypes_en.json"
	   File file = new File(getClass().getResource(path).toURI())
	   String json = file.text
	   def parser = new OpenEhrJsonParserQuick()
	   Composition c = (Composition)parser.parseJson(json)

      // TODO: add support for S3 repo
      String opt_repo_path = "/opts"
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
      String path = "/rm_validation/0_alternative_types.en.v1_000052_1.json"
	   File file = new File(getClass().getResource(path).toURI())
	   String json = file.text
	   def parser = new OpenEhrJsonParserQuick()
	   Composition c = (Composition)parser.parseJson(json)

      // TODO: add support for S3 repo
      String opt_repo_path = "/rm_validation"
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
      String path = "/rm_validation/0_alternative_types.en.v1_000052_1.json"
	   File file = new File(getClass().getResource(path).toURI())
	   String json = file.text
	   def parser = new OpenEhrJsonParserQuick()
	   Composition c = (Composition)parser.parseJson(json)

      // TODO: add support for S3 repo
      String opt_repo_path = "/rm_validation"
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
      String path = "/rm_validation/10_alternative_types.en.v1_000010_1.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick()
      Composition c = (Composition)parser.parseJson(json)

      // TODO: add support for S3 repo
      String opt_repo_path = "/rm_validation"
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
      String path = "/rm_validation/10_alternative_types.en.v1_000010_1.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick()
      Composition c = (Composition)parser.parseJson(json)

      // TODO: add support for S3 repo
      String opt_repo_path = "/rm_validation"
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
   */

   Composition load_compo(String path)
   {
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick()
      return (Composition)parser.parseJson(json)
   }

   OptManager init_manager(String path)
   {
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(path).toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)
      return opt_manager
   }

   void testDataValidationAdmin1()
   {
      Composition c = load_compo("/rm_validation/data_validation_admin_1.json")
      OptManager opt_manager = init_manager("/rm_validation")

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(c, "")

      /*
      String template_id = c.archetype_details.template_id.value
      def opt = opt_manager.getOpt(template_id, "")

      println template_id

      println opt.nodes.each { path, node ->
         println path +' '+ node.occurrences
      }
      */

      report.errors.each { error ->
         println '11: '+ error
      }

      //println report.errors

      assert report.errors
   }

   void testDataValidationAdmin2()
   {
      Composition c = load_compo("/rm_validation/data_validation_admin_2.json")
      OptManager opt_manager = init_manager("/rm_validation")

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(c, "")

      report.errors.each { error ->
         println '12: '+ error
      }

      assert report.errors
   }

   void testDataValidationAdmin3()
   {
      Composition c = load_compo("/rm_validation/data_validation_admin_3.json")
      OptManager opt_manager = init_manager("/rm_validation")

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(c, "")

      report.errors.each { error ->
         println '13: '+ error
      }

      assert report.errors
   }

   void testDataValidationAdmin4()
   {
      Composition c = load_compo("/rm_validation/data_validation_admin_4.json")
      OptManager opt_manager = init_manager("/rm_validation")

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(c, "")

      report.errors.each { error ->
         println '14: '+ error
      }

      assert report.errors
   }


   void testDataValidationAdmin5()
   {
      Composition c = load_compo("/rm_validation/data_validation_admin_5.json")
      OptManager opt_manager = init_manager("/rm_validation")

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(c, "")

      report.errors.each { error ->
         println '15: '+ error
      }

      assert report.errors
   }

   void testDataValidationEval1()
   {
      Composition c = load_compo("/rm_validation/data_validation_evaluation_1.json")
      OptManager opt_manager = init_manager("/rm_validation")

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(c, "")

      report.errors.each { error ->
         println '16: '+ error
      }

      assert report.errors
   }

   void testDataValidationEval2()
   {
      Composition c = load_compo("/rm_validation/data_validation_evaluation_2.json")
      OptManager opt_manager = init_manager("/rm_validation")

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(c, "")

      report.errors.each { error ->
         println '17: '+ error
      }

      assert report.errors
   }

   // FIXME: the RM validator fails to detect an error in the terminology ID when the OPT has an external
   //        reference to the terminology: <referenceSetUri>terminology:SNOMED-CT?subset=problems</referenceSetUri>
   //        the data has terminology ID 'SNOMED-XXXXX' and that should be compared to 'SNOMED-CT' from the referenceSetUri
   //        in the OPT.
   void testDataValidationEval3()
   {
      Composition c = load_compo("/rm_validation/data_validation_evaluation_3.json")
      OptManager opt_manager = init_manager("/rm_validation")

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(c, "")

      report.errors.each { error ->
         println '18: '+ error
      }

      assert report.errors
   }

   // FIXME: this is not detecting the error that the name of a coded text doesn't correspond to the code selected,
   //        beign the code valid.
   void testDataValidationEval4()
   {
      Composition c = load_compo("/rm_validation/data_validation_evaluation_4.json")
      OptManager opt_manager = init_manager("/rm_validation")

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(c, "")

      report.errors.each { error ->
         println '19: '+ error
      }

      assert report.errors
   }

   void testDataValidationEPrescriptionFHIR()
   {
      Composition c = load_compo("/rm_validation/eprescription_fhir_invalid_opt.json")
      OptManager opt_manager = init_manager("/rm_validation")

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(c, "")

      report.errors.each { error ->
         println '20: '+ error
      }

      // I have fixed the problematic OPT to avoid issues with the name constraints for nodes at0002 inside at0113
      assert !report.errors
   }

   void testDataValidationMultipleStructured()
   {
      Composition c = load_compo("/rm_validation/test_multiple_structured_1.json")
      OptManager opt_manager = init_manager("/rm_validation")

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(c, "")

      report.errors.each { error ->
         println '21: '+ error
      }

      // I have fixed the problematic OPT to avoid issues with the name constraints for nodes at0002 inside at0113
      assert !report.errors
   }

   void testDataValidationASSECO()
   {
      Composition c = load_compo("/rm_validation/pulsecomposition.json")
      OptManager opt_manager = init_manager("/rm_validation")

      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(c, "")

      report.errors.each { error ->
         println '22: '+ error
      }

      // I have fixed the problematic OPT to avoid issues with the name constraints for nodes at0002 inside at0113
      assert report.errors.size() == 1
   }
}