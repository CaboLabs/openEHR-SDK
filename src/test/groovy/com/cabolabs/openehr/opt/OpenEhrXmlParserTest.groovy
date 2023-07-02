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
      def xml = """<folder xsi:type="FOLDER" archetype_node_id="openEHR-EHR-FOLDER.generic.v1">
         <name xsi:type="DV_TEXT">
            <value>root</value>
         </name>
         <archetype_details>
            <archetype_id>
               <value>openEHR-EHR-FOLDER.generic.v1</value>
            </archetype_id>
            <template_id>
               <value>alternative_types.en.v1</value>
            </template_id>
            <rm_version>1.0.2</rm_version>
         </archetype_details>
         <folders archetype_node_id="openEHR-EHR-FOLDER.generic.v1">
            <name xsi:type="DV_TEXT">
               <value>subfolder 2</value>
            </name>
            <items>
               <id xsi:type="HIER_OBJECT_ID">
                  <value>d936409e-901f-4994-8d33-ed104d460789</value>
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
      </folder>"""

      def parser = new OpenEhrXmlParser(true) // true validates against JSON Schema
      Folder f = (Folder)parser.parseLocatable(xml)

      assert f
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