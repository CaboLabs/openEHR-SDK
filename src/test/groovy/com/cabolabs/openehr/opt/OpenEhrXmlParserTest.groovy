package com.cabolabs.openehr.opt

import com.cabolabs.openehr.formats.OpenEhrXmlParser
import com.cabolabs.openehr.formats.OpenEhrXmlSerializer
import com.cabolabs.openehr.opt.instance_validation.XmlValidation
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.rm_1_0_2.composition.EventContext
import com.cabolabs.openehr.rm_1_0_2.composition.content.entry.AdminEntry
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemTree
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_types.text.CodePhrase
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText
import com.cabolabs.openehr.rm_1_0_2.support.identification.ArchetypeId
import com.cabolabs.openehr.rm_1_0_2.support.identification.HierObjectId
import com.cabolabs.openehr.rm_1_0_2.support.identification.TemplateId
import com.cabolabs.openehr.rm_1_0_2.support.identification.TerminologyId

import com.cabolabs.openehr.rm_1_0_2.ehr.Ehr
import com.cabolabs.openehr.rm_1_0_2.ehr.EhrStatus

import groovy.util.GroovyTestCase
import groovy.xml.*
import groovy.json.JsonOutput
import com.cabolabs.openehr.rm_1_0_2.common.change_control.*
import com.cabolabs.openehr.dto_1_0_2.common.change_control.ContributionDto

import com.cabolabs.openehr.rm_1_0_2.common.directory.Folder

class OpenEhrXmlParserTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")

   void testXmlParserFolder()
   {
      def xml = $/<folder xmlns="http://schemas.openehr.org/v1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="FOLDER" archetype_node_id="openEHR-EHR-FOLDER.generic.v1">
         <name xsi:type="DV_TEXT">
            <value>root</value>
         </name>
         <archetype_details>
            <archetype_id>
               <value>openEHR-EHR-FOLDER.generic.v1</value>
            </archetype_id>
            <template_id>
               <value>generic_folder</value>
            </template_id>
            <rm_version>1.0.2</rm_version>
         </archetype_details>
         <folders archetype_node_id="openEHR-EHR-FOLDER.generic.v1">
            <name xsi:type="DV_TEXT">
               <value>subfolder 1</value>
            </name>
            <items>
               <id xsi:type="HIER_OBJECT_ID">
                  <value>d936409e-901f-4994-8d33-ed104d460789</value>
               </id>
               <namespace>EHR</namespace>
               <type>VERSIONED_COMPOSITION</type>
            </items>
         </folders>
         <folders archetype_node_id="openEHR-EHR-FOLDER.generic.v1">
            <name xsi:type="DV_TEXT">
               <value>subfolder 2</value>
            </name>
            <items>
               <id xsi:type="HIER_OBJECT_ID">
                  <value>d936409e-901f-4994-8d33-ed104d460456</value>
               </id>
               <namespace>EHR</namespace>
               <type>VERSIONED_COMPOSITION</type>
            </items>
         </folders>
         <items>
            <id xsi:type="HIER_OBJECT_ID">
               <value>d936409e-901f-4994-8d33-ed104d46015b</value>
            </id>
            <namespace>EHR</namespace>
            <type>VERSIONED_COMPOSITION</type>
         </items>
      </folder>/$

      def parser = new OpenEhrXmlParser(true) // true validates against JSON Schema
      Folder f = (Folder)parser.parseLocatable(xml)

      if (!f) println parser.getValidationErrors()

      assert f

      assert f.items.size() == 1
      assert f.folders.size() == 2

      assert f.name.value == 'root'
      assert f.folders[0].name.value == 'subfolder 1'
      assert f.folders[1].name.value == 'subfolder 2'
   }

   void testXmlParserFolder2()
   {
       def xml = $/<folder xsi:type="FOLDER" archetype_node_id="openEHR-EHR-FOLDER.generic.v1">
         <name xsi:type="DV_TEXT">
            <value>root</value>
         </name>
         <archetype_details>
            <archetype_id>
               <value>openEHR-EHR-FOLDER.generic.v1</value>
            </archetype_id>
            <template_id>
               <value>generic_folder</value>
            </template_id>
            <rm_version>1.0.2</rm_version>
         </archetype_details>
         <folders archetype_node_id="openEHR-EHR-FOLDER.generic.v1">
            <name xsi:type="DV_TEXT">
               <value>subfolder 1</value>
            </name>
         </folders>
         <folders archetype_node_id="openEHR-EHR-FOLDER.generic.v1">
            <name xsi:type="DV_TEXT">
               <value>subfolder 2</value>
            </name>
         </folders>
         <items>
            <id xsi:type="HIER_OBJECT_ID">
               <value>d936409e-901f-4994-8d33-ed104d46015b</value>
            </id>
            <namespace>EHR</namespace>
            <type>VERSIONED_COMPOSITION</type>
         </items>
      </folder>/$

      def parser = new OpenEhrXmlParser()
      Folder folder = parser.parseLocatable(xml)

      // serialize status object
      def serializer = new OpenEhrXmlSerializer()
      String xml2 = serializer.serialize(folder)

      println xml2 // TODO: should compare this one with the xml string above without indentation
      // Check xmluint https://stackoverflow.com/questions/16540318/compare-two-xml-strings-ignoring-element-order
      // https://stackoverflow.com/questions/48216562/compare-two-xmls-using-xmlunit-bypassing-the-order-of-elements

   }

   void testXmlParserEhr()
   {
      def xml_ehr = $/
         <ehr xmlns="http://schemas.openehr.org/v1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            <system_id>
               <value>d60e2348-b083-48ce-93b9-916cef1d3a5a</value>
            </system_id>
            <ehr_id>
               <value>7d44b88c-4199-4bad-97dc-d78268e01398</value>
            </ehr_id>
            <time_created>
               <value>2015-01-20T19:30:22.765+01:00</value>
            </time_created>
            <ehr_status>
               <id xsi:type="OBJECT_VERSION_ID">
                  <value>8849182c-82ad-4088-a07f-48ead4180515::openEHRSys.example.com::1</value>
               </id>
               <namespace>local</namespace>
               <type>EHR_STATUS</type>
            </ehr_status>
         </ehr>
      /$

      def parser = new OpenEhrXmlParser(true)
      Ehr ehr = parser.parseEhr(xml_ehr)

      assert ehr

      assert ehr.system_id.value == "d60e2348-b083-48ce-93b9-916cef1d3a5a"

      assert ehr.ehr_status.id.value == "8849182c-82ad-4088-a07f-48ead4180515::openEHRSys.example.com::1"
      assert ehr.ehr_status.type == "EHR_STATUS"
   }

   void testXmlParserEhrWithSchemaError()
   {
      def xml_ehr = $/
         <ehr xmlns="http://schemas.openehr.org/v1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            <system_id>
               <value>d60e2348-b083-48ce-93b9-916cef1d3a5a</value>
            </system_id>
            <ehr_id>
               <value>7d44b88c-4199-4bad-97dc-d78268e01398</value>
            </ehr_id>
            <time_created>
               <value>2015-01-20T19:30:22.765+01:00</value>
            </time_created>
         </ehr>
      /$

      def parser = new OpenEhrXmlParser(true)
      Ehr ehr = parser.parseEhr(xml_ehr)

      assert !ehr

      List<String> errors = parser.getValidationErrors()

      assert errors.size() == 1
      assert errors[0] == "ERROR cvc-complex-type.2.4.b: The content of element 'ehr' is not complete. One of '\u007B\"http://schemas.openehr.org/v1\":ehr_status\u007D' is expected.\nline #: 12\n>>> </ehr>"
   }

   void testJsonParserEhrStatus()
   {
       def xml_ehr_status = $/
         <ehr_status xmlns="http://schemas.openehr.org/v1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" archetype_node_id="openEHR-EHR-EHR_STATUS.generic.v1" xsi:type="EHR_STATUS">
            <name xsi:type="DV_TEXT">
              <value>EHR Status</value>
            </name>
            <archetype_details>
               <archetype_id>
                  <value>openEHR-EHR-EHR_STATUS.generic.v1</value>
               </archetype_id>
               <template_id>
                  <value>generic.en.v1</value>
               </template_id>
               <rm_version>1.0.2</rm_version>
            </archetype_details>
            <subject>
               <external_ref>
                  <id xsi:type="GENERIC_ID">
                     <value>ins01</value>
                     <scheme>id_scheme</scheme>
                  </id>
                  <namespace>DEMOGRAPHIC</namespace>
                  <type>PERSON</type>
               </external_ref>
            </subject>
            <is_queryable>true</is_queryable>
            <is_modifiable>true</is_modifiable>
         </ehr_status>
      /$

      def parser = new OpenEhrXmlParser(true)
      EhrStatus status = parser.parseLocatable(xml_ehr_status)

      //if (!status) println parser.getValidationErrors()

      assert status.archetype_node_id == "openEHR-EHR-EHR_STATUS.generic.v1"
      assert status.name.value == "EHR Status"
      assert status.subject != null
      assert status.subject.external_ref.id.value == "ins01"
      assert status.subject.external_ref.namespace == "DEMOGRAPHIC"
      assert status.other_details == null
      assert status.is_modifiable == true
      assert status.is_queryable == true
   }

   void testJsonParserEhrStatusWithSchemaErrors()
   {
       def xml_ehr_status = $/
         <ehr_status xmlns="http://schemas.openehr.org/v1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" archetype_node_id="openEHR-EHR-EHR_STATUS.generic.v1" xsi:type="EHR_STATUS">
            <name xsi:type="DV_TEXT">
              <value>EHR Status</value>
            </name>
            <archetype_details>
               <archetype_id>
                  <value>openEHR-EHR-EHR_STATUS.generic.v1</value>
               </archetype_id>
               <template_id>
                  <value>generic.en.v1</value>
               </template_id>
               <rm_version>1.0.2</rm_version>
            </archetype_details>
            <subject>
               <external_ref>
                  <id xsi:type="GENERIC_ID">
                     <value>ins01</value>
                     <scheme>id_scheme</scheme>
                  </id>
                  <namespace>DEMOGRAPHIC</namespace>
                  <type>PERSON</type>
               </external_ref>
            </subject>
         </ehr_status>
      /$

      def parser = new OpenEhrXmlParser(true)
      EhrStatus status = parser.parseLocatable(xml_ehr_status)

      assert !status


      List<String> errors = parser.getValidationErrors()

      assert errors.size() == 1
      assert errors[0] == "ERROR cvc-complex-type.2.4.b: The content of element 'ehr_status' is not complete. One of '\u007B\"http://schemas.openehr.org/v1\":is_queryable\u007D' is expected.\nline #: 25\n>>> </ehr_status>"
   }


   void testXmlParserVersion()
   {
      String path = PS +"canonical_xml"+ PS +"test_all_datatypes.version.en.xml"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OpenEhrXmlParser()
      Version v = (Version)parser.parseVersion(xml)

      assert v != null

      //def out = JsonOutput.toJson(c)
      //out = JsonOutput.prettyPrint(out)
      //println out
   }

   void testXmlParserVersion2()
   {
      String path = PS +"canonical_xml"+ PS +"test_all_datatypes.version.en2.xml"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OpenEhrXmlParser()
      Version v = (Version)parser.parseVersion(xml)

      assert v != null

      //def out = JsonOutput.toJson(c)
      //out = JsonOutput.prettyPrint(out)
      //println out
   }


   void testXmlParserComposition()
   {
      String path = PS +"canonical_xml"+ PS +"test_all_datatypes.composition.en.xml"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OpenEhrXmlParser()
      Composition c = (Composition)parser.parseLocatable(xml)

      // TODO: check values at paths

      //def out = JsonOutput.toJson(c)
      //out = JsonOutput.prettyPrint(out)
      //println out
   }

   void testXmlParserAndMarshallerVerasion()
   {
      String path = PS +"canonical_xml"+ PS +"test_all_datatypes.version.en.xml"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OpenEhrXmlParser()
      Version v = (Version)parser.parseVersion(xml)

      OpenEhrXmlSerializer marshal = new OpenEhrXmlSerializer()
      String xml2 = marshal.serialize(v)

      //println xml.replaceAll(">\\s+<", "><").replaceAll("[\n\r]", "")
      //println xml2

      // original xml, when parsed and serialized again, are exactly the same as strings (without indentation and new lines)
      assert xml.replaceAll(">\\s+<", "><").replaceAll("[\n\r]", "") == xml2

      assert v.uid.value == '553c1d04-6d78-4f42-a07d-34a9344ca71d::EMR_APP::1'
      assert v.data.name.value == 'Test all datatypes'

      // assert v.data.context.path == '/context'
      // assert v.data.context.dataPath == '/context'

      // assert v.data.content[0].path == '/content'
      // assert v.data.content[0].dataPath == '/content[0]'

      // assert v.data.content[0].data.path == '/content/data'
      // assert v.data.content[0].data.dataPath == '/content[0]/data'

      // assert v.data.content[0].data.events[0].path == '/content/data/events'
      // assert v.data.content[0].data.events[0].dataPath == '/content[0]/data/events[0]'
   }

   void testXmlParserAndMarshallerComposition()
   {
      String path = PS +"canonical_xml"+ PS +"test_all_datatypes.composition.en.xml"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OpenEhrXmlParser()
      Composition c = (Composition)parser.parseLocatable(xml)

      OpenEhrXmlSerializer marshal = new OpenEhrXmlSerializer()
      String xml2 = marshal.serialize(c)

      //println xml.replaceAll(">\\s+<", "><").replaceAll("[\n\r]", "")
      //println xml2

      // original xml, when parsed and serialized again, are exactly the same as strings (without indentation and new lines)
      assert xml.replaceAll(">\\s+<", "><").replaceAll("[\n\r]", "") == xml2

      assert c.name.value == 'Test all datatypes'
      assert c.archetype_details.archetype_id.value == 'openEHR-EHR-COMPOSITION.test_all_datatypes.v1'

      // assert c.context.path == '/context'
      // assert c.context.dataPath == '/context'

      // assert c.content[0].path == '/content'
      // assert c.content[0].dataPath == '/content[0]'

      // assert c.content[0].data.path == '/content/data'
      // assert c.content[0].data.dataPath == '/content[0]/data'

      // assert c.content[0].data.events[0].path == '/content/data/events'
      // assert c.content[0].data.events[0].dataPath == '/content[0]/data/events[0]'
   }

   void testXmlParserContributionDto()
   {
      String path = PS +"canonical_xml"+ PS +"test_all_datatypes.api_contribution.en.xml"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OpenEhrXmlParser()
      ContributionDto contribution = parser.parseContributionDto(xml)

      assert contribution.versions.size() == 1
      assert contribution.versions[0].uid.value == '553c1d04-6d78-4f42-a07d-34a9344ca71d::CABOLABS::1'
   }

   void testXmlParserContribution()
   {
      String path = PS +"canonical_xml"+ PS +"test_all_datatypes.contribution.en.xml"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OpenEhrXmlParser()
      Contribution contribution = parser.parseContribution(xml)

      assert contribution.versions.size() == 1
      assert contribution.versions[0].id.value == '12345678-197b-4d3b-b567-90bdcf29bc35'
   }
}