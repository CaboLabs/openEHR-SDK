package com.cabolabs.openehr.opt

import com.cabolabs.openehr.formats.OpenEhrJsonParser
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

import com.cabolabs.openehr.dto_1_0_2.common.change_control.ContributionDto

import groovy.json.JsonSlurper
import com.cedarsoftware.util.io.JsonWriter

class OpenEhrJsonParserTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")

   void testJsonParserEhr()
   {
      def json_ehr = $/
         {
           "system_id": {
             "value": "d60e2348-b083-48ce-93b9-916cef1d3a5a"
           },
           "ehr_id": {
             "value": "7d44b88c-4199-4bad-97dc-d78268e01398"
           },
           "ehr_status": {
             "id": {
               "_type": "OBJECT_VERSION_ID",
               "value": "8849182c-82ad-4088-a07f-48ead4180515::openEHRSys.example.com::1"
             },
             "namespace": "local",
             "type": "EHR_STATUS"
           },
           "time_created": {
             "value": "2015-01-20T19:30:22.765+01:00"
           }
         }
      /$

      def parser = new OpenEhrJsonParser()
      Ehr ehr = parser.parseEhr(json_ehr)

      assert ehr.system_id.value == "d60e2348-b083-48ce-93b9-916cef1d3a5a"

      assert ehr.ehr_status.id.value == "8849182c-82ad-4088-a07f-48ead4180515::openEHRSys.example.com::1"
      assert ehr.ehr_status.type == "EHR_STATUS"
   }

   void testJsonParserEhrStatus()
   {
       def json_ehr_status = $/
         {
            "_type": "EHR_STATUS",
            "archetype_node_id": "openEHR-EHR-EHR_STATUS.generic.v1",
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

      def parser = new OpenEhrJsonParser()
      EhrStatus status = parser.parseEhrStatus(json_ehr_status)

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
      String json2 = serializer.serializeEhrStatus(status)
      
      


      def map1 = new JsonSlurper().parseText(json_ehr_status)
      def map2 = new JsonSlurper().parseText(json2)

      assert map1 == map2
   }
   
   void testJsonParserInstruction()
   {
      String path = PS +"canonical_json"+ PS +"lab_order.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParser()
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
      def parser = new OpenEhrJsonParser()
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
      def parser = new OpenEhrJsonParser()
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
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)

      assert c.path     == '/'
      assert c.dataPath == '/'

      assert c.context.path     == '/context'
      assert c.context.dataPath == '/context'

      assert c.content[0].path     == '/content'
      assert c.content[0].dataPath == '/content[0]'

      assert c.content[0].protocol.path     == '/content/protocol'
      assert c.content[0].protocol.dataPath == '/content[0]/protocol'

      assert c.content[0].protocol.items[0].path     == '/content/protocol/items'
      assert c.content[0].protocol.items[0].dataPath == '/content[0]/protocol/items[0]'

      // there are more items in the protocol

      assert c.content[0].activities[0].path     == '/content/activities'
      assert c.content[0].activities[0].dataPath == '/content[0]/activities[0]'
      
      assert c.content[0].activities[0].description.path     == '/content/activities/description'
      assert c.content[0].activities[0].description.dataPath == '/content[0]/activities[0]/description'
      
      assert c.content[0].activities[0].description.items[0].path     == '/content/activities/description/items'
      assert c.content[0].activities[0].description.items[0].dataPath == '/content[0]/activities[0]/description/items[0]'

      // there are more items in the description

      //def out = JsonWriter.objectToJson(c.content, [(JsonWriter.PRETTY_PRINT): true])
      //println out
   }
   
   void testJsonParserAdminEntry()
   {
      String path = PS +"canonical_json"+ PS +"admin.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)
      
      // This doesn't handle loops created by the parent references of PATHABLE
      //def out = JsonOutput.toJson(c)
      //out = JsonOutput.prettyPrint(out)
      //println out

      assert c.path     == '/'
      assert c.dataPath == '/'

      assert c.context.path     == '/context'
      assert c.context.dataPath == '/context'

      assert c.content[0].path     == '/content'
      assert c.content[0].dataPath == '/content[0]'

      assert c.content[0].data.path     == '/content/data'
      assert c.content[0].data.dataPath == '/content[0]/data'

      assert c.content[0].data.items[0].path     == '/content/data/items'
      assert c.content[0].data.items[0].dataPath == '/content[0]/data/items[0]'

      //def out = JsonWriter.objectToJson(c, [(JsonWriter.PRETTY_PRINT): true])
      //println out
   }

   void testJsonParserVersion()
   {
      String path = PS +"canonical_json"+ PS +"version_test_all_datatypes_en.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Version v = parser.parseVersionJson(json)
      // TODO: check internals

      assert v != null
   }

   void testJsonParserVersionInsteadOfComposition()
   {
      String path = PS +"canonical_json"+ PS +"version_test_all_datatypes_en.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParser()
      
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
      def parser = new OpenEhrJsonParser()
      
      String message = shouldFail {
         parser.parseVersionJson(json) // this tries to parse a version cant parse the provided composition
      }

      assert message == "Can't parse JSON, check COMPOSITION is a VERSION type. If you tried to parse a LOCATABLE, use the parseJson method"
   }

   void testJsonParserVersionList()
   {
      println "--- testJsonParserVersionList ---"
      String path = PS +"canonical_json"+ PS +"version_list_test_all_datatypes_en.json"
      File file = new File(getClass().getResource(path).toURI())
      String json = file.text
      def parser = new OpenEhrJsonParser()
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
      def parser = new OpenEhrJsonParser()
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
      def parser = new OpenEhrJsonParser()
      ContributionDto contribution = parser.parseContributionDto(json)

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
      def parser = new OpenEhrJsonParser()
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
      def parser = new OpenEhrJsonParser()
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
      def parser = new OpenEhrJsonParser()
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
	  def parser = new OpenEhrJsonParser()
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
	  def parser = new OpenEhrJsonParser()
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
	  def parser = new OpenEhrJsonParser()
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
	  def parser = new OpenEhrJsonParser()
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
	  def parser = new OpenEhrJsonParser()
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
	  def parser = new OpenEhrJsonParser()
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
	  def parser = new OpenEhrJsonParser()
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
     def parser = new OpenEhrJsonParser()
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
     def parser = new OpenEhrJsonParser()
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
     def parser = new OpenEhrJsonParser()
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
     def parser = new OpenEhrJsonParser()
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
   
   void testJsonSchema()
   {
      //def uri = 'https://gist.githubusercontent.com/pieterbos/81651d2d7a5041a130ecb21b0a852e39/raw/2f31b9c7067bccf192256358da868ee8fbc7239a/OpenEHR%2520RM%2520json%2520schema,%2520with%2520default%2520instances%2520of%2520objects%2520addedcorrectly.json'
      //def jsonValidator = new JsonInstanceValidation(uri)
      def jsonValidator = new JsonInstanceValidation()

      ObjectMapper mapper = new ObjectMapper()

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
         'canonical_json/vital_signs_pathfinder_demo.en.v1_instance_3602591.json'
      ]

      InputStream ins
      JsonNode json
      Set<ValidationMessage> errors

      // https://github.com/networknt/json-schema-validator/blob/master/src/test/java/com/networknt/schema/V7JsonSchemaTest.java
      files.each { testCaseFile ->

         //println testCaseFile

         //final URI testCaseFileUri = URI.create("classpath:" + testCaseFile)
         ins = Thread.currentThread().getContextClassLoader().getResourceAsStream(testCaseFile)

         if (!ins) throw new Exception("Test file $testCaseFile not found")

         // https://fasterxml.github.io/jackson-databind/javadoc/2.7/com/fasterxml/jackson/databind/ObjectMapper.html#readTree(java.io.InputStream)
         json = mapper.readTree(ins)
         errors = jsonValidator.validate(json)

         // TODO: use asserts
         def out = JsonOutput.toJson(errors)
         out = JsonOutput.prettyPrint(out)
         println out
      }

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
      def parser = new OpenEhrJsonParser()
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
}
