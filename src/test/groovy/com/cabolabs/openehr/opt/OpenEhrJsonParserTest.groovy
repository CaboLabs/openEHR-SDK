package com.cabolabs.openehr.opt

import com.cabolabs.openehr.formats.OpenEhrJsonParser
import com.cabolabs.openehr.formats.OpenEhrJsonParserQuick
import com.cabolabs.openehr.formats.OpenEhrXmlSerializer
import com.cabolabs.openehr.formats.OpenEhrJsonSerializer

import com.cabolabs.openehr.opt.instance_validation.XmlValidation
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Archetyped
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.rm_1_0_2.composition.EventContext
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.AdminEntry
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemTree
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_types.text.CodePhrase
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText
import com.cabolabs.openehr.rm_1_0_2.ehr.Ehr
import com.cabolabs.openehr.rm_1_0_2.support.identification.ArchetypeId
import com.cabolabs.openehr.rm_1_0_2.support.identification.HierObjectId
import com.cabolabs.openehr.rm_1_0_2.support.identification.TemplateId
import com.cabolabs.openehr.rm_1_0_2.support.identification.TerminologyId
import groovy.util.GroovyTestCase
import groovy.json.JsonOutput
import com.cabolabs.openehr.rm_1_0_2.common.change_control.OriginalVersion
import com.cabolabs.openehr.rm_1_0_2.common.change_control.Contribution
import com.cabolabs.openehr.rm_1_0_2.common.change_control.Version

import com.networknt.schema.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.cabolabs.openehr.opt.instance_validation.JsonInstanceValidation
import com.cabolabs.openehr.rm_1_0_2.ehr.EhrStatus
import com.cabolabs.openehr.rm_1_0_2.common.directory.Folder

import com.cabolabs.openehr.dto_1_0_2.common.change_control.ContributionDto

import groovy.json.JsonSlurper
import com.cedarsoftware.util.io.JsonWriter

import groovy.time.TimeCategory
import groovy.time.TimeDuration

class OpenEhrJsonParserTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")

   void testParseAndValidateRole()
   {
      String path = "${PS}canonical_json${PS}demographic${PS}paciente_cua_instance.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text

      def parser = new OpenEhrJsonParserQuick(true)
      parser.setSchemaFlavorAPI()
      def role = parser.parseJson(json)
      assert parser.getJsonValidationErrors() == null
   }


   // This test is just to compare execution time for json parser calculating paths
   // vs. que quick one that doesn't calculate the paths.
   void testJsonParserNormalVsQuick()
   {
      String path = PS +"canonical_json"+ PS +"lab_results.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text


      def parser = new OpenEhrJsonParser()
      def parserq = new OpenEhrJsonParserQuick()


      Date start = new Date()
      (1..10).each {

         parser.parseJson(json)
      }
      Date end = new Date()

      TimeDuration td = TimeCategory.minus(end, start)
      println td


      start = new Date()
      (1..10).each {

         parserq.parseJson(json)
      }
      end = new Date()

      td = TimeCategory.minus(end, start)
      println td



      path = PS +"canonical_json"+ PS +"subfolders_in_directory_with_details_items.json"
      file = new File(getClass().getResource(path).toURI())
      json = file.text


      start = new Date()
      (1..10).each {

         parser.parseJson(json)
      }
      end = new Date()

      td = TimeCategory.minus(end, start)
      println td


      start = new Date()
      (1..10).each {

         parserq.parseJson(json)
      }
      end = new Date()

      td = TimeCategory.minus(end, start)
      println td

   }


   void testJsonParserFolder()
   {
      String path = PS +"canonical_json"+ PS +"subfolders_in_directory_with_details_items.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick(true) // true validates against JSON Schema
      Folder f = (Folder)parser.parseJson(json)

      println parser.getJsonValidationErrors()

      assert f

      assert f.items.size() == 1
      assert f.folders.size() == 2

      assert f.name.value == 'root'
      assert f.folders[0].name.value == 'subfolder 1'
      assert f.folders[1].name.value == 'subfolder 2'

      //def out = JsonWriter.objectToJson(f, [(JsonWriter.PRETTY_PRINT): true])
      //println out
   }

   // this test uses parser and serializer
   void testJsonParserFolder2()
   {
       def json = $/
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
                     "value": "subfolder 1"
                  },
                  "archetype_node_id": "openEHR-EHR-FOLDER.generic.v1"
               },
               {
                  "_type": "FOLDER",
                  "name": {
                     "_type": "DV_TEXT",
                     "value": "subfolder 2"
                  },
                  "archetype_node_id": "openEHR-EHR-FOLDER.generic.v1"
               }
            ],
            "items": [
               {
                  "id": {
                     "_type": "HIER_OBJECT_ID",
                     "value": "d936409e-901f-4994-8d33-ed104d46015b"
                  },
                  "namespace": "EHR",
                  "type": "VERSIONED_COMPOSITION"
               }
            ]
         }
      /$

      def parser = new OpenEhrJsonParserQuick()
      Folder folder = parser.parseJson(json)


      // serialize status object
      def serializer = new OpenEhrJsonSerializer()
      String json2 = serializer.serialize(folder)

      println json2

      //def map1 = new JsonSlurper().parseText(json_ehr_status)
      //def map2 = new JsonSlurper().parseText(json2)
      //assert map1 == map2
   }


   void testJsonParserEhr()
   {
      def json_ehr = $/
         {
           "_type": "EHR",
           "system_id": {
             "value": "d60e2348-b083-48ce-93b9-916cef1d3a5a"
           },
           "ehr_id": {
             "value": "7d44b88c-4199-4bad-97dc-d78268e01398"
           },
           "ehr_access": {
             "id": {
               "_type": "OBJECT_VERSION_ID",
               "value": "8849182c-82ad-4088-a07f-48ead4180999::openEHRSys.example.com::1"
             },
             "namespace": "local",
             "type": "EHR_ACCESS"
           },
           "ehr_status": {
             "_type": "LOCATABLE_REF",
             "id": {
               "_type": "OBJECT_VERSION_ID",
               "value": "8849182c-82ad-4088-a07f-48ead4180515::openEHRSys.example.com::1"
             },
             "namespace": "local",
             "type": "EHR_STATUS"
           },
           "time_created": {
             "value": "2015-01-20T19:30:22.765+01:00"
           },
           "contributions": []
         }
      /$

      def parser = new OpenEhrJsonParserQuick(true)
      Ehr ehr = parser.parseEhr(json_ehr)

      assert ehr

      assert ehr.system_id.value == "d60e2348-b083-48ce-93b9-916cef1d3a5a"

      assert ehr.ehr_status.id.value == "8849182c-82ad-4088-a07f-48ead4180515::openEHRSys.example.com::1"
      assert ehr.ehr_status.type == "EHR_STATUS"
   }

   void testJsonParserEhrWithSchemaError()
   {
      def json_ehr = $/
         {
           "_type": "EHR",
           "system_id": {
             "value": "d60e2348-b083-48ce-93b9-916cef1d3a5a"
           },
           "ehr_id": {
             "value": "7d44b88c-4199-4bad-97dc-d78268e01398"
           },
           "time_created": {
             "value": "2015-01-20T19:30:22.765+01:00"
           }
         }
      /$

      def parser = new OpenEhrJsonParserQuick(true)

      Ehr ehr = parser.parseEhr(json_ehr)

      assert !ehr

      // Set<ValitaionMessage>
      // https://javadoc.io/doc/com.networknt/json-schema-validator/1.0.51/com/networknt/schema/ValidationMessage.html
      def errors = parser.getJsonValidationErrors()

      def err_ehr_access = errors.find{ it.message == "\$.ehr_access: is missing but it is required" }

      assert err_ehr_access
      assert err_ehr_access.type == "required"


      def err_ehr_status = errors.find{ it.message == "\$.ehr_status: is missing but it is required" }

      assert err_ehr_status
      assert err_ehr_status.type == "required"
   }


   void testJsonParserEhrStatus()
   {
       def json_ehr_status = $/
         {
            "_type": "EHR_STATUS",
            "name": {
               "_type": "DV_TEXT",
               "value": "EHR Status"
            },
            "archetype_details": {
               "archetype_id": {
                  "value": "openEHR-EHR-EHR_STATUS.generic.v1"
               },
               "template_id": {
                  "value": "generic.en.v1"
               },
               "rm_version": "1.0.2"
            },
            "archetype_node_id": "openEHR-EHR-EHR_STATUS.generic.v1",
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

      def parser = new OpenEhrJsonParserQuick(true)
      EhrStatus status = parser.parseJson(json_ehr_status)

      assert status.archetype_node_id == "openEHR-EHR-EHR_STATUS.generic.v1"
      assert status.name.value == "EHR Status"
      assert status.subject != null
      assert status.subject.external_ref.id.value == "ins01"
      assert status.subject.external_ref.namespace == "DEMOGRAPHIC"
      assert status.other_details == null
      assert status.is_modifiable == true
      assert status.is_queryable == true

      // serialize status object
      def serializer = new OpenEhrJsonSerializer()
      String json2 = serializer.serialize(status)


      def map1 = new JsonSlurper().parseText(json_ehr_status)
      def map2 = new JsonSlurper().parseText(json2)

      assert map1 == map2
   }

   void testJsonParserEhrStatusWithSchemaError()
   {
       def json_ehr_status = $/
         {
            "_type": "EHR_STATUS",
            "archetype_node_id": "openEHR-EHR-EHR_STATUS.generic.v1",
            "archetype_details": {
               "archetype_id": {
                  "value": "openEHR-EHR-EHR_STATUS.generic.v1"
               },
               "template_id": {
                  "value": "ehr_status.en.v1"
               },
               "rm_version": "1.0.2"
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
            }
         }
      /$

      def parser = new OpenEhrJsonParserQuick(true)
      EhrStatus status = parser.parseJson(json_ehr_status)

      assert !status

      // Set<ValitaionMessage>
      // https://javadoc.io/doc/com.networknt/json-schema-validator/1.0.51/com/networknt/schema/ValidationMessage.html
      def errors = parser.getJsonValidationErrors()

      def err_name = errors.find{ it.message == "\$.name: is missing but it is required" }

      assert err_name
      assert err_name.type == "required"

      // TODO
      //[$.name: is missing but it is required, $.is_queryable: is missing but it is required, $.is_modifiable: is missing but it is required]
   }

   void testJsonParserInstruction()
   {
      String path = PS +"canonical_json"+ PS +"lab_order.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick()
      Composition c = (Composition)parser.parseJson(json)


      // TODO assert paths

      def out = JsonWriter.objectToJson(c.content, [(JsonWriter.PRETTY_PRINT): true])
      println out
   }

   void testJsonParserObservation()
   {
      String path = PS +"canonical_json"+ PS +"lab_results.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick()
      Composition c = (Composition)parser.parseJson(json)

      // TODO assert paths

      def out = JsonWriter.objectToJson(c.content, [(JsonWriter.PRETTY_PRINT): true])
      println out
   }

   void testJsonParserNoNameType()
   {
      println "== testJsonParserNoNameType =="
      String path = PS +"canonical_json"+ PS +"composition_missing_name_type.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick()
      Composition c = (Composition)parser.parseJson(json)

      assert c != null
      // TODO assert paths

      def out = JsonWriter.objectToJson(c.content, [(JsonWriter.PRETTY_PRINT): true])
      println out
   }

   void testJsonParserReferralWithParticipations()
   {
      String path = PS +"canonical_json"+ PS +"referral.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick()
      Composition c = (Composition)parser.parseJson(json)

      assert c != null

      /* NOTE: the new parser doesn't calculate the paths
      assert c.path     == '/'
      assert c.dataPath == '/'

      assert c.context.path     == '/context'
      assert c.context.dataPath == '/context'

      assert c.content[0].path     == '/content[archetype_id=openEHR-EHR-INSTRUCTION.request-referral.v1]'
      assert c.content[0].dataPath == '/content(0)'

      assert c.content[0].protocol.path     == '/content[archetype_id=openEHR-EHR-INSTRUCTION.request-referral.v1]/protocol[at0008]'
      assert c.content[0].protocol.dataPath == '/content(0)/protocol'

      assert c.content[0].protocol.items[0].path     == '/content[archetype_id=openEHR-EHR-INSTRUCTION.request-referral.v1]/protocol[at0008]/items[at0010]'
      assert c.content[0].protocol.items[0].dataPath == '/content(0)/protocol/items(0)'

      // there are more items in the protocol

      assert c.content[0].activities[0].path     == '/content[archetype_id=openEHR-EHR-INSTRUCTION.request-referral.v1]/activities[at0001]'
      assert c.content[0].activities[0].dataPath == '/content(0)/activities(0)'

      assert c.content[0].activities[0].description.path     == '/content[archetype_id=openEHR-EHR-INSTRUCTION.request-referral.v1]/activities[at0001]/description[at0009]'
      assert c.content[0].activities[0].description.dataPath == '/content(0)/activities(0)/description'

      assert c.content[0].activities[0].description.items[0].path     == '/content[archetype_id=openEHR-EHR-INSTRUCTION.request-referral.v1]/activities[at0001]/description[at0009]/items[at0121]'
      assert c.content[0].activities[0].description.items[0].dataPath == '/content(0)/activities(0)/description/items(0)'
      */

      // there are more items in the description

      //def out = JsonWriter.objectToJson(c.content, [(JsonWriter.PRETTY_PRINT): true])
      //println out
   }

   void testJsonParserAdminEntry()
   {
      String path = PS +"canonical_json"+ PS +"admin.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick()
      Composition c = (Composition)parser.parseJson(json)

      assert c != null

      // This doesn't handle loops created by the parent references of PATHABLE
      //def out = JsonOutput.toJson(c)
      //out = JsonOutput.prettyPrint(out)
      //println out

      /* NOTE: the new parser doesn't calculate the paths
      assert c.path     == '/'
      assert c.dataPath == '/'

      assert c.context.path     == '/context'
      assert c.context.dataPath == '/context'

      assert c.content[0].path     == '/content[archetype_id=openEHR-EHR-ADMIN_ENTRY.minimal.v1]'
      assert c.content[0].dataPath == '/content(0)'

      assert c.content[0].data.path     == '/content[archetype_id=openEHR-EHR-ADMIN_ENTRY.minimal.v1]/data[at0001]'
      assert c.content[0].data.dataPath == '/content(0)/data'

      assert c.content[0].data.items[0].path     == '/content[archetype_id=openEHR-EHR-ADMIN_ENTRY.minimal.v1]/data[at0001]/items[at0002]'
      assert c.content[0].data.items[0].dataPath == '/content(0)/data/items(0)'
      */

      //def out = JsonWriter.objectToJson(c, [(JsonWriter.PRETTY_PRINT): true])
      //println out
   }

   void testJsonParserVersion()
   {
      String path = PS +"canonical_json"+ PS +"version_test_all_datatypes_en.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick()
      Version v = parser.parseVersionJson(json)
      // TODO: check internals

      assert v != null
   }

   void testJsonParserVersionInsteadOfComposition()
   {
      String path = PS +"canonical_json"+ PS +"version_test_all_datatypes_en.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick()

      String message = shouldFail {
         parser.parseJson(json) // this tries to parse a pathable cant parse the provided version
      }

      assert message == "Can't parse JSON, check ORIGINAL_VERSION is a LOCATABLE type. If you tried to parse a VERSION, use the parseVersionJson method"
   }

   void testJsonParserCompositionInsteadOfVersion()
   {
      String path = PS +"canonical_json"+ PS +"admin.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick()

      String message = shouldFail {
         parser.parseVersionJson(json) // this tries to parse a version cant parse the provided composition
      }

      assert message == "Can't parse JSON: type COMPOSITION should be either ORIGINAL_VERSION or IMPORTED_VERSION"
   }

   void testJsonParserVersionList()
   {
      println "--- testJsonParserVersionList ---"
      String path = PS +"canonical_json"+ PS +"version_list_test_all_datatypes_en.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick()
      List<Version> vl = parser.parseVersionList(json)

      // TODO: check internals

      assert vl.size() == 1
      assert vl[0].uid.value == '93bbff8b-cdd5-43a3-8d71-194a735cc704::CABOLABS::1'

      println vl[0].data.context.start_time

      //def out = JsonWriter.objectToJson(vl, [(JsonWriter.PRETTY_PRINT): true])
      //println out
   }

   // this is the contribution from the RM
   void testJsonParserContribution()
   {
      String path = PS +"canonical_json"+ PS +"contribution_test_all_datatypes_en.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick(true)
      Contribution contribution = parser.parseContribution(json)

      // TODO: check internals

      assert contribution.versions.size() == 1
      assert contribution.versions[0].id.value == 'fb458d9c-1323-42bc-b7f8-787f3660a0b5::CABOLABS::1'
   }

   // this is the contribution used in the API
   void testJsonParserContributionDto()
   {
      String path = PS +"canonical_json"+ PS +"contribution_dto_test_all_datatypes_en.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick(true)
      ContributionDto contribution = parser.parseContributionDto(json)

      if (!contribution)
      {
         println parser.getJsonValidationErrors()
      }

      // TODO: check internals

      assert contribution.versions.size() == 1
      assert contribution.versions[0].uid.value == '93bbff8b-cdd5-43a3-8d71-194a735cc704::CABOLABS::1'
   }

   // TODO: move to the XML test suite
   /*
   void testXmlSerializerCompo()
   {
      Composition c = new Composition()

      c.name = new DvText(value: 'clinical document')
      c.archetype_node_id = 'openEHR-EHR-COMPOSITION.doc.v1'
      c.uid = new HierObjectId(value: UUID.randomUUID())

      c.language = new CodePhrase()
      c.language.code_string = '1234'
      c.language.terminology_id = new TerminologyId(value:'LOCAL')

      c.territory = new CodePhrase()
      c.territory.code_string = 'UY'
      c.territory.terminology_id = new TerminologyId(value:'LOCAL')

      c.category = new DvCodedText()
      c.category.value = 'event'
      c.category.defining_code = new CodePhrase()
      c.category.defining_code.code_string = '531'
      c.category.defining_code.terminology_id = new TerminologyId(value:'openehr')

      c.archetype_details = new Archetyped()
      c.archetype_details.archetype_id = new ArchetypeId(value: c.archetype_node_id)
      c.archetype_details.template_id = new TemplateId()
      c.archetype_details.template_id.value = 'doc.en.v1'

      c.context = new EventContext()
      c.context.start_time = new DvDateTime(value: '2021-01-14T10:10:00Z')
      c.context.location = 'location'

      c.context.setting = new DvCodedText()
      c.context.setting.value = 'setting'
      c.context.setting.defining_code = new CodePhrase()
      c.context.setting.defining_code.code_string = '12345'
      c.context.setting.defining_code.terminology_id = new TerminologyId(value:'LOCAL')

      AdminEntry a = new AdminEntry()

      a.language = new CodePhrase()
      a.language.code_string = 'ES'
      a.language.terminology_id = new TerminologyId(value:'ISO_639-1')

      a.encoding = new CodePhrase()
      a.encoding.code_string = 'UTF-8'
      a.encoding.terminology_id = new TerminologyId(value:'IANA_character-sets')

      a.archetype_node_id = 'openEHR-EHR-ADMIN_ENTRY.test.v1'
      a.name = new DvText(value:'admin')
      a.data = new ItemTree(name: new DvText(value:'tree'))

      c.content.add(a)

      OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
      String out = serial.serialize(c)

      //println out
   }
   */


   void testJsonParserAdminEntryToXml()
   {
      // parse JSON
      String path = PS +"canonical_json"+ PS +"admin.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick()
      Composition c = (Composition)parser.parseJson(json)

      // serialize to XML
      OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
      String xml = serial.serialize(c)
      //println xml

      // validate xml
      def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
      def validator = new XmlValidation(inputStream)
      assert validateXMLInstance(validator, xml)
   }


   void testJsonParserInstructionToXml()
   {
      // parse JSON
      String path = PS +"canonical_json"+ PS +"lab_order.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick()
      Composition c = (Composition)parser.parseJson(json)

      // serialize to XML
      OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
      String xml = serial.serialize(c)
      //println xml

      // validate xml
      def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
      def validator = new XmlValidation(inputStream)
      assert validateXMLInstance(validator, xml)
   }


   void testJsonParserObservationToXml()
   {
      // parse JSON
      String path = PS +"canonical_json"+ PS +"lab_results.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParserQuick()
      Composition c = (Composition)parser.parseJson(json)

      // serialize to XML
      OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
      String xml = serial.serialize(c)
      //println xml

      // validate xml
      def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
      def validator = new XmlValidation(inputStream)
      assert validateXMLInstance(validator, xml)
   }

   void testJsonParserReferralWithParticipationsToXml()
   {
     // parse JSON
     String path = PS +"canonical_json"+ PS +"referral.json"
     File file = new File(getClass().getResource(path).toURI())
     String json = file.text
     def parser = new OpenEhrJsonParserQuick()
     Composition c = (Composition)parser.parseJson(json)

     // serialize to XML
     OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
     String xml = serial.serialize(c)
     //println xml

     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }

   void testJsonParserMinimalActionToXml()
   {
     // parse JSON
     String path = PS +"canonical_json"+ PS +"minimal_action.json"
     File file = new File(getClass().getResource(path).toURI())
     String json = file.text
     def parser = new OpenEhrJsonParserQuick()
     Composition c = (Composition)parser.parseJson(json)

     // serialize to XML
     OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
     String xml = serial.serialize(c)
     //println xml

     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }


   void testJsonParserMinimalEvaluationToXml()
   {
     // parse JSON
     String path = PS +"canonical_json"+ PS +"minimal_evaluation.json"
     File file = new File(getClass().getResource(path).toURI())
     String json = file.text
     def parser = new OpenEhrJsonParserQuick()
     Composition c = (Composition)parser.parseJson(json)

     // serialize to XML
     OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
     String xml = serial.serialize(c)
     //println xml

     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }

   void testJsonParserNestedToXml()
   {
     // parse JSON
     String path = PS +"canonical_json"+ PS +"nested.json"
     File file = new File(getClass().getResource(path).toURI())
     String json = file.text
     def parser = new OpenEhrJsonParserQuick()
     Composition c = (Composition)parser.parseJson(json)

     // serialize to XML
     OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
     String xml = serial.serialize(c)
     //println xml

     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }

   void testJsonParserOximetriaToXml()
   {
     // parse JSON
     String path = PS +"canonical_json"+ PS +"oximetria_obs.json"
     File file = new File(getClass().getResource(path).toURI())
     String json = file.text
     def parser = new OpenEhrJsonParserQuick()
     Composition c = (Composition)parser.parseJson(json)

     // serialize to XML
     OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
     String xml = serial.serialize(c)
     //println xml

     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }

   void testJsonParserPhysicalActivityToXml()
   {
     // parse JSON
     String path = PS +"canonical_json"+ PS +"physical_activity.json"
     File file = new File(getClass().getResource(path).toURI())
     String json = file.text
     def parser = new OpenEhrJsonParserQuick()
     Composition c = (Composition)parser.parseJson(json)

     // serialize to XML
     OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
     String xml = serial.serialize(c)
     //println xml

     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }

   void testJsonParserProzedurToXml()
   {
     // parse JSON
     String path = PS +"canonical_json"+ PS +"prozedur.json"
     File file = new File(getClass().getResource(path).toURI())
     String json = file.text
     def parser = new OpenEhrJsonParserQuick()
     Composition c = (Composition)parser.parseJson(json)

     // serialize to XML
     OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
     String xml = serial.serialize(c)
     //println xml

     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }

   void testJsonParserAmdAssessmentToXml()
   {
     // parse JSON
     String path = PS +"canonical_json"+ PS +"amd_assessment.en.v1.json"
     File file = new File(getClass().getResource(path).toURI())
     String json = file.text
     def parser = new OpenEhrJsonParserQuick()
     Composition c = (Composition)parser.parseJson(json)

     // serialize to XML
     OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
     String xml = serial.serialize(c)
     //println xml

     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }

   void testJsonParserDiagnoseToXml()
   {
     // parse JSON
     String path = PS +"canonical_json"+ PS +"diagnose.de.v1.json"
     File file = new File(getClass().getResource(path).toURI())
     String json = file.text
     def parser = new OpenEhrJsonParserQuick()
     Composition c = (Composition)parser.parseJson(json)

     // serialize to XML
     OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
     String xml = serial.serialize(c)
     //println xml

     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }

   void testJsonParserExperimentalRespToXml()
   {
     // parse JSON
     String path = PS +"canonical_json"+ PS +"experimental_respiratory_parameters_document.json"
     File file = new File(getClass().getResource(path).toURI())
     String json = file.text
     def parser = new OpenEhrJsonParserQuick()
     OriginalVersion v = (OriginalVersion)parser.parseVersionJson(json)


     //def out = JsonOutput.toJson(v)
     //out = JsonOutput.prettyPrint(out)
     //println out


     // serialize to XML
     OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
     String xml = serial.serialize(v)
     //println xml

     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }

   void testJsonParserKorptempToXml()
   {
     // parse JSON
     String path = PS +"canonical_json"+ PS +"intensivmedizinisches_monitoring_korpertemperatur.json"
     File file = new File(getClass().getResource(path).toURI())
     String json = file.text
     def parser = new OpenEhrJsonParserQuick()
     Composition c = (Composition)parser.parseJson(json)

     // serialize to XML
     OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
     String xml = serial.serialize(c)
     //println xml

     // validate xml
     def inputStream = this.getClass().getResourceAsStream('/xsd/Version.xsd')
     def validator = new XmlValidation(inputStream)
     assert validateXMLInstance(validator, xml)
   }

   static boolean validateXMLInstance(validator, xml)
   {
      if (!validator.validate(xml))
      {
         println 'NOT VALID'
         println '====================================='
         validator.errors.each {
            println it
         }
         println '====================================='

         return false
      }

      return true
   }



   void testCompositionJsonParseValidationSerializationValidation()
   {
      String path = PS +"canonical_json"+ PS +"admin.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text

      // JSON VALIDATION
      def jsonValidator = new JsonInstanceValidation()
      //ObjectMapper mapper = new ObjectMapper()
      //InputStream ins = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)
      //JsonNode json = mapper.readTree(ins)
      Set<ValidationMessage> errors = jsonValidator.validate(json)

      assert errors.size() == 0

      // JSON PARSE
      def parser = new OpenEhrJsonParserQuick()
      Composition compo = (Composition)parser.parseJson(json)

      // TODO: COMPOSITION VALIDATION AGAINST OPT

      // JSON SERIALIZATION
      def serializer = new OpenEhrJsonSerializer()
      String json2 = serializer.serialize(compo)
      errors = jsonValidator.validate(json2)


      // TODO: use asserts
      // JSON VALIDATION
      def out = JsonOutput.toJson(errors)
      out = JsonOutput.prettyPrint(out)
      //println out

      println json2

      assert errors.size() == 0
   }




   void testJsonParsePerson()
   {
       def json = $/
         {
            "_type": "ORIGINAL_VERSION",
            "uid": {
               "_type": "OBJECT_VERSION_ID",
               "value": "e50e7e48-fb93-45eb-ac7d-892ba4154c5c::ATOMIK::1"
            },
            "contribution": {
               "id": {
                  "_type": "HIER_OBJECT_ID",
                  "value": "683cd722-a886-44eb-ae3e-55eb18a2ed06"
               },
               "namespace": "EHR::COMMON",
               "type": "CONTRIBUTION"
            },
            "commit_audit": {
               "system_id": "ATOMIK",
               "committer": {
                  "_type": "PARTY_IDENTIFIED",
                  "name": "John Doe",
                  "external_ref": {
                     "id": {
                        "_type": "HIER_OBJECT_ID",
                        "value": "BC8132EA-8F4A-11E7-BB31-BE2E44B06B34"
                     },
                     "namespace": "demographic",
                     "type": "PERSON"
                  }
               },
               "time_committed": {
                  "value": "2023-03-27T00:03:30Z"
               },
               "change_type": {
                  "value": "creation",
                  "defining_code": {
                     "terminology_id": {
                        "value": "openehr"
                     },
                     "code_string": "249"
                  }
               }
            },
            "data": {
               "_type": "PERSON",
               "name": {
                  "_type": "DV_TEXT",
                  "value": "generic person"
               },
               "uid": {
                  "_type": "OBJECT_VERSION_ID",
                  "value": "e50e7e48-fb93-45eb-ac7d-892ba4154c5c::ATOMIK::1"
               },
               "archetype_details": {
                  "archetype_id": {
                     "_type": "ARCHETYPE_ID",
                     "value": "openEHR-DEMOGRAPHIC-PERSON.generic_person.v1"
                  },
                  "template_id": {
                     "_type": "TEMPLATE_ID",
                     "value": "generic_person"
                  },
                  "rm_version": "1.0.2"
               },
               "archetype_node_id": "openEHR-DEMOGRAPHIC-PERSON.generic.v1",
               "identities": [{
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
                     "items": [{
                        "_type": "ELEMENT",
                        "name": {
                           "_type": "DV_TEXT",
                           "value": "name"
                        },
                        "archetype_node_id": "at0006",
                        "value": {
                           "_type": "DV_TEXT",
                           "value": "Pablo Pazos"
                        }
                     }]
                  }
               }],
               "roles": [{
                  "_type": "ROLE",
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
                  "identities": [{
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
                        "items": [{
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
                        }]
                     }
                  }]
               }]
            },
            "lifecycle_state": {
               "value": "complete",
               "defining_code": {
                  "terminology_id": {
                     "value": "openehr"
                  },
                  "code_string": "532"
               }
            }
         }
      /$

      def parser = new OpenEhrJsonParserQuick(true)
      parser.setSchemaFlavorAPI()
      Version version = parser.parseVersionJson(json)

      println parser.getJsonValidationErrors()

      assert version

      // // Set<ValitaionMessage>
      // // https://javadoc.io/doc/com.networknt/json-schema-validator/1.0.51/com/networknt/schema/ValidationMessage.html
      // def errors = parser.getJsonValidationErrors()

      // def err_name = errors.find{ it.message == "\$.name: is missing but it is required" }

      // assert err_name
      // assert err_name.type == "required"

      // TODO
      //[$.name: is missing but it is required, $.is_queryable: is missing but it is required, $.is_modifiable: is missing but it is required]
   }
}
