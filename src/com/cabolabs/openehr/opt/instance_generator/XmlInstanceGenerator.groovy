package com.cabolabs.openehr.opt.instance_generator

import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.terminology.TerminologyParser
import groovy.xml.MarkupBuilder
import java.text.SimpleDateFormat

/**
 * Based on EMRApp xml.XmlSerializer
 * @author Pablo Pazos <pablo.pazos@cabolabs.com>
 *
 */
class XmlInstanceGenerator {

   def opt
   def writer
   def builder
   
   def terminology
   
   // Formats
   def datetime_format = "yyyyMMdd'T'HHmmss,SSSZ"
   def formatter = new SimpleDateFormat( datetime_format )
   
   // Dummy data (TODO: make this configurable from an external file)
   def composition_settings = ['Hospital A', 'Hospital B', 'Hospital C', 'Hospital D', 'Clinic X']
   def composition_composers = ['Dr. House', 'Dr. Yamamoto']
   
   // Helpers
   def datavalues = ['DV_TEXT', 'DV_CODED_TEXT', 'DV_QUANTITY', 'DV_COUNT',
                     'DV_ORDINAL', 'DV_DATE', 'DV_DATE_TIME']
   def entries = ['OBSERVATION', 'EVALUATION', 'INSTRUCTION', 'ACTION', 'ADMIN_ENTRY']
   
   
   XmlInstanceGenerator()
   {
      writer = new StringWriter()
      builder = new MarkupBuilder(writer)
      builder.setDoubleQuotes(true) // Use double quotes on attributes
      
      // returns random object from any List
      java.util.ArrayList.metaClass.pick {
         delegate.get( new Random().nextInt( delegate.size() ) )
      }
      
      terminology = new TerminologyParser()
      terminology.parseTerms(new File("resources"+ PS +"terminology"+ PS +"openehr_terminology_en.xml"))
   }
   
   String generateXMLCompositionStringFromOPT(OperationalTemplate opt)
   {
      this.opt = opt
      
      builder.composition(xmlns:'http://schemas.openehr.org/v1',
         'xmlns:xsi':'http://www.w3.org/2001/XMLSchema-instance')
      {
         generateCompositionHeader() // name, language, territory, ...
         generateCompositionContent()
      }
      
      return writer.toString()
   }
   
   private generateCompositionHeader()
   {
      // FIXME: this should only generate what doesnt comes from the OPT (category and context are in the OPT!)
      
      // Campos heredados de LOCATABLE
      builder.name() {
         //value('TODO: lookup al arquetipo para obtener el valor por el at0000')
         value( opt.getTerm(opt.definition.archetypeId, 'at0000') )
      }
      builder.archetype_details() { // ARCHETYPED
         archetype_id() { // ARCHETYPE_ID
            value(opt.definition.archetypeId)
         }
         template_id() { // TEMPLATE_ID
            value(opt.templateId)
         }
         rm_version('1.0.2')
      }
      
      // Campos de COMPOSITION
      builder.language() {
         terminology_id() {
            value('ISO_639-1')
         }
         code_string('es') // TODO: deberia salir de una config global
      }
      builder.territory() {
         terminology_id() {
            value('ISO_3166-1')
         }
         code_string('UY') // TODO: deberia salir de una config global
      }
      
      // FIXME: this comes on the OPT
      builder.category() {
         value('event') // TODO: persistent, TODO: to resolve codes that are on the OPT I need the openEHR terminology file loaded.
         defining_code() {
            terminology_id() {
               value('openehr')
            }
            code_string(433)
         }
      }
      
      builder.composer('xsi:type':'PARTY_IDENTIFIED') {
         
         external_ref {
            id('xsi:type': 'HIER_OBJECT_ID') {
               value( java.util.UUID.randomUUID() )
            }
            namespace('DEMOGRAPHIC')
            type('PERSON')
         }
         name( composition_composers.pick() )
         // identifiers DV_IDENTIFIER
      }
      
      builder.context() {
         start_time() {
            value( formatter.format( new Date() ) )
         }
         setting() {
            value( composition_settings.pick() )
            defining_code() {
               terminology_id() {
                  value('openehr') // all openehr terminology should be handled from this.terminology
               }
               code_string(229)
            }
         }
         // health_care_facility
      }
   }
   
