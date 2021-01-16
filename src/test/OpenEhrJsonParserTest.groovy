package test

import com.cabolabs.openehr.formats.OpenEhrJsonParser
import com.cabolabs.openehr.formats.OpenEhrXmlSerializer
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
import groovy.json.JsonOutput

class OpenEhrJsonParserTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")
   
   void testJsonParserInstruction()
   {
      String path = "resources" + PS +"canonical_json"+ PS +"lab_order.json"
      File file = new File(path)
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)
      
      def out = JsonOutput.toJson(c)
      out = JsonOutput.prettyPrint(out)
      //println out
   }
   
   void testJsonParserObservation()
   {
      String path = "resources" + PS +"canonical_json"+ PS +"lab_results.json"
      File file = new File(path)
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)
      
      def out = JsonOutput.toJson(c)
      out = JsonOutput.prettyPrint(out)
      //println out
   }
   
   void testJsonParserReferralWithParticipations()
   {
      String path = "resources" + PS +"canonical_json"+ PS +"referral.json"
      File file = new File(path)
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)
      
      def out = JsonOutput.toJson(c)
      out = JsonOutput.prettyPrint(out)
      //println out
   }
   
   void testJsonParserAdminEntry()
   {
      String path = "resources" + PS +"canonical_json"+ PS +"admin.json"
      File file = new File(path)
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)
      
      def out = JsonOutput.toJson(c)
      out = JsonOutput.prettyPrint(out)
      //println out
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
      a.language.terminology_id = new TerminologyId(value:'LOCAL')
      
      a.encoding = new CodePhrase()
      a.encoding.code_string = 'UTF-8'
      a.encoding.terminology_id = new TerminologyId(value:'LOCAL')
      
      a.archetype_node_id = 'openEHR-EHR-ADMIN_ENTRY.test.v1'
      a.name = new DvText(value:'admin')
      a.data = new ItemTree(name: new DvText(value:'tree'))
      
      c.content.add(a)
      
      OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
      String out = serial.serialize(c)
      
      //println out
   }
   */
   
   void testJsonParserAdminEntryTiXml()
   {
      // parse JSON
      String path = "resources" + PS +"canonical_json"+ PS +"admin.json"
      File file = new File(path)
      String json = file.text
      def parser = new OpenEhrJsonParser()
      Composition c = (Composition)parser.parseJson(json)
      
      // serialize to XML
      OpenEhrXmlSerializer serial = new OpenEhrXmlSerializer()
      String out = serial.serialize(c)
      println out
   }
   
}
