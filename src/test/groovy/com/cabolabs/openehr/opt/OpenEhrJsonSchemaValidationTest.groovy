package com.cabolabs.openehr.opt

import groovy.json.JsonSlurper
import com.cabolabs.openehr.opt.instance_validation.JsonInstanceValidation
import com.networknt.schema.*

class OpenEhrJsonSchemaValidationTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")

   void testValidation102AllValid()
   {
      //def uri = 'https://gist.githubusercontent.com/pieterbos/81651d2d7a5041a130ecb21b0a852e39/raw/2f31b9c7067bccf192256358da868ee8fbc7239a/OpenEHR%2520RM%2520json%2520schema,%2520with%2520default%2520instances%2520of%2520objects%2520addedcorrectly.json'
      //def jsonValidator = new JsonInstanceValidation(uri)
      def jsonValidator = new JsonInstanceValidation('rm', '1.0.2') // TODO: obtain from file?

      // these files are loaded from the resources included from the JAR in the classpath
      def files = [
         'canonical_json/admin.json',
         'canonical_json/amd_assessment.en.v1.json',
         'canonical_json/diagnose.de.v1.json',
         'canonical_json/experimental_respiratory_parameters_document.json',
         'canonical_json/intensivmedizinisches_monitoring_korpertemperatur.json',
         'canonical_json/lab_order.json',
         'canonical_json/lab_results.json',
         'canonical_json/minimal_action_2.en.v1_instance_6866896.json',
         'canonical_json/minimal_action.json',
         'canonical_json/minimal_evaluation.json',
         'canonical_json/minimal_observation.en.v1_instance_7696347.json',
         'canonical_json/nested.json',
         'canonical_json/opt_tester.en.v1_instance_1482167.json',
         'canonical_json/oximetria_obs.json',
         'canonical_json/physical_activity.json',
         'canonical_json/prozedur.json',
         'canonical_json/referral.json',
         'canonical_json/test_all_datatypes_en.json',
         'canonical_json/vital_signs_pathfinder_demo.en.v1_instance_3602591.json',
         'canonical_json/ehr_status.json',
         'canonical_json/subfolders_in_directory_with_details_items.json'
      ]

      InputStream ins
      Set<ValidationMessage> errors

      def slurper = new JsonSlurper()
      def map

      // https://github.com/networknt/json-schema-validator/blob/master/src/test/java/com/networknt/schema/V7JsonSchemaTest.java
      files.each { testCaseFile ->

         println testCaseFile

         //final URI testCaseFileUri = URI.create("classpath:" + testCaseFile)
         ins = Thread.currentThread().getContextClassLoader().getResourceAsStream(testCaseFile)

         if (!ins) throw new Exception("Test file $testCaseFile not found")


         map = slurper.parseText(ins.text) // json string > map

         switch (map._type)
         {
            case ["ORIGINAL_VERSION", "IMPORTED_VERSION"]:
               println map.data.archetype_details.rm_version
            break
            default: // NOTE: archetype_details should be mandatory in root types: COMPOSITION, EHR_STATUS, FOLDER, PARTY subtypes.
               println map.archetype_details.rm_version
         }


         // https://fasterxml.github.io/jackson-databind/javadoc/2.7/com/fasterxml/jackson/databind/ObjectMapper.html#readTree(java.io.InputStream)
         //json = mapper.readTree(ins)
         errors = jsonValidator.validate(map) // transforms map to json node then validates

         assert !errors

         // TODO: asserts on introduced errors (another test)
         // def out = JsonOutput.toJson(errors)
         // out = JsonOutput.prettyPrint(out)
         // println testCaseFile
         // println out
      }
   }

   void testValidation102InvalidAdmin()
   {
      //def uri = 'https://gist.githubusercontent.com/pieterbos/81651d2d7a5041a130ecb21b0a852e39/raw/2f31b9c7067bccf192256358da868ee8fbc7239a/OpenEHR%2520RM%2520json%2520schema,%2520with%2520default%2520instances%2520of%2520objects%2520addedcorrectly.json'
      //def jsonValidator = new JsonInstanceValidation(uri)
      def jsonValidator = new JsonInstanceValidation()

      // these files are loaded from the resources included from the JAR in the classpath
      def testCaseFile = 'canonical_json/invalid/admin.json'

      def slurper = new JsonSlurper()
      def map

      //final URI testCaseFileUri = URI.create("classpath:" + testCaseFile)
      InputStream ins = Thread.currentThread().getContextClassLoader().getResourceAsStream(testCaseFile)

      if (!ins) throw new Exception("Test file $testCaseFile not found")

      map = slurper.parseText(ins.text) // json string > map

      def rm_version = map.archetype_details.rm_version

      Set<ValidationMessage> errors = jsonValidator.validate(map) // transforms map to json node then validates

      assert errors

      def err_name = errors.find{ it.message == "\$.name: is missing but it is required" }

      assert err_name
      assert err_name.type == "required"
   }

   void testValidation102InvalidEhrStatus()
   {
      //def uri = 'https://gist.githubusercontent.com/pieterbos/81651d2d7a5041a130ecb21b0a852e39/raw/2f31b9c7067bccf192256358da868ee8fbc7239a/OpenEHR%2520RM%2520json%2520schema,%2520with%2520default%2520instances%2520of%2520objects%2520addedcorrectly.json'
      //def jsonValidator = new JsonInstanceValidation(uri)
      def jsonValidator = new JsonInstanceValidation()

      // these files are loaded from the resources included from the JAR in the classpath
      def testCaseFile = 'canonical_json/invalid/ehr_status.json'

      def slurper = new JsonSlurper()
      def map

      //final URI testCaseFileUri = URI.create("classpath:" + testCaseFile)
      InputStream ins = Thread.currentThread().getContextClassLoader().getResourceAsStream(testCaseFile)

      if (!ins) throw new Exception("Test file $testCaseFile not found")

      map = slurper.parseText(ins.text) // json string > map

      //def rm_version = map.archetype_details.rm_version

      Set<ValidationMessage> errors = jsonValidator.validate(map) // transforms map to json node then validates

      assert errors

      def err_archetype_details = errors.find{ it.message == "\$.archetype_details: is missing but it is required" }

      assert err_archetype_details
      assert err_archetype_details.type == "required"
   }

   void testValidation102ContributionForImportWithTwoVersions()
   {
      def jsonValidator = new JsonInstanceValidation('api', '1.0.2')
      def testCaseFile = 'canonical_json/contribution_for_import.json'

      def slurper = new JsonSlurper()
      def map

      InputStream ins = Thread.currentThread().getContextClassLoader().getResourceAsStream(testCaseFile)

      if (!ins) throw new Exception("Test file $testCaseFile not found")

      map = slurper.parseText(ins.text)

      assert map._type == "CONTRIBUTION"
      assert map.versions instanceof List
      assert map.versions.size() == 2

      Set<ValidationMessage> errors = jsonValidator.validate(map)

      assert !errors : errors*.message.join('\n')
   }
}
