package com.cabolabs.openehr.opt

import groovy.util.GroovyTestCase
import com.cabolabs.testing.TestUtils
import static com.cabolabs.testing.TestUtils.PS as PS
import com.cabolabs.openehr.formats.*
import com.cabolabs.openehr.validation.*
import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.rm_1_0_2.ehr.Ehr
import com.cabolabs.openehr.rm_1_0_2.ehr.EhrStatus
import com.cabolabs.openehr.rm_1_0_2.common.directory.Folder
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.rm_1_0_2.demographic.Person
import com.cabolabs.openehr.rm_1_0_2.demographic.Organization
import com.cabolabs.openehr.rm_1_0_2.demographic.Role
import com.cabolabs.openehr.rm_1_0_2.demographic.Group
import com.cabolabs.openehr.rm_1_0_2.demographic.Agent
import com.cabolabs.openehr.rm_1_0_2.demographic.PartyRelationship
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText
import com.cabolabs.openehr.rm_1_0_2.support.identification.*
import com.cabolabs.openehr.dto_1_0_2.ehr.*
import com.cabolabs.openehr.dto_1_0_2.demographic.*

import com.cabolabs.openehr.opt.serializer.*

// TODO: this test case is JSON only, we need to do the same with XML payloads!

class ValidationFlowTest extends GroovyTestCase {

   // ====================================================
   // EHR

   void test_ehr_rm_valid()
   {
      // NOTE: RM requires EHR.compositions [1..1] and EHR.contributions [1..1]
      //       in the 1.0.2 schema those are not mandatory.
      def json_ehr = $/
         {
            "_type": "EHR",
            "system_id": {
               "_type": "HIER_OBJECT_ID",
               "value": "f885dfca-4ac0-45b3-808f-6045d613bd86"
            },
            "ehr_id": {
               "_type": "HIER_OBJECT_ID",
               "value": "70038ea5-464e-4d08-9b55-3975aa796177"
            },
            "ehr_status": {
               "_type": "LOCATABLE_REF",
               "namespace": "EHR",
               "type": "VERSIONED_EHR_STATUS",
               "path": "/ehr_status",
               "id": {
                  "_type": "OBJECT_VERSION_ID",
                  "value": "11627915-e27d-4478-82b9-14e871ee1466::com.cabolabs.conformance::1"
               }
            },
            "ehr_access": {
               "_type": "LOCATABLE_REF",
               "namespace": "EHR",
               "type": "VERSIONED_EHR_ACCESS",
               "path": "/ehr_access",
               "id": {
                  "_type": "OBJECT_VERSION_ID",
                  "value": "720707c8-7a57-473b-a9a6-dbb76dedd8f2::com.cabolabs.conformance::1"
               }
            },
            "time_created": {
               "value": "2015-01-20T19:30:22.765+01:00"
            }
         }
      /$

      def parser = new OpenEhrJsonParser(true) // does RM schema validation
      Ehr ehr = parser.parseEhr(json_ehr)

      assert ehr

      assert ehr.time_created.value == "2015-01-20T19:30:22.765+01:00"

      assert ehr.ehr_id.value == "70038ea5-464e-4d08-9b55-3975aa796177"

      // NOTE: ehr can't be validated via OPT, but it's components can, but only if it's the API payload because
      //       on the RM payload it only has the REFs (e.g. for ehr_status)
   }

   void test_ehr_api_valid()
   {
      def json_ehr = $/
         {
            "_type": "EHR",
            "system_id": {
               "_type": "HIER_OBJECT_ID",
               "value": "f885dfca-4ac0-45b3-808f-6045d613bd86"
            },
            "ehr_id": {
               "_type": "HIER_OBJECT_ID",
               "value": "70038ea5-464e-4d08-9b55-3975aa796177"
            },
            "ehr_status": {
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
                  "value": "Generic Status"
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
            },
            "ehr_access": {
               "_type": "EHR_ACCESS",
               "archetype_node_id": "openEHR-EHR-EHR_STATUS.generic.v1",
               "name": {
                  "_type": "DV_TEXT",
                  "value": "EHR Status"
               },
               "settings": {
                  "value": "dummy"
               }
            },
            "time_created": {
               "value": "2015-01-20T19:30:22.765+01:00"
            }
         }
      /$

      def parser = new OpenEhrJsonParser(true) // does RM schema validation
      EhrDto ehr = parser.parseEhrDto(json_ehr)

      // parsed OK (validation doesn't retrieve any errors)
      assert ehr

      assert ehr.time_created.value == "2015-01-20T19:30:22.765+01:00"

      assert ehr.ehr_id.value == "70038ea5-464e-4d08-9b55-3975aa796177"

      //println groovy.json.JsonOutput.toJson(ehr.ehr_status)

      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR (EHR_STATUS only)
      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(ehr.ehr_status, 'com.cabolabs.openehr_opt.namespaces.default')

      assert !report.errors
   }

