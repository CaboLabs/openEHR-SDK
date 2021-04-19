package  com.cabolabs.openehr.opt

import com.cabolabs.openehr.formats.OpenEhrXmlParser
//import com.cabolabs.openehr.formats.OpenEhrXmlSerializer
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
import com.cabolabs.openehr.rm_1_0_2.common.change_control.OriginalVersion
import com.cabolabs.openehr.rm_1_0_2.common.change_control.Version

class OpenEhrXmlParserTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")
   
   void testXmlParserVersion()
   {
      String path = PS +"canonical_xml"+ PS +"test_all_datatypes.version.en.xml"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OpenEhrXmlParser()
      Version c = (Version)parser.parseVersionXml(xml)
      
      def out = JsonOutput.toJson(c)
      out = JsonOutput.prettyPrint(out)
      println out
   }
   
   void testXmlParserComposition()
   {
      String path = PS +"canonical_xml"+ PS +"test_all_datatypes.composition.en.xml"
      File file = new File(getClass().getResource(path).toURI())
      String xml = file.text
      def parser = new OpenEhrXmlParser()
      Composition c = (Composition)parser.parseXml(xml)
      
      def out = JsonOutput.toJson(c)
      out = JsonOutput.prettyPrint(out)
      println out
   }
}