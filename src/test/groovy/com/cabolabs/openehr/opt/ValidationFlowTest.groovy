package com.cabolabs.openehr.opt

import groovy.util.GroovyTestCase
import com.cabolabs.testing.TestUtils
import static com.cabolabs.testing.TestUtils.PS as PS
import com.cabolabs.openehr.formats.OpenEhrJsonParser
import com.cabolabs.openehr.validation.*
import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.rm_1_0_2.ehr.EhrStatus
import com.cabolabs.openehr.rm_1_0_2.common.directory.Folder
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.rm_1_0_2.demographic.Person
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText

// TODO: this test case is JSON only, we need to do the same with XML payloads!

class ValidationFlowTest extends GroovyTestCase {

   // ===================================================
   // EHR_STATUS

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


      // SETUP OPT REPO
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


      // SETUP OPT REPO
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


      // SETUP OPT REPO
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


   // ===================================================
   // FOLDERs

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


      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(PS + "opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(folder, 'com.cabolabs.openehr_opt.namespaces.default')

      assert !report.errors
   }

   void test_folder_with_items_any_valid()
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
            },
            "items": [
               {
                  "id": {
                     "_type": "HIER_OBJECT_ID",
                     "value": "replaceme"
                  },
                  "namespace": "my.system.id",
                  "type": "VERSIONED_COMPOSITION"
               }
            ]
         }
      /$

      def parser = new OpenEhrJsonParser(true) // does RM schema validation not API
      Folder folder = parser.parseFolder(json_folder)

      assert folder


      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(PS + "opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(folder, 'com.cabolabs.openehr_opt.namespaces.default')

      assert !report.errors
   }

   void test_folder_with_subfolders_any_valid()
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
            },
            "folders": [
               {
                  "_type": "FOLDER",
                  "name": {
                     "_type": "DV_TEXT",
                     "value": "emergency"
                  },
                  "archetype_node_id": "openEHR-EHR-FOLDER.generic.v1",
                  "folders": [
                     {
                        "_type": "FOLDER",
                        "name": {
                           "_type": "DV_TEXT",
                           "value": "episode_x"
                        },
                        "archetype_node_id": "openEHR-EHR-FOLDER.generic.v1",
                        "folders": [
                           {
                              "_type": "FOLDER",
                              "name": {
                                 "_type": "DV_TEXT",
                                 "value": "summary_compo_x"
                              },
                              "archetype_node_id": "openEHR-EHR-FOLDER.generic.v1"
                           }
                        ]
                     },
                     {
                        "_type": "FOLDER",
                        "name": {
                           "_type": "DV_TEXT",
                           "value": "episode_y"
                        },
                        "archetype_node_id": "openEHR-EHR-FOLDER.generic.v1",
                        "folders": [
                           {
                              "_type": "FOLDER",
                              "name": {
                                 "_type": "DV_TEXT",
                                 "value": "summary_compo_y"
                              },
                              "archetype_node_id": "openEHR-EHR-FOLDER.generic.v1"
                           }
                        ]
                     }
                  ]
               },
               {
                  "_type": "FOLDER",
                  "name": {
                     "_type": "DV_TEXT",
                     "value": "hospitalization"
                  },
                  "archetype_node_id": "openEHR-EHR-FOLDER.generic.v1",
                  "folders": [
                     {
                        "_type": "FOLDER",
                        "name": {
                           "_type": "DV_TEXT",
                           "value": "summary_compo_z"
                        },
                        "archetype_node_id": "openEHR-EHR-FOLDER.generic.v1"
                     }
                  ]
               },
               {
                  "_type": "FOLDER",
                  "name": {
                     "_type": "DV_TEXT",
                     "value": "foldername-w-special-chars"
                  },
                  "archetype_node_id": "openEHR-EHR-FOLDER.generic.v1"
               }
            ]
         }
      /$

      def parser = new OpenEhrJsonParser(true) // does RM schema validation not API
      Folder folder = parser.parseFolder(json_folder)

      assert folder


      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(PS + "opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(folder, 'com.cabolabs.openehr_opt.namespaces.default')

      assert !report.errors
   }


   void test_folder_any_missing_archetype_details()
   {
      // PARSE JSON WITH RM SCHEMA VALIDATION
      def json_folder = $/
         {
            "_type": "FOLDER",
            "name": {
               "_type": "DV_TEXT",
               "value": "root"
            },
            "archetype_node_id": "openEHR-EHR-FOLDER.generic.v1"
         }
      /$

      def parser = new OpenEhrJsonParser(true) // does RM schema validation not API

      // NOTE: the exception happens here because the parser does a schema validation which requires to know the archetype_details.rm_version
      //       we need to create an insteance of the RM Folder to test the RmValidator also fails if the archetype_details is not there
      shouldFail {
         parser.parseFolder(json_folder)
      }
   }

   /**
    * Instead of parsing the folder like `test_folder_any_missing_archetype_details` it creates an RM Folder instance
    * without the archetype details, so the RmValidator fails.
    */
   void test_folder_rm_missing_archetype_details()
   {
      def folder = new Folder(
         name: new DvText(
            value: 'root'
         ),
         archetype_node_id: 'openEHR-EHR-FOLDER.generic.v1'
      )

      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(PS + "opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(folder, 'com.cabolabs.openehr_opt.namespaces.default')

      assert report.errors
   }


   // ===================================================
   // COMPOSITIONs

   void test_compo_minimal_action_valid()
   {
      // PARSE JSON WITH RM SCHEMA VALIDATION
      String path = PS +"canonical_json"+ PS +"minimal_action.json"
	   File file = new File(getClass().getResource(path).toURI())
      def json_compo = file.text

      def parser = new OpenEhrJsonParser(true) // does RM schema validation not API
      Composition compo = parser.parseJson(json_compo)

      assert compo


      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(PS + "opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(compo, 'com.cabolabs.openehr_opt.namespaces.default')

      assert !report.errors
   }

   void test_compo_test_all_datatypes_en_valid()
   {
      // PARSE JSON WITH RM SCHEMA VALIDATION
      String path = PS +"canonical_json"+ PS +"test_all_datatypes_en.json"
	   File file = new File(getClass().getResource(path).toURI())
      def json_compo = file.text

      def parser = new OpenEhrJsonParser(true) // does RM schema validation not API
      Composition compo = parser.parseJson(json_compo)

      assert compo


      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(PS + "opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(compo, 'com.cabolabs.openehr_opt.namespaces.default')

      assert !report.errors
   }

   void test_compo_test_all_datatypes_en_invalid()
   {
      // PARSE JSON WITH RM SCHEMA VALIDATION
      String path = PS +"canonical_json"+ PS +"test_all_datatypes_en_constraints_violated.json"
	   File file = new File(getClass().getResource(path).toURI())
      def json_compo = file.text

      def parser = new OpenEhrJsonParser(true) // does RM schema validation not API
      Composition compo = parser.parseJson(json_compo)

      assert compo


      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(PS + "opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(compo, 'com.cabolabs.openehr_opt.namespaces.default')

      assert report.errors
      assert report.errors.size() == 4

      def err

      err = report.errors.find { it.path == "/content[0]/data/events[0]/data/items[9]/value/lower/magnitude" }

      assert err.error == "value '5' is not contained in the range '10..100'"


      err = report.errors.find { it.path == "/content[0]/data/events[0]/data/items[9]/value/upper/magnitude" }

      assert err.error == "value '10' is not contained in the range '50..200'"


      err = report.errors.find { it.path == "/content[0]/data/events[0]/data/items[16]/value/issuer" }

      assert err.error == "/content[0]/data/events[0]/data/items[16]/value 'Hospital de Clinicas' doesn't match pattern 'issuerA'"


      err = report.errors.find { it.path == "/content[0]/data/events[0]/data/items[16]/value/type" }

      assert err.error == "/content[0]/data/events[0]/data/items[16]/value 'LOCALID' doesn't match pattern 'typeB'"
   }


   // ===================================================
   // DEMOGRAPHIC


   void test_person_valid()
   {
      // PARSE JSON WITH RM SCHEMA VALIDATION
      String path = PS +"canonical_json"+ PS +"generic_person.json"
	   File file = new File(getClass().getResource(path).toURI())
      def json_person = file.text

      def parser = new OpenEhrJsonParser(true) // does RM schema validation not API
      Person person = parser.parseJson(json_person)

      println person

      println parser.getJsonValidationErrors()
      
      assert person



      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource(PS + "opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(person, 'com.cabolabs.openehr_opt.namespaces.default')

      println report.errors

      assert !report.errors
   }
}