   void test_ehr_rm_schema_invalid()
   {
      // NOTE: RM requires EHR.compositions [1..1] and EHR.contributions [1..1]
      //       in the 1.0.2 schema those are not mandatory.
      def json_ehr = $/
         {
            "_type": "EHR",
            "system_id": {
               "_type": "HIER_OBJECT_ID",
               "value": "f885dfca-4ac0-45b3-808f-6045d613bd86"
            },
            "ehr_id": {
               "value": 123
            },
            "ehr_status": {
               "_type": "LOCATABLE_REF",
               "namespace": "EHR",
               "type": "VERSIONED_EHR_STATUS",
               "path": "/ehr_status",
               "id": {
                  "_type": "OBJECT_VERSION_ID",
                  "value": "11627915-e27d-4478-82b9-14e871ee1466::com.cabolabs.conformance::1"
               }
            },
            "ehr_access": {
               "_type": "LOCATABLE_REF",
               "namespace": "EHR",
               "type": "VERSIONED_EHR_ACCESS",
               "path": "/ehr_access",
               "id": {
                  "_type": "OBJECT_VERSION_ID",
                  "value": "720707c8-7a57-473b-a9a6-dbb76dedd8f2::com.cabolabs.conformance::1"
               }
            }
         }
      /$

      def parser = new OpenEhrJsonParser(true) // does RM schema validation
      Ehr ehr = parser.parseEhr(json_ehr)

      def errors = parser.getJsonValidationErrors()

      assert errors.size() == 2

      assert errors.find { it.arguments == ['time_created'] }.message == '$.time_created: is missing but it is required'

      assert errors.find { it.path == '$.ehr_id.value' }.message == '$.ehr_id.value: integer found, string expected'

      assert !ehr

      // NOTE: ehr can't be validated via OPT, but it's components can, but only if it's the API payload because
      //       on the RM payload it only has the REFs (e.g. for ehr_status)
   }

   void test_ehr_api_schema_invalid()
   {
      def json_ehr = $/
         {
            "_type": "EHR",
            "system_id": {
               "_type": "HIER_OBJECT_ID",
               "value": "f885dfca-4ac0-45b3-808f-6045d613bd86"
            },
            "ehr_id": {
               "_type": "HIER_OBJECT_ID",
               "value": "70038ea5-464e-4d08-9b55-3975aa796177"
            },
            "ehr_status": {
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
               "is_modifiable": true,
               "is_queryable": true
            }
         }
      /$

      def parser = new OpenEhrJsonParser(true) // does RM schema validation
      EhrDto ehr = parser.parseEhrDto(json_ehr)

      def errors = parser.getJsonValidationErrors()

      assert errors.size() == 2

      assert errors.find { it.arguments == ['time_created'] }.message == '$.time_created: is missing but it is required'

      assert errors.find { it.arguments == ['subject'] }.message == '$.ehr_status.subject: is missing but it is required'

      assert !ehr
   }

   // For the next 2 methods we should create an instance from scratch instead of parsing a JSON
   void test_ehr_rm_rm_invalid()
   {
      def ehr = new Ehr(
         system_id: new HierObjectId(
            value: '8b201872-9d95-4ffa-adfe-4eaa4cfaecf0'
         )
      )

      RmValidator2 validator = new RmValidator2()
      RmValidationReport report = validator.dovalidate(ehr)

      assert report.errors.find{ it.path == '/ehr_id' }.error == 'attribute is not present but is required'
      assert report.errors.find{ it.path == '/ehr_status' }.error == 'attribute is not present but is required'
      assert report.errors.find{ it.path == '/ehr_access' }.error == 'attribute is not present but is required'
      assert report.errors.find{ it.path == '/time_created' }.error == 'attribute is not present but is required'

      assert report.errors.size() == 4
   }

   void test_ehr_api_rm_invalid()
   {
      def ehr = new EhrDto(
         system_id: new HierObjectId(
            value: '8b201872-9d95-4ffa-adfe-4eaa4cfaecf0'
         )
      )

      RmValidator2 validator = new RmValidator2()
      RmValidationReport report = validator.dovalidate(ehr)

      assert report.errors.find{ it.path == '/ehr_id' }.error == 'attribute is not present but is required'
      assert report.errors.find{ it.path == '/ehr_status' }.error == 'attribute is not present but is required'
      assert report.errors.find{ it.path == '/time_created' }.error == 'attribute is not present but is required'

      assert report.errors.size() == 3
   }

   // ===================================================
   // EHR_STATUS

