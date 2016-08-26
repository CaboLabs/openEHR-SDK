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
   
   
   def XmlInstanceGenerator()
   {
      writer = new StringWriter()
      builder = new MarkupBuilder(writer)
      builder.setDoubleQuotes(true) // Use double quotes on attributes
      
      // ---------------------------------------------------------------------------------
      // Helpers
      
      // returns random object from any List
      java.util.ArrayList.metaClass.pick {
         delegate.get( new Random().nextInt( delegate.size() ) )
      }
      
      String.metaClass.static.randomNumeric = { int digits ->
         def alphabet = ['0','1','2','3','4','5','6','7','8','9']
         new Random().with {
            (1..digits).collect { alphabet[ nextInt( alphabet.size() ) ] }.join()
         }
      }
      
      // String.random( (('A'..'Z')+('a'..'z')+' ').join(), 30 )
      String.metaClass.static.random = { String alphabet, int n ->
         new Random().with {
           (1..n).collect { alphabet[ nextInt( alphabet.length() ) ] }.join()
         }
      }
       
      Integer.metaClass.static.random = { int max, int from ->
         new Random( System.currentTimeMillis() ).nextInt( max+1 ) + from
      }
      
      String.metaClass.static.uuid = { ->
         java.util.UUID.randomUUID().toString()
      }
      
      Date.metaClass.toOpenEHRDateTime = {
         def datetime_format_openEHR = "yyyyMMdd'T'HHmmss,SSSZ" // openEHR format
         def format_oehr = new SimpleDateFormat(datetime_format_openEHR)
         return format_oehr.format(delegate) // string, delegate is the Date instance
      }
      
      Date.metaClass.toOpenEHRDate = {
         def date_format_openEHR = "yyyyMMdd"
         def format_oehr = new SimpleDateFormat(date_format_openEHR)
         return format_oehr.format(delegate) // string, delegate is the Date instance
      }
      
      // ---------------------------------------------------------------------------------
      
      terminology = new TerminologyParser()
      terminology.parseTerms(new File("resources"+ PS +"terminology"+ PS +"openehr_terminology_en.xml"))
      terminology.parseTerms(new File("resources"+ PS +"terminology"+ PS +"openehr_terminology_es.xml"))
   }
   
   String generateXMLCompositionStringFromOPT(OperationalTemplate opt)
   {
      this.opt = opt
      
      builder.composition(xmlns:'http://schemas.openehr.org/v1',
         'xmlns:xsi':'http://www.w3.org/2001/XMLSchema-instance',
         archetype_node_id: opt.definition.archetypeId)
      {
         generateCompositionHeader() // name, language, territory, ...
         generateCompositionContent(opt.definition.archetypeId)
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
               value( String.uuid() )
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
   
   private generateCompositionContent(String parent_arch_id)
   {
      // opt.definition.attributes has attributes category, context and content of the COMPOSITION
      // category and context where already processed on generateCompositionHeader
      def a = opt.definition.attributes.find{ it.rmAttributeName == 'content' }
      
      assert a.rmAttributeName == 'content'
      
      processAttributeChildren(a, parent_arch_id)
   }
   
   /**
    * Continues the opt recursive traverse.
    */
   private processAttributeChildren(AttributeNode a, String parent_arch_id)
   {
      //println "processAttributeChildren parent_arch_id: "+ parent_arch_id
      
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
         if (obj.type == 'ARCHETYPE_SLOT')
         {
            builder.mkp.comment('SLOT NOT PROCESSED')
            return
         }
      
         // wont process all the alternatives from children, just the first
         obj_type = obj.rmTypeName
         method = 'generate_'+ obj_type
         "$method"(obj, parent_arch_id) // generate_OBSERVATION(a)
      }
   }
   
   /*
   private generate(AttributeNode a)
   {
      // wont process all the alternatives from children, just the first
      def child_obj = a.children[0]
      
      builder."${a.rmAttributeName}"() {
         
      }
   }
   */
   
   /**
    * These functions process an attribute of the rmtype mentioned on the function name.
    * e.g. for generate_EVENT_CONTEXT(AttributeNode a), a.rmAttributeName == 'context'
    */
   
   private generate_EVENT_CONTEXT(ObjectNode o, String parent_arch_id)
   {
      // TBD
      /* already generated on the main method, this avoids processing the node again
      builder."${a.rmAttributeName}"(n:'event_ctx') {
      
      }
      */
   }
   
   
   /**
    * DATATYPES -----------------------------------------------------------------------------------------------
    */
   
   /**
    * TODO: for datatypes the generator should check if the data is on the OPT (like constraints that allow
    *       just one value), if not, we will get the first constraint and generate data that complies with
    *       that constraint.
    */
   private generate_DV_CODED_TEXT(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_CODED_TEXT">
        <value>Cut of knee</value>
        <defining_code>
            <terminology_id>
              <value>SNOMED-CT</value>
            </terminology_id>
            <code_string>283434009</code_string>
        </defining_code>
      </value>
      */

      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_CODED_TEXT') {
         value( String.random( (('A'..'Z')+('a'..'z')+' ,.').join(), 30 ) )
         defining_code {
            terminology_id {
               value('ULTIMATE_TERMINOLOGY') // TODO: consider the terminology constraint from the OPT
            }
            code_string( Integer.random(10000, 1000000) )
         }
      }
   }
   
   private generate_DV_TEXT(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_TEXT">
        <value>....</value>
      </value>
      */
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_TEXT') {
         value( String.random( (('A'..'Z')+('a'..'z')+' ,.').join(), 255 ) ) // TODO: improve using word / phrase dictionary
      }
   }
   
   private generate_DV_DATE_TIME(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_DATE_TIME">
         <value>20150515T183951,000-0300</value>
      </value>
      */
      AttributeNode a = o.parent
      /*builder."${a.rmAttributeName}"('xsi:type':'DV_DATE_TIME') {
         value( new Date().toOpenEHRDateTime() )
      }*/
      generate_attr_DV_DATE_TIME(a.rmAttributeName)
   }
   
   /**
    * helper to generate simple datetime attribute.
    */
   private generate_attr_DV_DATE_TIME(String attr)
   {
      /*
      <attr xsi:type="DV_DATE_TIME">
         <value>20150515T183951,000-0300</value>
      </attr>
      */
      builder."${attr}"('xsi:type':'DV_DATE_TIME') {
         value( new Date().toOpenEHRDateTime() )
      }
   }
   
   private generate_attr_CODE_PHRASE(String attr, String terminology, String code)
   {
      builder."${attr}"() {
         terminology_id() {
            value( terminology )
         }
         code_string( code )
      }
   }
   
   private generate_DV_DATE(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_DATE">
         <value>20150515</value>
      </value>
      */
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_DATE') {
         value( new Date().toOpenEHRDate() )
      }
   }
   
   private generate_DV_COUNT(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_COUNT">
         <magnitude>3</magnitude>
      </value>
      */
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_COUNT') {
         magnitude( Integer.random(10, 1) )
      }
   }
   
   private generate_DV_BOOLEAN(ObjectNode o, String parent_arch_id)
   {
      /*
       <value xsi:type="DV_BOOLEAN">
         <value>true</value>
       </value>
      */
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_BOOLEAN') {
         value(true)
      }
   }
   
   private generate_DV_MULTIMEDIA(ObjectNode o, String parent_arch_id)
   {
      /* http://www.cabolabs.com/CaboLabs%20New%20Logo%20Horizontal%20300dpi%20421.png
       <value xsi:type="DV_MULTIMEDIA">
         <media_type>
           <terminology_id>
             <value>IANA_media-types</value>
           </terminology_id>
           <code_string></code_string>
         </media_type>
         <size>1024</size>
       </value>
      */
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_MULTIMEDIA') {
         generate_attr_CODE_PHRASE('media_type', 'IANA_media-types', 'image/jpeg') // TODO: grab the terminology from the ObjectNode
         size(1024)
      }
   }
   
   private generate_DV_QUANTITY(ObjectNode o, String parent_arch_id)
   {
      /*
       <value xsi:type="DV_QUANTITY">
         <magnitude>120.0</magnitude>
         <units>mm[Hg]</units>
       </value>
      */
      
      // Take the first constraint and set the values based on it
      def constraint = o.xmlNode.list[0]
      def lo = (constraint.magnitude.lower_unbounded.text().toBoolean() ? 0 : constraint.magnitude.lower.text().toInteger())
      def hi = (constraint.magnitude.upper_unbounded.text().toBoolean() ? 1000 : constraint.magnitude.upper.text().toInteger())
      
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_QUANTITY') {
         magnitude( Integer.random(hi, lo) ) // TODO: should be BigDecinal not just Integer
         units( constraint.units.text() )
      }
   }
   
   private generate_DV_DURATION(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_DURATION">
        <value>PT30M</value>
      </value>
      */
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_DURATION') {
         value('PT30M') // TODO: Duration String generator
      }
   }
   
   private generate_DV_IDENTIFIER(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_IDENTIFIER">
        <issuer>sdfsfd</issuer>
        <assigner>sdfsfd</assigner>
        <id>sdfsfd</id>
        <type>sdfsfd</type>
      </value>
      */
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_IDENTIFIER') {
         issuer('Hospital de Clinicas')
         assigner('Hospital de Clinicas')
         id(String.randomNumeric(8))
         type('LOCALID')
      }
   }
   
   private generate_DV_ORDINAL(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_ORDINAL">
        <value>[[EYE_RESPONSE:::INTEGER:::1]]</value>
        <symbol>
          <value>None</value>
          <defining_code>
            <terminology_id>
               <value>local</value>
            </terminology_id>
            <code_string>at0010</code_string>
          </defining_code>
        </symbol>
      </value>
      */
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_ORDINAL') {
         value(1) // TODO: take the ordinal value from the ObjectNode
         symbol() {
            value( String.random(('A'..'Z').join(), 15) ) // TODO: take the value from the ObjectNode
            defining_code() {
               terminology_id() {
                  value('local')
               }
               code_string('at0010') // FIXME: take the value from the ObjectNode
            }
         }
      }
   }
   
   /**
    * /DATATYPES -----------------------------------------------------------------------------------------------
    */
   
   /**
    * helper to add name, also checks if the object has a constraint for the name.
    */
   private add_LOCATABLE_elements(ObjectNode o, String parent_arch_id)
   {
      def name_constraint = o.attributes.find { it.rmAttributeName == 'name' }
      if (name_constraint)
      {
         // Check for prmitive constraint over DV_TEXT.value, that is CString.
         // In our model this is just another ObjectNode, just for reference:
         // https://github.com/openEHR/java-libs/blob/master/openehr-aom/src/main/java/org/openehr/am/archetype/constraintmodel/primitive/CString.java
         println "NAME CONSTRAINT: " + name_constraint
         
         /*
         name_constraint.children.each {
            println it.rmTypeName // DV_TEXT
            println it.attributes.rmAttributeName // value
            it.attributes.each { a ->
            
               a.children.each { c ->
                  println c.rmTypeName // String
                  println c.xmlNode.item.list[0].text() // if there is a text constraint, grab the first option (can have many in the list)
               }
            }
         }
         */
         
         // childen[0] DV_TEXT
         //   for the DV_TEXT.value constraint
         //     the first children can be a STRING constraint
         //       check if there is a list constraint and get the first value as the name
         def name_value = name_constraint.children[0].attributes.find { it.rmAttributeName == 'value' }.children[0].xmlNode.item.list[0].text()
         builder.name() {
            value( name_value )
         }
      }
      else // just add the name based on the archetype ontology terms
      {
         builder.name() {
            value( this.opt.getTerm(parent_arch_id, o.nodeId) )
         }
      }
   }
   
   /**
    * helper to add language, encoding and subject to ENTRY nodes.
    */
   private add_ENTRY_elements(ObjectNode o, String parent_arch_id)
   {
      builder.language() {
         terminology_id() {
            value( this.opt.langTerminology )
         }
         code_string( this.opt.langCode )
      }
      builder.encoding() {
         terminology_id() {
            value('Unicode')
         }
         code_string('UTF-8') // TODO: deberia salir de una config global
      }
      builder.subject('xsi:type':'PARTY_SELF')
      
      // ENTRY.protocol
      def oa = o.attributes.find { it.rmAttributeName == 'protocol' }
      if (oa) processAttributeChildren(oa, parent_arch_id)
   }
   
    
   private generate_SECTION(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id
      def oa
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"(archetype_node_id: o.archetypeId, 'xsi:type': o.rmTypeName) {
         name() {
            value( opt.getTerm(parent_arch_id, o.nodeId) )
         }
         
         oa = o.attributes.find { it.rmAttributeName == 'items' }
         if (oa) processAttributeChildren(oa, parent_arch_id)
      }
   }
   
   private generate_OBSERVATION(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id
      
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"(archetype_node_id: o.archetypeId, 'xsi:type': o.rmTypeName) {
         name() {
            value( opt.getTerm(parent_arch_id, o.nodeId) )
         }
         add_ENTRY_elements(o, parent_arch_id)
         
         def oa
         
         // data
         oa = o.attributes.find { it.rmAttributeName == 'data' }
         if (oa) processAttributeChildren(oa, parent_arch_id)
      }
   }
   
   private generate_EVALUATION(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id
      
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"(archetype_node_id: o.archetypeId, 'xsi:type': o.rmTypeName) {
         name() {
            value( opt.getTerm(parent_arch_id, o.nodeId) )
         }
         add_ENTRY_elements(o, parent_arch_id)
         
         def oa
         
         // data
         oa = o.attributes.find { it.rmAttributeName == 'data' }
         if (oa) processAttributeChildren(oa, parent_arch_id)
      }
   }
   
   
   
   private generate_INSTRUCTION(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id
      
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"(archetype_node_id: o.archetypeId, 'xsi:type': o.rmTypeName) {
         
         name() {
            value( opt.getTerm(parent_arch_id, o.nodeId) )
         }
         add_ENTRY_elements(o, parent_arch_id)
         // TBD
         
         // Fix for a bug in the template designer:
         // The OPT has activities before protocol and in the
         // XSD protocol is before activities, this makes the XSD validation fail.
         // Only the attrs that are archetypable, the rest should be generated because
         // come from data not from the OPT, but are optional.
         
         // NARRATIVE DV?TEXT comes before activities in the XSD ....
         
         // Follow the XSD attribute order:
         // - process protocol (optional) // processed by the add_ENTRY_elements
         // - generate narrative (mandatory)
         // - process activities (optional)
         
         // DV_TEXT narrative (not in the OPT, is an IM attribute)
         builder.narrative() {
            value( String.random( (('A'..'Z')+('a'..'z')+' ,.').join(), 255 ) )
         }
         
         // activities
         def oa = o.attributes.find { it.rmAttributeName == 'activities' }
         if (oa) processAttributeChildren(oa, parent_arch_id)
      }
   }
   
   private generate_ACTIVITY(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id
      
      AttributeNode a = o.parent
      
      // is it arcehtyped or not?
      def arch_node_id = (o.archetypeId ?: o.nodeId)

      builder."${a.rmAttributeName}"(archetype_node_id:arch_node_id) {
      
         name() {
            value( opt.getTerm(parent_arch_id, o.nodeId) )
         }
         
         def oa
         
         // description
         oa = o.attributes.find { it.rmAttributeName == 'description' }
         if (oa) processAttributeChildren(oa, parent_arch_id)
         
         /*
         o.attributes.each { oa ->
            
            processAttributeChildren(oa, parent_arch_id)
         }
         */
         
         // DV_PARSABLE timing (IM attr, not in OPT)
         /*
         <timing>
           <value>P1D</value>
           <formalism>ISO8601</formalism>
         </timing>
         */
         timing {
           value("P1D") // TODO: duration string generator
           formalism("ISO8601")
         }
         
         // action_archetype_id
         // mandatory and should come from the OPT but some archetypes don't have it,
         // here I check if that exists in the OPT and if not, just generate dummy data
         oa = o.attributes.find { it.rmAttributeName == 'action_archetype_id' }
         if (oa)
         {
            //println oa.children[0].xmlNode.item.pattern // action_archetype_id from the OPT
            action_archetype_id( oa.children[0].xmlNode.item.pattern )
         }
         else
         {
            action_archetype_id('openEHR-EHR-ACTION\\.sample_action\\.v1')
         }
      }
   }
   
   private generate_ACTION(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id
      
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"(archetype_node_id: o.archetypeId, 'xsi:type': o.rmTypeName) {
         name() {
            value( opt.getTerm(parent_arch_id, o.nodeId) )
         }
         add_ENTRY_elements(o, parent_arch_id)
         
         
         // ACTION.time (not in the OPT, is an IM attribute)
         generate_attr_DV_DATE_TIME('time')
         
         // description
         def oa = o.attributes.find { it.rmAttributeName == 'description' }
         if (oa) processAttributeChildren(oa, parent_arch_id)
         
      
         // add one of the ism_transition in the OPT
         def attr_ism_transition = o.attributes.find { it.rmAttributeName == 'ism_transition' }
         
         // .children[0] ISM_TRANSITION
         //  .attributes current_state 
         //    .children[0] DV_CODED_TEXT
         //      .attributes defining_code
         //       .children[0] CODE_PHRASE
         def code_phrase = attr_ism_transition
                             .children[0].attributes.find { it.rmAttributeName == 'current_state' }
                             .children[0].attributes.find { it.rmAttributeName == 'defining_code' }
                             .children[0]
         
         //println code_phrase.xmlNode.terminology_id.value.text() // openehr
         //println code_phrase.xmlNode.code_list[0].text()         // 524
         
         ism_transition() {
           current_state() {
              value( terminology.getRubric(opt.langCode, code_phrase.xmlNode.code_list[0].text()) )
              defining_code() { // use generate_attr_CODE_PHRASE
                 terminology_id() {
                    value( code_phrase.xmlNode.terminology_id.value.text() )
                 }
                 code_string( code_phrase.xmlNode.code_list[0].text() )
              }
           }
         }
      }
   }
   
   private generate_HISTORY(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id
      
      AttributeNode a = o.parent
      
      // is it arcehtyped or not?
      def arch_node_id = (o.archetypeId ?: o.nodeId)

      builder."${a.rmAttributeName}"(archetype_node_id:arch_node_id, 'xsi:type': o.rmTypeName) {
      
         name() {
            value( opt.getTerm(parent_arch_id, o.nodeId) )
         }
         // IM attribute not present in the OPT
         generate_attr_DV_DATE_TIME('origin')
         
         o.attributes.each { oa ->
            
            processAttributeChildren(oa, parent_arch_id)
         }
      }
   }
   
   private generate_EVENT(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id
      
      AttributeNode a = o.parent
      
      // is it arcehtyped or not?
      def arch_node_id = (o.archetypeId ?: o.nodeId)

      builder."${a.rmAttributeName}"(archetype_node_id:arch_node_id, 'xsi:type': 'POINT_EVENT') { // dont use EVENT because is abstract 
      
         name() {
            value( opt.getTerm(parent_arch_id, o.nodeId) )
         }
         // IM attribute not present in the OPT
         generate_attr_DV_DATE_TIME('time')
         
         o.attributes.each { oa ->
            
            processAttributeChildren(oa, parent_arch_id)
         }
      }
   }
   
   private generate_POINT_EVENT(ObjectNode o, String parent_arch_id)
   {
      generate_EVENT(o, parent_arch_id)
   }
   
   private generate_ITEM_TREE(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id
      
      AttributeNode a = o.parent
      
      // is it arcehtyped or not?
      def arch_node_id = (o.archetypeId ?: o.nodeId)

      builder."${a.rmAttributeName}"(archetype_node_id:arch_node_id, 'xsi:type': o.rmTypeName) {
      
         name() {
            value( opt.getTerm(parent_arch_id, o.nodeId) )
         }
         
         o.attributes.each { oa ->
            
            processAttributeChildren(oa, parent_arch_id)
         }
      }
   }
   
   private generate_CLUSTER(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id
      
      AttributeNode a = o.parent
      
      // is it arcehtyped or not?
      def arch_node_id = (o.archetypeId ?: o.nodeId)
      
      builder."${a.rmAttributeName}"(archetype_node_id:arch_node_id, 'xsi:type': o.rmTypeName) {
      
         /*name() {
            value( opt.getTerm(parent_arch_id, o.nodeId) )
         }*/
         add_LOCATABLE_elements(o, parent_arch_id)
      
         o.attributes.each { oa ->
            if (oa.rmAttributeName == 'name') return // avoid processing name constraints, thos are processde by add_LOCATABLE_elements
            processAttributeChildren(oa, parent_arch_id)
         }
      }
   }
   
   
   private generate_ELEMENT(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id
      
      AttributeNode a = o.parent
      
      // is it arcehtyped or not?
      def arch_node_id = (o.archetypeId ?: o.nodeId)
      
      builder."${a.rmAttributeName}"(archetype_node_id:arch_node_id, 'xsi:type': o.rmTypeName) {
      
         add_LOCATABLE_elements(o, parent_arch_id)
         
         o.attributes.each { oa ->
            if (oa.rmAttributeName == 'name') return // avoid processing name constraints, thos are processde by add_LOCATABLE_elements
            processAttributeChildren(oa, parent_arch_id)
         }
      }
   }

}
