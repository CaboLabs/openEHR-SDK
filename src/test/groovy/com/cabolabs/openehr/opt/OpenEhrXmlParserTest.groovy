package com.cabolabs.openehr.opt

import com.cabolabs.openehr.formats.OpenEhrXmlParser
import com.cabolabs.openehr.formats.OpenEhrXmlSerializer
import com.cabolabs.openehr.opt.instance_validation.XmlInstanceValidation
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Archetyped
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

class OpenEhrXmlParserTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")
   
   
   void testXmlParserVersion()
   {
      String path = PS +"canonical_xml"+ PS +"test_all_datatypes.version.en.xml"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OpenEhrXmlParser()
      Version c = (Version)parser.parseVersionXml(xml)
      
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
      Composition c = (Composition)parser.parseXml(xml)
      
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
      Version v = (Version)parser.parseVersionXml(xml)

      OpenEhrXmlSerializer marshal = new OpenEhrXmlSerializer()
      String xml2 = marshal.serialize(v)

      //println xml.replaceAll(">\\s+<", "><").replaceAll("[\n\r]", "")
      //println xml2

      // original xml, when parsed and serialized again, are exactly the same as strings (without indentation and new lines)
      assert xml.replaceAll(">\\s+<", "><").replaceAll("[\n\r]", "") == xml2


      assert v.data.context.path == '/context'
      assert v.data.context.dataPath == '/context'

      assert v.data.content[0].path == '/content'
      assert v.data.content[0].dataPath == '/content[0]'

      assert v.data.content[0].data.path == '/content/data'
      assert v.data.content[0].data.dataPath == '/content[0]/data'

      assert v.data.content[0].data.events[0].path == '/content/data/events'
      assert v.data.content[0].data.events[0].dataPath == '/content[0]/data/events[0]'
   }

   void testXmlParserAndMarshallerComposition()
   {
      String path = PS +"canonical_xml"+ PS +"test_all_datatypes.composition.en.xml"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OpenEhrXmlParser()
      Composition c = (Composition)parser.parseXml(xml)

      OpenEhrXmlSerializer marshal = new OpenEhrXmlSerializer()
      String xml2 = marshal.serialize(c)

      //println xml.replaceAll(">\\s+<", "><").replaceAll("[\n\r]", "")
      //println xml2

      // original xml, when parsed and serialized again, are exactly the same as strings (without indentation and new lines)
      assert xml.replaceAll(">\\s+<", "><").replaceAll("[\n\r]", "") == xml2


      assert c.context.path == '/context'
      assert c.context.dataPath == '/context'

      assert c.content[0].path == '/content'
      assert c.content[0].dataPath == '/content[0]'

      assert c.content[0].data.path == '/content/data'
      assert c.content[0].data.dataPath == '/content[0]/data'

      assert c.content[0].data.events[0].path == '/content/data/events'
      assert c.content[0].data.events[0].dataPath == '/content[0]/data/events[0]'
   }

   void testXmlParserApiContribution()
   {
      String path = PS +"canonical_xml"+ PS +"test_all_datatypes.api_contribution.en.xml"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OpenEhrXmlParser()
      Contribution contribution = parser.parseContribution(xml)

      assert contribution.versions.size() == 1
      assert contribution.versions[0].id.value == '553c1d04-6d78-4f42-a07d-34a9344ca71d::CABOLABS::1'
   }
}