   private generateCompositionContent()
   {
      def obj_type, method
      opt.definition.attributes.each { a ->
         
         // wont process all the alternatives from children, just the first
         obj_type = a.children[0].rmTypeName
         method = 'generateAttribute_'+ obj_type
         "$method"(a) // generateAttribute_OBSERVATION(a)
      }
      /*
      builder.content() {
         
      }
      */
   }
   
   /*
   private generateAttribute(AttributeNode a)
   {
      // wont process all the alternatives from children, just the first
      def child_obj = a.children[0]
      
      builder."${a.rmAttributeName}"() {
         
      }
   }
   */
   
   private generateAttribute_EVENT_CONTEXT(AttributeNode a)
   {
      /* already generated on the main method, this avoids processing the node again
      builder."${a.rmAttributeName}"(n:'event_ctx') {
      
      }
      */
   }
   
   private generateAttribute_DV_CODED_TEXT(AttributeNode a)
   {
      // TEST: avoid processing compo.category for now, I need the terminology loaded in order to process that here...
      if (a.rmAttributeName == 'category') return
      
      builder."${a.rmAttributeName}"(n:'coded') {
      
      }
   }
   
   private generateAttribute_OBSERVATION(AttributeNode a)
   {
      builder."${a.rmAttributeName}"() {
      
      }
   }
   
   private generateAttribute_EVALUATION(AttributeNode a)
   {
      builder."${a.rmAttributeName}"() {
      
      }
   }
   
   private generateAttribute_INSTRUCTION(AttributeNode a)
   {
      // wont process all the alternatives from children, just the first
      def obj = a.children[0]
      builder."${a.rmAttributeName}"(archetype_node_id: obj.archetypeId, 'xsi:type':'INSTRUCTION') {
         
         name() {
            //value('TODO: lookup al arquetipo para obtener el valor por el at0000')
            value( opt.getTerm(obj.archetypeId, obj.nodeId) )
         }
         language() {
            terminology_id() {
               value('ISO_639-1')
            }
            code_string('es') // TODO: deberia salir de una config global
         }
         encoding() {
            terminology_id() {
               value('Unicode')
            }
            code_string('UTF-8') // TODO: deberia salir de una config global
         }
         subject('xsi:type':'PARTY_SELF')
         // TBD
         
         // process instruct attributes
         def obj_type, method
         obj.attributes.each { oa ->
            
            // wont process all the alternatives from children, just the first
            obj_type = oa.children[0].rmTypeName
            method = 'generateAttribute_'+ obj_type
            this."$method"(oa) // generateAttribute_OBSERVATION(a) // using this. avoids creating an element!
         }
      }
   }
   
   private generateAttribute_ITEM_TREE(AttributeNode a)
   {
      // wont process all the alternatives from children, just the first
      def obj = a.children[0]
      
      // is it arcehtyped or not?
      def arch_node_id = (obj.archetypeId ?: obj.nodeId)

      builder."${a.rmAttributeName}"(achetype_node_id:arch_node_id, 'xsi:type':'ITEM_TREE') {
      
      }
   }
   
   private generateAttribute_ACTIVITY(AttributeNode a)
   {
      // wont process all the alternatives from children, just the first
      def obj = a.children[0]
      
      // is it arcehtyped or not?
      def arch_node_id = (obj.archetypeId ?: obj.nodeId)

      builder."${a.rmAttributeName}"(achetype_node_id:arch_node_id) {
      
      }
   }
   
   private generateAttribute_ACTION(AttributeNode a)
   {
      builder."${a.rmAttributeName}"() {
      
      }
   }
   
   private generateAttribute_SECTION(AttributeNode a)
   {
      builder."${a.rmAttributeName}"() {
      
      }
   }
}
