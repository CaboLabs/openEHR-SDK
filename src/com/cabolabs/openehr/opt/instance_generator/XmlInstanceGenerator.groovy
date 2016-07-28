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

   static String PS = File.separator
   
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
            value( this.opt.langTerminology )
         }
         code_string( this.opt.langCode )
      }
      builder.territory() {
         terminology_id() {
            value('ISO_3166-1')
         }
         code_string('UY') // TODO: deberia salir de una config global o de parametros
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
      // opt.definition.attributes has attributes category, context and content of the COMPOSITION
      // category and context where already processed on generateCompositionHeader
      def a = opt.definition.attributes.find{ it.rmAttributeName == 'content' }
      
      assert a.rmAttributeName == 'content'
      
      processAttributeChildren(a)
   }
   
   /**
    * Continues the opt recursive traverse.
    */
   private processAttributeChildren(AttributeNode a)
   {
      def obj_type, method
   
      // Process all the attributes if it is C_MULTIPLE_ATTRIBUTE
      // or just the first alternative if it is C_SINGLE_ATTRIBUTE
      def children
      
      if (a.type == 'C_MULTIPLE_ATTRIBUTE')
      {
         children = a.children
      }
      else
      {
         children = [ a.children[0] ]
      }
      
      children.each { obj ->
      
         // Avoid processing slots
         if (obj.type == 'ARCHETYPE_SLOT') return
      
         // wont process all the alternatives from children, just the first
         obj_type = obj.rmTypeName
         method = 'generateAttribute_'+ obj_type
         "$method"(obj) // generateAttribute_OBSERVATION(a)
      }
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
   
   /**
    * These functions process an attribute of the rmtype mentioned on the function name.
    * e.g. for generateAttribute_EVENT_CONTEXT(AttributeNode a), a.rmAttributeName == 'context'
    */
   
   private generateAttribute_EVENT_CONTEXT(ObjectNode o)
   {
      /* already generated on the main method, this avoids processing the node again
      builder."${a.rmAttributeName}"(n:'event_ctx') {
      
      }
      */
   }
   
   /**
    * TODO: for datatypes the generator should check if the data is on the OPT (like constraints that allow
    *       just one value), if not, we will get the first constraint and generate data that complies with
    *       that constraint.
    */
   private generateAttribute_DV_CODED_TEXT(ObjectNode o)
   {
      // TEST: avoid processing compo.category for now, I need the terminology loaded in order to process that here...
      //if (a.rmAttributeName == 'category') return
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"(TODO:'coded') {
      
      }
   }
   
   private generateAttribute_DV_TEXT(ObjectNode o)
   {
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"(TODO:'text') {
      
      }
   }
   
   private generateAttribute_DV_DATE_TIME(ObjectNode o)
   {
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"(TODO:'datetime') {
      
      }
   }
   
   private generateAttribute_DV_BOOLEAN(ObjectNode o)
   {
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"(TODO:'bool') {
      
      }
   }
   
   private generateAttribute_DV_DURATION(ObjectNode o)
   {
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"(TODO:'duration') {
      
      }
   }
   
   private generateAttribute_OBSERVATION(ObjectNode o)
   {
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"() {
         name() {
            value( opt.getTerm(o.archetypeId, o.nodeId) )
         }
      }
   }
   
   private generateAttribute_EVALUATION(ObjectNode o)
   {
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"() {
         name() {
            value( opt.getTerm(o.archetypeId, o.nodeId) )
         }
      }
   }
   
   private generateAttribute_INSTRUCTION(ObjectNode o)
   {
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"(archetype_node_id: o.archetypeId, 'xsi:type':'INSTRUCTION') {
         
         name() {
            //value('TODO: lookup al arquetipo para obtener el valor por el at0000')
            value( opt.getTerm(o.archetypeId, o.nodeId) )
         }
         language() {
            terminology_id() {
               value( this.opt.langTerminology )
            }
            code_string( this.opt.langCode )
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
         o.attributes.each { oa ->
            
            processAttributeChildren(oa)
         }
      }
   }
   
   private generateAttribute_ITEM_TREE(ObjectNode o)
   {
      AttributeNode a = o.parent
      
      // is it arcehtyped or not?
      def arch_node_id = (o.archetypeId ?: o.nodeId)

      builder."${a.rmAttributeName}"(achetype_node_id:arch_node_id, 'xsi:type':'ITEM_TREE') {
      
         name() {
            value( opt.getTerm(o.archetypeId, o.nodeId) )
         }
         
         def obj_type, method
         o.attributes.each { oa ->
            
            processAttributeChildren(oa)
         }
      }
   }
   
   private generateAttribute_CLUSTER(ObjectNode o)
   {
      AttributeNode a = o.parent
      
      // is it arcehtyped or not?
      def arch_node_id = (o.archetypeId ?: o.nodeId)
      
      builder."${a.rmAttributeName}"(achetype_node_id:arch_node_id, 'xsi:type':'CLUSTER') {
      
         name() {
            value( opt.getTerm(o.archetypeId, o.nodeId) )
         }
      
         def obj_type, method
         o.attributes.each { oa ->
            
            processAttributeChildren(oa)
         }
      }
   }
   
   
   private generateAttribute_ELEMENT(ObjectNode o)
   {
      AttributeNode a = o.parent
      
      // is it arcehtyped or not?
      def arch_node_id = (o.archetypeId ?: o.nodeId)
      
      /***
      *
      *
      * hay que pasarle el parent archetype porque el obj no lo tiene y no puedo buscar el termino.
      *
      */
      println "ELEMENT text for :"+ o.archetypeId +" "+ o.nodeId // FIXME: if o is not the root of an archetype, archetypeId is null.

      builder."${a.rmAttributeName}"(achetype_node_id:arch_node_id, 'xsi:type':'ELEMENT') {
      
         name() {
            value( opt.getTerm(o.archetypeId, o.nodeId) )
         }
         
         def obj_type, method
         o.attributes.each { oa ->
            
            processAttributeChildren(oa)
         }
      }
   }
   
   private generateAttribute_ACTIVITY(ObjectNode o)
   {
      AttributeNode a = o.parent
      
      // is it arcehtyped or not?
      def arch_node_id = (o.archetypeId ?: o.nodeId)

      builder."${a.rmAttributeName}"(achetype_node_id:arch_node_id) {
      
         def obj_type, method
         o.attributes.each { oa ->
            
            processAttributeChildren(oa)
         }
      }
   }
   
   private generateAttribute_ACTION(ObjectNode o)
   {
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"() {
         name() {
            value( opt.getTerm(o.archetypeId, o.nodeId) )
         }
      }
   }
   
   private generateAttribute_SECTION(ObjectNode o)
   {
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"() {
         name() {
            value( opt.getTerm(o.archetypeId, o.nodeId) )
         }
      }
   }
}