   void test_ehr_status_any_valid()
   {
      // LOAD OPT
      // def path = "opts"+ PS + 'com.cabolabs.openehr_opt.namespaces.default' + PS +"ehr_status_any_en_v1.opt"
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
               "value": "Generic Status"
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
      EhrStatus status = parser.parseJson(json_ehr_status)

      assert status


      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator2 validator = new RmValidator2(opt_manager)
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
               "value": "status"
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
                  "value": "tree"
               },
               "archetype_node_id": "at0001"
            }
         }
      /$

      def parser = new OpenEhrJsonParser(true) // does RM schema validation not API
      EhrStatus status = parser.parseJson(json_ehr_status)

      //println parser.getJsonValidationErrors()
      assert status


      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(status, 'com.cabolabs.openehr_opt.namespaces.default')

      //println report.errors

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
               "value": "status"
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

      def parser = new OpenEhrJsonParser(true) // NOTE: does RM schema validation not API
      EhrStatus status = parser.parseJson(json_ehr_status) // NOTE: this parses OK because it doesn't verifies the OPT constraints

      //println parser.getJsonValidationErrors()
      assert status


      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(status, 'com.cabolabs.openehr_opt.namespaces.default')

      //println report.errors

      assert report.errors
      assert report.errors[0].error == "type 'ITEM_LIST' is not allowed here, it should be in [ITEM_TREE]"
   }

   void test_ehr_status_empty_subject()
   {
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
               "value": "Generic Status"
            },
            "subject": {
            },
            "is_modifiable": true,
            "is_queryable": true
         }
      /$

      def parser = new OpenEhrJsonParserQuick(true) // does RM schema validation not API
      EhrStatus status = parser.parseJson(json_ehr_status)

      assert status
      assert status.subject


      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(status, 'com.cabolabs.openehr_opt.namespaces.default')

      assert !report.errors
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
               "value": "generic"
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
      Folder folder = parser.parseJson(json_folder)

      assert folder


      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(folder, 'com.cabolabs.openehr_opt.namespaces.default')

      assert !report.errors
   }

   void test_folder_any_valid_api()
   {
      // PARSE JSON WITH API SCHEMA VALIDATION
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

      def parser = new OpenEhrJsonParser(true)
      parser.setSchemaFlavorAPI()
      Folder folder = parser.parseJson(json_folder)

      println parser.getJsonValidationErrors()
      assert folder
   }

   void test_folder_with_items_any_valid()
   {
      // PARSE JSON WITH RM SCHEMA VALIDATION
      // NOTE: the schema is not actually validating the HIER_OBJECT_ID.value format is a UID[::string]
      def json_folder = $/
         {
            "_type": "FOLDER",
            "name": {
               "_type": "DV_TEXT",
               "value": "generic"
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
                     "value": "7496d24d-feb5-4b68-a91f-ef4b7eb86394"
                  },
                  "namespace": "EHR",
                  "type": "VERSIONED_COMPOSITION"
               }
            ]
         }
      /$

      def parser = new OpenEhrJsonParser(true) // does RM schema validation not API
      Folder folder = parser.parseJson(json_folder)

      println parser.getJsonValidationErrors()

      assert folder


      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
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
               "value": "generic"
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
      Folder folder = parser.parseJson(json_folder)

      assert folder


      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
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
         parser.parseJson(json_folder)
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
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(folder, 'com.cabolabs.openehr_opt.namespaces.default')

      assert report.errors
   }


   // ===================================================
   // COMPOSITIONs

   void test_compo_validation_missing_node()
   {
      String path = "/opts/test_validation_missing_node/composition.json"
      File file = new File(getClass().getResource(path).toURI())
      def json_compo = file.text

      def parser = new OpenEhrJsonParser(true) // does RM schema validation not API
      Composition compo = parser.parseJson(json_compo)

      assert compo

      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)

      // SETUP RM VALIDATOR
      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(compo, 'test_validation_missing_node')

      assert report.errors.size() == 5

      def err

      err = report.errors.find { it.dataPath == "/content(0)/name" }

      assert err.error == "expected name is 'Test all datatypes' and actual name is 'Blood Pressure'"

      err = report.errors.find { it.dataPath == "/content(0)/data/name" }

      assert err.error == "expected name is 'Event Series' and actual name is 'history'"

      err = report.errors.find { it.dataPath == "/content(0)/data/events(0)/name" }

      assert err.error == "expected name is 'Cualquier evento' and actual name is 'any event'"

      err = report.errors.find { it.dataPath == "/content(0)/data/events(1)/name" }

      assert err.error == "expected name is 'Cualquier evento' and actual name is 'any event'"

      err = report.errors.find { it.dataPath == "/content(0)/data/events(2)/name" }

      assert err.error == "expected name is 'Cualquier evento' and actual name is 'any event'"
   }

   void test_compo_minimal_action_valid()
   {
      // PARSE JSON WITH RM SCHEMA VALIDATION
      String path = "/canonical_json/minimal_action.json"
      File file = new File(getClass().getResource(path).toURI())
      def json_compo = file.text

      def parser = new OpenEhrJsonParser(true) // does RM schema validation not API
      Composition compo = parser.parseJson(json_compo)

      assert compo


      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
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
      String path = "/canonical_json/test_all_datatypes_en.json"
      File file = new File(getClass().getResource(path).toURI())
      def json_compo = file.text

      def parser = new OpenEhrJsonParser(true) // does RM schema validation not API
      Composition compo = parser.parseJson(json_compo)

      assert compo


      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
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
      String path = "/canonical_json/test_all_datatypes_en_constraints_violated.json"
      File file = new File(getClass().getResource(path).toURI())
      def json_compo = file.text

      def parser = new OpenEhrJsonParser(true) // does RM schema validation not API
      Composition compo = parser.parseJson(json_compo)

      assert compo


      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator validator = new RmValidator(opt_manager)
      RmValidationReport report = validator.dovalidate(compo, 'com.cabolabs.openehr_opt.namespaces.default')

      assert report.errors
      assert report.errors.size() == 4

      def err

      err = report.errors.find { it.path == "/content(0)/data/events(0)/data/items(9)/value/lower/magnitude" }

      assert err.error == "value '5' is not contained in the range '10..100'"


      err = report.errors.find { it.path == "/content(0)/data/events(0)/data/items(9)/value/upper/magnitude" }

      assert err.error == "value '10' is not contained in the range '50..200'"


      err = report.errors.find { it.path == "/content(0)/data/events(0)/data/items(16)/value/issuer" }

      assert err.error == "value 'Hospital de Clinicas' doesn't match pattern 'issuerA'"


      err = report.errors.find { it.path == "/content(0)/data/events(0)/data/items(16)/value/type" }

      assert err.error == "value 'LOCALID' doesn't match pattern 'typeB'"
   }



   void test_compo_vital_signs_monitoring()
   {
      String path = "/canonical_json/vital_signs_monitoring.json"
      File file = new File(getClass().getResource(path).toURI())
      def json_compo = file.text

      def parser = new OpenEhrJsonParserQuick(true) // does RM schema validation not API
      Composition compo = parser.parseJson(json_compo)

      assert compo

      assert compo.content.size() == 5

      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)

      // SETUP RM VALIDATOR
      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(compo, 'com.cabolabs.openehr_opt.namespaces.default')

      assert report.errors.size() == 0

      def locatable_string = new OpenEhrJsonSerializer(true).serialize(compo)

      println locatable_string
   }


   // ===================================================
   // DEMOGRAPHIC

   void test_person_valid()
   {
      // PARSE JSON WITH RM SCHEMA VALIDATION
      String path = "/canonical_json/demographic/generic_person.json"
      File file = new File(getClass().getResource(path).toURI())
      def json_person = file.text

      def parser = new OpenEhrJsonParserQuick(true) // does RM schema validation not API
      Person person = parser.parseJson(json_person)

      //println parser.getJsonValidationErrors()

      assert person

      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(person, 'com.cabolabs.openehr_opt.namespaces.default')

      //println report.errors

      assert !report.errors
   }

   void test_person_complete_valid()
   {
      // PARSE JSON WITH RM SCHEMA VALIDATION
      String path = "/canonical_json/demographic/person_complete.json"
      File file = new File(getClass().getResource(path).toURI())
      def json_person = file.text

      def parser = new OpenEhrJsonParserQuick(true)
      parser.setSchemaFlavorAPI()
      PersonDto person = parser.parsePersonDto(json_person)
      //PersonDto person = parser.parseActorDto(json_person) // This also works

      println parser.getJsonValidationErrors()

      assert person

      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(person, 'com.cabolabs.openehr_opt.namespaces.default')

      println report.errors

      assert !report.errors
   }

   void test_organization_valid()
   {
      // PARSE JSON WITH RM SCHEMA VALIDATION
      String path = "/canonical_json/demographic/generic_organization.json"
      File file = new File(getClass().getResource(path).toURI())
      def json_organization = file.text

      def parser = new OpenEhrJsonParserQuick(true) // does RM schema validation not API
      parser.setSchemaFlavorAPI() // testing if schema flavor is API
      Organization organization = parser.parseJson(json_organization)

      //println organization

      //println parser.getJsonValidationErrors()

      assert organization

      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(organization, 'com.cabolabs.openehr_opt.namespaces.default')

      //println report.errors

      assert !report.errors
   }

   void test_group_valid()
   {
      // PARSE JSON WITH RM SCHEMA VALIDATION
      String path = "/canonical_json/demographic/generic_group.json"
      File file = new File(getClass().getResource(path).toURI())
      def json_group = file.text

      def parser = new OpenEhrJsonParserQuick(true) // does RM schema validation not API
      Group group = parser.parseJson(json_group)

      //println person

      //println parser.getJsonValidationErrors()

      assert group


      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(group, 'com.cabolabs.openehr_opt.namespaces.default')

      //println report.errors

      assert !report.errors
   }

   void test_agent_valid()
   {
      // PARSE JSON WITH RM SCHEMA VALIDATION
      String path = "/canonical_json/demographic/generic_agent.json"
      File file = new File(getClass().getResource(path).toURI())
      def json_agent = file.text

      def parser = new OpenEhrJsonParserQuick(true) // does RM schema validation not API
      Agent agent = parser.parseJson(json_agent)

      // FIXME: this should fail because the uid is mandatory and it's not in the JSON

      //println parser.getJsonValidationErrors()

      assert agent

      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(agent, 'com.cabolabs.openehr_opt.namespaces.default')

      assert !report.errors
   }

   void test_role_valid()
   {
      // PARSE JSON WITH RM SCHEMA VALIDATION
      String path = "/canonical_json/demographic/generic_role.json"
      File file = new File(getClass().getResource(path).toURI())
      def json_role = file.text

      def parser = new OpenEhrJsonParser(true) // does RM schema validation not API
      Role role = parser.parseJson(json_role)

      //println person

      //println parser.getJsonValidationErrors()

      assert role

      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR
      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(role, 'com.cabolabs.openehr_opt.namespaces.default')

      //println report.errors

      assert !report.errors
   }

   void test_person_api_valid()
   {
      def json_person_dto = $/
         {
            "_type": "PERSON",
            "name": {
               "_type": "DV_TEXT",
               "value": "Pablo Pazos"
            },
            "archetype_node_id": "openEHR-DEMOGRAPHIC-PERSON.generic.v1",
            "archetype_details": {
               "archetype_id": {
                  "value": "openEHR-DEMOGRAPHIC-PERSON.generic.v1"
               },
               "template_id": {
                  "value": "generic_person"
               },
               "rm_version": "1.0.2"
            },
            "roles": [
               {
                  "name": {
                     "_type": "DV_TEXT",
                     "value": "Patient"
                  },
                  "archetype_details": {
                     "archetype_id": {
                        "_type": "ARCHETYPE_ID",
                        "value": "openEHR-DEMOGRAPHIC-ROLE.generic_role.v1"
                     },
                     "template_id": {
                        "_type": "TEMPLATE_ID",
                        "value": "generic_role"
                     },
                     "rm_version": "1.0.2"
                  },
                  "archetype_node_id": "openEHR-DEMOGRAPHIC-ROLE.generic.v1",
                  "identities": [
                     {
                        "_type": "PARTY_IDENTITY",
                        "name": {
                           "_type": "DV_TEXT",
                           "value": "identity"
                        },
                        "archetype_node_id": "at0004",
                        "details": {
                           "_type": "ITEM_TREE",
                           "name": {
                              "_type": "DV_TEXT",
                              "value": "tree"
                           },
                           "archetype_node_id": "at0005",
                           "items": [
                              {
                                 "_type": "ELEMENT",
                                 "name": {
                                    "_type": "DV_TEXT",
                                    "value": "name"
                                 },
                                 "archetype_node_id": "at0006",
                                 "value": {
                                    "_type": "DV_TEXT",
                                    "value": "patient"
                                 }
                              }
                           ]
                        }
                     }
                  ]
               }
            ],
            "identities": [
               {
                  "_type": "PARTY_IDENTITY",
                  "name": {
                     "_type": "DV_TEXT",
                     "value": "identity"
                  },
                  "archetype_node_id": "at0004",
                  "details": {
                     "_type": "ITEM_TREE",
                     "name": {
                        "_type": "DV_TEXT",
                        "value": "tree"
                     },
                     "archetype_node_id": "at0005",
                     "items": [
                        {
                           "_type": "ELEMENT",
                           "name": {
                              "_type": "DV_TEXT",
                              "value": "name"
                           },
                           "archetype_node_id": "at0006",
                           "value": {
                              "_type": "DV_TEXT",
                              "value": "PiV.POWOXMcONGEMaWuJOkdsW.WLvJOia hLPIyhIIk DA mQAtKBfxJUFWrjVIYtNuJnneQCsxEgCXzQfI KjvGHOfPWaZvvtaCdIQOPtOYKEmES BgWSbYfBFQazoHXgujVFcd.GcirgvNtUdlKThXI VIjlBzxYbtJY.Nfg,DqVGRmfJbLeOiJAyBuDHV.tPFK,XbsAesIqRUTnulF XoASDnVwMMptttPjGXxmrNPvbAHSwqwyyArtrfUIQXXvdduhACRVhjD,aRBidoM,SzNmphdpwyIDdpUt,dDRMR"
                           }
                        }
                     ]
                  }
               }
            ]
         }
      /$

      def parser = new OpenEhrJsonParserQuick(true) // does schema validation
      parser.setSchemaFlavorAPI()
      PersonDto person = parser.parsePersonDto(json_person_dto)

      assert person
      assert person.roles.size() == 1
      assert person.roles[0].name.value == 'Patient'
   }

   // parses using the generic actor parser
   void test_generic_person_api_valid()
   {
      def json_person_dto = $/
         {
            "_type": "PERSON",
            "name": {
               "_type": "DV_TEXT",
               "value": "generic person"
            },
            "archetype_node_id": "openEHR-DEMOGRAPHIC-PERSON.generic.v1",
            "archetype_details": {
               "archetype_id": {
                  "value": "openEHR-DEMOGRAPHIC-PERSON.generic.v1"
               },
               "template_id": {
                  "value": "generic_person"
               },
               "rm_version": "1.0.2"
            },
            "roles": [
               {
                  "name": {
                     "_type": "DV_TEXT",
                     "value": "Patient"
                  },
                  "archetype_details": {
                     "archetype_id": {
                        "_type": "ARCHETYPE_ID",
                        "value": "openEHR-DEMOGRAPHIC-ROLE.generic_role.v1"
                     },
                     "template_id": {
                        "_type": "TEMPLATE_ID",
                        "value": "generic_role"
                     },
                     "rm_version": "1.0.2"
                  },
                  "archetype_node_id": "openEHR-DEMOGRAPHIC-ROLE.generic.v1",
                  "details": {
                     "_type": "ITEM_TREE",
                     "name": {
                        "_type": "DV_TEXT",
                        "value": "tree"
                     },
                     "archetype_node_id": "at0001",
                     "items": [
                        {
                           "_type": "ELEMENT",
                           "name": {
                              "_type": "DV_TEXT",
                              "value": "name"
                           },
                           "archetype_node_id": "at0002",
                           "value": {
                              "_type": "DV_IDENTIFIER",
                              "issuer": "issuerA",
                              "assigner": "Hospital de Clinicas",
                              "id": "assignerC",
                              "type": "typeB"
                           }
                        }
                     ]
                  },
                  "identities": [
                     {
                        "_type": "PARTY_IDENTITY",
                        "name": {
                           "_type": "DV_TEXT",
                           "value": "identity"
                        },
                        "archetype_node_id": "at0004",
                        "details": {
                           "_type": "ITEM_TREE",
                           "name": {
                              "_type": "DV_TEXT",
                              "value": "tree"
                           },
                           "archetype_node_id": "at0005",
                           "items": [
                              {
                                 "_type": "ELEMENT",
                                 "name": {
                                    "_type": "DV_TEXT",
                                    "value": "name"
                                 },
                                 "archetype_node_id": "at0006",
                                 "value": {
                                    "_type": "DV_TEXT",
                                    "value": "patient"
                                 }
                              }
                           ]
                        }
                     }
                  ]
               }
            ],
            "identities": [
               {
                  "_type": "PARTY_IDENTITY",
                  "name": {
                     "_type": "DV_TEXT",
                     "value": "identity"
                  },
                  "archetype_node_id": "at0004",
                  "details": {
                     "_type": "ITEM_TREE",
                     "name": {
                        "_type": "DV_TEXT",
                        "value": "tree"
                     },
                     "archetype_node_id": "at0005",
                     "items": [
                        {
                           "_type": "ELEMENT",
                           "name": {
                              "_type": "DV_TEXT",
                              "value": "name"
                           },
                           "archetype_node_id": "at0006",
                           "value": {
                              "_type": "DV_TEXT",
                              "value": "PiV.POWOXMcONGEMaWuJOkdsW.WLvJOia hLPIyhIIk DA mQAtKBfxJUFWrjVIYtNuJnneQCsxEgCXzQfI KjvGHOfPWaZvvtaCdIQOPtOYKEmES BgWSbYfBFQazoHXgujVFcd.GcirgvNtUdlKThXI VIjlBzxYbtJY.Nfg,DqVGRmfJbLeOiJAyBuDHV.tPFK,XbsAesIqRUTnulF XoASDnVwMMptttPjGXxmrNPvbAHSwqwyyArtrfUIQXXvdduhACRVhjD,aRBidoM,SzNmphdpwyIDdpUt,dDRMR"
                           }
                        }
                     ]
                  }
               }
            ]
         }
      /$

      def parser = new OpenEhrJsonParserQuick(true) // does schema validation
      parser.setSchemaFlavorAPI()
      PersonDto person = parser.parseActorDto(json_person_dto)

      assert person
      assert person.roles.size() == 1
      assert person.roles[0].name.value == 'Patient'

      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR (EHR_STATUS only)
      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(person, 'com.cabolabs.openehr_opt.namespaces.default')

      assert !report.errors

      def serializer = new OpenEhrJsonSerializer()
      println serializer.serialize(person)
   }

   void test_generic_group_api_valid()
   {
      def json_dto = $/
         {
            "_type": "GROUP",
            "name": {
               "_type": "DV_TEXT",
               "value": "generic group"
            },
            "archetype_details": {
               "archetype_id": {
                  "_type": "ARCHETYPE_ID",
                  "value": "openEHR-DEMOGRAPHIC-GROUP.generic_group.v1"
               },
               "template_id": {
                  "_type": "TEMPLATE_ID",
                  "value": "generic_group"
               },
               "rm_version": "1.0.2"
            },
            "archetype_node_id": "openEHR-DEMOGRAPHIC-GROUP.generic_group.v1",
            "details": {
               "_type": "ITEM_TREE",
               "name": {
                  "_type": "DV_TEXT",
                  "value": "tree"
               },
               "archetype_node_id": "at0001",
               "items": [
                  {
                     "_type": "ELEMENT",
                     "name": {
                        "_type": "DV_TEXT",
                        "value": "identifier"
                     },
                     "archetype_node_id": "at0002",
                     "value": {
                        "_type": "DV_IDENTIFIER",
                        "issuer": "Hospital de Clinicas",
                        "assigner": "Hospital de Clinicas",
                        "id": "12345",
                        "type": "NHID"
                     }
                  }
               ]
            },
            "identities": [
               {
                  "_type": "PARTY_IDENTITY",
                  "name": {
                     "_type": "DV_TEXT",
                     "value": "identity"
                  },
                  "archetype_node_id": "at0004",
                  "details": {
                     "_type": "ITEM_TREE",
                     "name": {
                        "_type": "DV_TEXT",
                        "value": "tree"
                     },
                     "archetype_node_id": "at0005",
                     "items": [
                        {
                              "_type": "ELEMENT",
                              "name": {
                                 "_type": "DV_TEXT",
                                 "value": "name"
                              },
                              "archetype_node_id": "at0006",
                              "value": {
                                 "_type": "DV_TEXT",
                                 "value": "YdD,l dALfXOGOXCntaZjAYoRNLzOTxAkDKmLAd.hDsqqHZZ.QEqzNTnpCYvvKtkpN.LgmmksJGTEeyyKLVwKoi,QEMwpuvzGsbNdwSworMqjPxaSbSRnEhdEUzllqYQlQuVsYRUZzSxGJqhUnwGs.XmYMRdSApBBcP,mMHNNewtMlveJFFiVrMB gpgpImNY,KqiFyuiKbNSpmlddrx.oEqLx.yYFoXEZiuXdGKJhtrYmcB.LKMkzqxNOazQCP dFBJhXpnl,KTmiRykpt,pucglAmVLn hsCwPhNFBwMtR"
                              }
                        }
                     ]
                  }
               }
            ],
            "roles": [
               {
                  "name": {
                     "_type": "DV_TEXT",
                     "value": "Dr. James Kernel's Surgical Team"
                  },
                  "archetype_details": {
                     "archetype_id": {
                        "_type": "ARCHETYPE_ID",
                        "value": "openEHR-DEMOGRAPHIC-ROLE.generic_role.v1"
                     },
                     "template_id": {
                        "_type": "TEMPLATE_ID",
                        "value": "generic_role"
                     },
                     "rm_version": "1.0.2"
                  },
                  "archetype_node_id": "openEHR-DEMOGRAPHIC-ROLE.generic.v1",
                  "identities": [
                     {
                        "_type": "PARTY_IDENTITY",
                        "name": {
                           "_type": "DV_TEXT",
                           "value": "identity"
                        },
                        "archetype_node_id": "at0004",
                        "details": {
                           "_type": "ITEM_TREE",
                           "name": {
                              "_type": "DV_TEXT",
                              "value": "tree"
                           },
                           "archetype_node_id": "at0005",
                           "items": [
                              {
                                 "_type": "ELEMENT",
                                 "name": {
                                    "_type": "DV_TEXT",
                                    "value": "name"
                                 },
                                 "archetype_node_id": "at0006",
                                 "value": {
                                    "_type": "DV_TEXT",
                                    "value": "patient"
                                 }
                              }
                           ]
                        }
                     }
                  ]
               }
            ]
         }
      /$

      def parser = new OpenEhrJsonParserQuick(true) // does RM schema validation
      GroupDto group = parser.parseActorDto(json_dto)

      assert group
      assert group.roles.size() == 1
      assert group.roles[0].name.value == "Dr. James Kernel's Surgical Team"
   }


   void test_generic_relationship_api_valid()
   {
      def json = $/
         {
            "_type": "PARTY_RELATIONSHIP",
            "name": {
               "_type": "DV_TEXT",
               "value": "generic relationship"
            },
            "archetype_details": {
               "archetype_id": {
                  "_type": "ARCHETYPE_ID",
                  "value": "openEHR-DEMOGRAPHIC-PARTY_RELATIONSHIP.generic_relationship.v1"
               },
               "template_id": {
                  "_type": "TEMPLATE_ID",
                  "value": "generic_relationship"
               },
               "rm_version": "1.0.2"
            },
            "archetype_node_id": "openEHR-DEMOGRAPHIC-PARTY_RELATIONSHIP.generic_relationship.v1",
            "details": {
               "_type": "ITEM_TREE",
               "name": {
                  "_type": "DV_TEXT",
                  "value": "tree"
               },
               "archetype_node_id": "at0001",
               "items": [
                  {
                  "_type": "ELEMENT",
                  "name": {
                     "_type": "DV_TEXT",
                     "value": "relationship type"
                  },
                  "archetype_node_id": "at0002",
                  "value": {
                     "_type": "DV_CODED_TEXT",
                     "value": "Natural child",
                     "defining_code": {
                        "terminology_id": {
                        "value": "SNOMED-CT"
                        },
                        "code_string": "75226009"
                     }
                  }
                  }
               ]
            },
            "source": {
               "id": {
                  "_type": "OBJECT_VERSION_ID",
                  "value": "cf9c8328-8e43-407a-bcb1-90bd88fd52f3::ATOMIK_CDR::1"
               },
               "namespace": "demographic",
               "type": "PERSON"
            },
            "target": {
               "id": {
                  "_type": "OBJECT_VERSION_ID",
                  "value": "a7118145-43e3-4810-bdab-3753d9714aa3::ATOMIK_CDR::1"
               },
               "namespace": "demographic",
               "type": "PERSON"
            }
            }
      /$

      def parser = new OpenEhrJsonParserQuick(true) // does schema validation
      parser.setSchemaFlavorAPI()
      PartyRelationship relationship = parser.parseJson(json)

      assert relationship
      assert relationship.source
      assert relationship.source.id.value == "cf9c8328-8e43-407a-bcb1-90bd88fd52f3::ATOMIK_CDR::1"
      assert relationship.target
      assert relationship.target.id.value == "a7118145-43e3-4810-bdab-3753d9714aa3::ATOMIK_CDR::1"

      // SETUP OPT REPO
      OptRepository repo = new OptRepositoryFSImpl(getClass().getResource("/opts").toURI())
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(repo)


      // SETUP RM VALIDATOR (EHR_STATUS only)
      RmValidator2 validator = new RmValidator2(opt_manager)
      RmValidationReport report = validator.dovalidate(relationship, 'com.cabolabs.openehr_opt.namespaces.default')

      assert !report.errors

      def serializer = new OpenEhrJsonSerializer()
      println serializer.serialize(relationship)
   }


   void test_generic_organization_api_valid()
   {
      def json_dto = $/
         {
            "_type": "ORGANISATION",
            "name": {
               "_type": "DV_TEXT",
               "value": "generic organization"
            },
            "archetype_details": {
               "archetype_id": {
                     "_type": "ARCHETYPE_ID",
                     "value": "openEHR-DEMOGRAPHIC-ORGANISATION.generic_organization.v1"
               },
               "template_id": {
                     "_type": "TEMPLATE_ID",
                     "value": "generic_organization"
               },
               "rm_version": "1.0.2"
            },
            "archetype_node_id": "openEHR-DEMOGRAPHIC-ORGANISATION.generic_organization.v1",
            "details": {
               "_type": "ITEM_TREE",
               "name": {
                     "_type": "DV_TEXT",
                     "value": "tree"
               },
               "archetype_node_id": "at0001",
               "items": [
                     {
                        "_type": "ELEMENT",
                        "name": {
                           "_type": "DV_TEXT",
                           "value": "identifier"
                        },
                        "archetype_node_id": "at0002",
                        "value": {
                           "_type": "DV_IDENTIFIER",
                           "issuer": "",
                           "assigner": "Hospital de Clinicas",
                           "id": "",
                           "type": ""
                        }
                     }
               ]
            },
            "identities": [
               {
                     "_type": "PARTY_IDENTITY",
                     "name": {
                        "_type": "DV_TEXT",
                        "value": "identity"
                     },
                     "archetype_node_id": "at0004",
                     "details": {
                        "_type": "ITEM_TREE",
                        "name": {
                           "_type": "DV_TEXT",
                           "value": "tree"
                        },
                        "archetype_node_id": "at0005",
                        "items": [
                           {
                                 "_type": "ELEMENT",
                                 "name": {
                                    "_type": "DV_TEXT",
                                    "value": "name"
                                 },
                                 "archetype_node_id": "at0006",
                                 "value": {
                                    "_type": "DV_TEXT",
                                    "value": ".brRUdYXmPKNaDjC.DRJrA,gZbsQswslWX.UVJURhS.haBCyTbYPzNoatIcQUgC HjBOpfWUe OFpzHkNiTpQYNwEkatQMKWhYWXGoHkHwFRsMzTGwiXzkQJteFOCPqJ.CyNEJFHsLsAwhTQySS aYWlKzNn,rEMAGqKHPjqjWtX.rNeChEWtTRxSlD,llO.,ibAvDmPaQXZzVYZeWduufoVRMhZNkLZFRzhrdNyKS,WlqOZlXPKxOvunozcZz,YhoK,,UrQDiLZQevXVTgwFPP.F JNTtCZcgUnrAFhoEeS"
                                 }
                           }
                        ]
                     }
               }
            ],
            "roles": [
               {
                  "name": {
                     "_type": "DV_TEXT",
                     "value": "People's Hospital"
                  },
                  "archetype_details": {
                     "archetype_id": {
                        "_type": "ARCHETYPE_ID",
                        "value": "openEHR-DEMOGRAPHIC-ROLE.generic_role.v1"
                     },
                     "template_id": {
                        "_type": "TEMPLATE_ID",
                        "value": "generic_role"
                     },
                     "rm_version": "1.0.2"
                  },
                  "archetype_node_id": "openEHR-DEMOGRAPHIC-ROLE.generic.v1",
                  "identities": [
                     {
                        "_type": "PARTY_IDENTITY",
                        "name": {
                           "_type": "DV_TEXT",
                           "value": "identity"
                        },
                        "archetype_node_id": "at0004",
                        "details": {
                           "_type": "ITEM_TREE",
                           "name": {
                              "_type": "DV_TEXT",
                              "value": "tree"
                           },
                           "archetype_node_id": "at0005",
                           "items": [
                              {
                                 "_type": "ELEMENT",
                                 "name": {
                                    "_type": "DV_TEXT",
                                    "value": "name"
                                 },
                                 "archetype_node_id": "at0006",
                                 "value": {
                                    "_type": "DV_TEXT",
                                    "value": "organization_provider"
                                 }
                              }
                           ]
                        }
                     }
                  ]
               }
            ]
         }
      /$

      def parser = new OpenEhrJsonParserQuick(true) // does RM schema validation
      OrganizationDto org = parser.parseActorDto(json_dto)

      assert org
      assert org.roles.size() == 1
      assert org.roles[0].name.value == "People's Hospital"
   }


   void test_generic_agent_api_valid()
   {
      def json_dto = $/
         {
            "_type": "AGENT",
            "uid": {
               "_type": "OBJECT_VERSION_ID",
               "value": "54475504-beac-45cc-b866-314510e20520::com.cabolabs.demographic::1"
            },
            "name": {
               "_type": "DV_TEXT",
               "value": "generic agent"
            },
            "archetype_details": {
               "archetype_id": {
                     "_type": "ARCHETYPE_ID",
                     "value": "openEHR-DEMOGRAPHIC-AGENT.generic_agent.v1"
               },
               "template_id": {
                     "_type": "TEMPLATE_ID",
                     "value": "generic_agent"
               },
               "rm_version": "1.0.2"
            },
            "archetype_node_id": "openEHR-DEMOGRAPHIC-AGENT.generic_agent.v1",
            "details": {
               "_type": "ITEM_TREE",
               "name": {
                     "_type": "DV_TEXT",
                     "value": "tree"
               },
               "archetype_node_id": "at0001",
               "items": [
                     {
                        "_type": "ELEMENT",
                        "name": {
                           "_type": "DV_TEXT",
                           "value": "identifier"
                        },
                        "archetype_node_id": "at0002",
                        "value": {
                           "_type": "DV_IDENTIFIER",
                           "issuer": "",
                           "assigner": "Hospital de Clinicas",
                           "id": "",
                           "type": ""
                        }
                     }
               ]
            },
            "identities": [
               {
                     "_type": "PARTY_IDENTITY",
                     "name": {
                        "_type": "DV_TEXT",
                        "value": "identity"
                     },
                     "archetype_node_id": "at0004",
                     "details": {
                        "_type": "ITEM_TREE",
                        "name": {
                           "_type": "DV_TEXT",
                           "value": "tree"
                        },
                        "archetype_node_id": "at0005",
                        "items": [
                           {
                                 "_type": "ELEMENT",
                                 "name": {
                                    "_type": "DV_TEXT",
                                    "value": "name"
                                 },
                                 "archetype_node_id": "at0006",
                                 "value": {
                                    "_type": "DV_TEXT",
                                    "value": ".jkmvPRollKXTqonAzFPmhBnNhy.xqV,kZUKIkBxswTYHThtaH.A.jzbemVK NqpRBvJsdfuoIbXTfBzlqwqnL,bLJufaoFEpENcQv kMeIMPsqioC.SBNFqU,jNCeu,sJTOPSvUiXXnYGrjKgkqYufwcSXklAb,.sblKKznECTVwTbiocTuVNFCzRVYeiIVmNxIxsCQlgYiChNBsdrTggHsHwYlvlwlmjyUNbtLTTrbnYbNRYuCbzQGZ g.MQmMiSbWR pbmLbefuWKnZPCRmSQijZxIHiituSrQUAZPWHg"
                                 }
                           }
                        ]
                     }
               }
            ],
            "roles": [
               {
                  "name": {
                     "_type": "DV_TEXT",
                     "value": "Agent Role"
                  },
                  "archetype_details": {
                     "archetype_id": {
                        "_type": "ARCHETYPE_ID",
                        "value": "openEHR-DEMOGRAPHIC-ROLE.generic_role.v1"
                     },
                     "template_id": {
                        "_type": "TEMPLATE_ID",
                        "value": "generic_role"
                     },
                     "rm_version": "1.0.2"
                  },
                  "archetype_node_id": "openEHR-DEMOGRAPHIC-ROLE.generic.v1",
                  "identities": [
                     {
                        "_type": "PARTY_IDENTITY",
                        "name": {
                           "_type": "DV_TEXT",
                           "value": "identity"
                        },
                        "archetype_node_id": "at0004",
                        "details": {
                           "_type": "ITEM_TREE",
                           "name": {
                              "_type": "DV_TEXT",
                              "value": "tree"
                           },
                           "archetype_node_id": "at0005",
                           "items": [
                              {
                                 "_type": "ELEMENT",
                                 "name": {
                                    "_type": "DV_TEXT",
                                    "value": "name"
                                 },
                                 "archetype_node_id": "at0006",
                                 "value": {
                                    "_type": "DV_TEXT",
                                    "value": "agent_x"
                                 }
                              }
                           ]
                        }
                     }
                  ]
               }
            ]
         }
      /$

      def parser = new OpenEhrJsonParserQuick(true) // does RM schema validation
      AgentDto agent = parser.parseActorDto(json_dto)

      assert agent
      assert agent.roles.size() == 1
      assert agent.roles[0].name.value == "Agent Role"
   }
}