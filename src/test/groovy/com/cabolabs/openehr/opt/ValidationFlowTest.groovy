package com.cabolabs.openehr.opt

import groovy.util.GroovyTestCase
import com.cabolabs.testing.TestUtils
import static com.cabolabs.testing.TestUtils.PS as PS
import com.cabolabs.openehr.formats.OpenEhrJsonParser
import com.cabolabs.openehr.validation.*
import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.rm_1_0_2.ehr.EhrStatus
import com.cabolabs.openehr.rm_1_0_2.common.directory.Folder

class ValidationFlowTest extends GroovyTestCase {

   void test_ehr_status_any_valid()
   {
      // LOAD OPT
      // def path = PS +"opts"+ PS + 'com.cabolabs.openehr_opt.namespaces.default' + PS +"ehr_status_any_en_v1.opt"
      // def opt = TestUtils.loadTemplate(path)
      // opt.complete()

      // PARSE JSON WITH RM SCHEMA VALIDATION
      def json_ehr_status = $/
         {
            "_type": "EHR_STATUS",
            "archetype_node_id": "openEHR-EHR-EHR_STATUS.generic.v1",
            "archetype_details": {
               "archetype_id": {
                  "value": "openEHR-EHR-EHR_STATUS.any.v1"
               },
               "template_id": {
                  "value": "ehr_status_any_en_v1"
               },
               "rm_version": "1.0.2"
            },
            "name": {
               "_type": "DV_TEXT",
               "value": "EHR Status"
            },
            "subject": {
               "external_ref": {
                  "id": {
                     "_type": "GENERIC_ID",
                     "value": "ins01",
                     "scheme": "id_scheme"
                  },
                  "namespace": "DEMOGRAPHIC",
                  "type": "PERSON"
               }
            },
            "is_modifiable": true,
            "is_queryable": true
         }
      /$

      def parser = new OpenEhrJsonParser(true) // does RM schema validation not API
      EhrStatus status = parser.parseEhrStatus(json_ehr_status)

      assert status


      // SETUP OPT REPO\
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(PS + "opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(status, 'com.cabolabs.openehr_opt.namespaces.default')

      assert !report.errors
   }

   void test_ehr_status_coded_valid()
   {
      // PARSE JSON WITH RM SCHEMA VALIDATION
      def json_ehr_status = $/
         {
            "_type": "EHR_STATUS",
            "archetype_node_id": "openEHR-EHR-EHR_STATUS.generic.v1",
            "archetype_details": {
               "archetype_id": {
                  "value": "openEHR-EHR-EHR_STATUS.coded.v1"
               },
               "template_id": {
                  "value": "ehr_status_coded_en_v1"
               },
               "rm_version": "1.0.2"
            },
            "name": {
               "_type": "DV_TEXT",
               "value": "EHR Status"
            },
            "subject": {
               "external_ref": {
                  "id": {
                     "_type": "GENERIC_ID",
                     "value": "ins01",
                     "scheme": "id_scheme"
                  },
                  "namespace": "DEMOGRAPHIC",
                  "type": "PERSON"
               }
            },
            "is_modifiable": true,
            "is_queryable": true,
            "other_details": {
               "_type": "ITEM_TREE",
               "name": {
                  "_type": "DV_TEXT",
                  "value": "Other details"
               },
               "archetype_node_id": "at0001"
            }
         }
      /$

      def parser = new OpenEhrJsonParser(true) // does RM schema validation not API
      EhrStatus status = parser.parseEhrStatus(json_ehr_status)

      println parser.getJsonValidationErrors()
      assert status


      // SETUP OPT REPO\
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(PS + "opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(status, 'com.cabolabs.openehr_opt.namespaces.default')

      println report.errors

      assert !report.errors
   }

   void test_ehr_status_coded_rm_fail()
   {
      // PARSE JSON WITH RM SCHEMA VALIDATION
      // NOTE: other_details SHOULD BE AN ITEM_TREE but it's an ITEM_LIST so the RM validator will throw an error
      def json_ehr_status = $/
         {
            "_type": "EHR_STATUS",
            "archetype_node_id": "openEHR-EHR-EHR_STATUS.generic.v1",
            "archetype_details": {
               "archetype_id": {
                  "value": "openEHR-EHR-EHR_STATUS.coded.v1"
               },
               "template_id": {
                  "value": "ehr_status_coded_en_v1"
               },
               "rm_version": "1.0.2"
            },
            "name": {
               "_type": "DV_TEXT",
               "value": "EHR Status"
            },
            "subject": {
               "external_ref": {
                  "id": {
                     "_type": "GENERIC_ID",
                     "value": "ins01",
                     "scheme": "id_scheme"
                  },
                  "namespace": "DEMOGRAPHIC",
                  "type": "PERSON"
               }
            },
            "is_modifiable": true,
            "is_queryable": true,
            "other_details": {
               "_type": "ITEM_LIST",
               "name": {
                  "_type": "DV_TEXT",
                  "value": "Other details"
               },
               "archetype_node_id": "at0001"
            }
         }
      /$

      def parser = new OpenEhrJsonParser(true) // does RM schema validation not API
      EhrStatus status = parser.parseEhrStatus(json_ehr_status)

      println parser.getJsonValidationErrors()
      assert status


      // SETUP OPT REPO\
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(PS + "opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(status, 'com.cabolabs.openehr_opt.namespaces.default')

      println report.errors

      assert report.errors
      assert report.errors[0].error == "type 'ITEM_LIST' is not allowed here, it should be in [ITEM_TREE]"
   }


   void test_folder_any_valid()
   {
      // PARSE JSON WITH RM SCHEMA VALIDATION
      def json_folder = $/
         {
            "_type": "FOLDER",
            "name": {
               "_type": "DV_TEXT",
               "value": "root"
            },
            "archetype_node_id": "openEHR-EHR-FOLDER.generic.v1",
            "archetype_details": {
               "archetype_id": {
                  "value": "openEHR-EHR-FOLDER.generic.v1"
               },
               "template_id": {
                  "value": "generic_folder"
               },
               "rm_version": "1.0.2"
            }
         }
      /$

      def parser = new OpenEhrJsonParser(true) // does RM schema validation not API
      Folder folder = parser.parseFolder(json_folder)

      assert folder


      // SETUP OPT REPO\
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(PS + "opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(folder, 'com.cabolabs.openehr_opt.namespaces.default')

      assert !report.errors
   }
}