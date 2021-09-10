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
class XmlInstanceGeneratorForCommitter {

   static String PS = File.separator

   def opt
   def writer
   def builder

   def terminology

   // Field names should be unique for the committer,
   // because if multiple values come from the UI the
   // controller will treat them as a date. This will
   // store the used names for checking uniqueness.
   def field_names = []
   static int counter = 1 // help to create unique names

   // Formats
   def datetime_format = "yyyy-MM-dd'T'HH:mm:ss.sssZ" // https://www.w3.org/TR/xmlschema-2/#dateTime
   def formatter = new SimpleDateFormat( datetime_format )

   // Dummy data (TODO: make this configurable from an external file)
   def composition_settings = [
      'en': [
         '225': 'home',
         '227': 'emergency care',
         '228': 'primary medical care'
      ],
      'es': [
         '225': 'hogar',
         '227': 'atención de emergencia',
         '228': 'atención médica primaria'
      ]
      // TODO: for other laguages we need to add more here, or access the terminology and pick terms from there...
   ]
   def composition_composers = ['Dr. House', 'Dr. Yamamoto']

   def XmlInstanceGeneratorForCommitter()
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

      // used to select a setting from a Map
      java.util.LinkedHashMap.metaClass.pick {
         (delegate.entrySet() as List).pick()
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

      terminology = TerminologyParser.getInstance()
      terminology.parseTerms(getClass().getResourceAsStream(PS +"terminology"+ PS +"openehr_terminology_en.xml")) // this works to load the resource from the jar
      terminology.parseTerms(getClass().getResourceAsStream(PS +"terminology"+ PS +"openehr_terminology_es.xml"))
      terminology.parseTerms(getClass().getResourceAsStream(PS +"terminology"+ PS +"openehr_terminology_pt.xml"))
   }

   /**
    * generates vresion with composition.
    */
   String generateXMLVersionStringFromOPT(OperationalTemplate opt)
   {
      this.opt = opt

      builder.version(xmlns:       'http://schemas.openehr.org/v1',
                      'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
                      'xsi:type':  'ORIGINAL_VERSION') {
         /**
          * required by the XSD
          */
         contribution {
           id('xsi:type':'HIER_OBJECT_ID') {
             value('[[CONTRIBUTION:::UUID:::ANY]]')
           }
           namespace('EHR::COMMON')
           type('CONTRIBUTION')
         }

         commit_audit() {
            system_id('CABOLABS_EHR') // TODO: should be configurable and the same as auditSystemId sent to the commit service from CommitJob

            /*
             Identity and optional reference into identity
             management service, of user who committed the item.
             */
            committer('xsi:type':"PARTY_IDENTIFIED") {
               external_ref {
                  id('xsi:type': 'HIER_OBJECT_ID') {
                     value('[[COMMITTER_ID:::UUID:::ANY]]')
                  }
                  namespace('DEMOGRAPHIC')
                  type('PERSON')
               }
               name('[[COMMITTER_NAME:::STRING:::Dr. House]]')
               // identifiers DV_IDENTIFIER
            }

            /*
             * This time will be overriden by the server to be copliant with this rule:
             *
             * The time_committed attribute in both the Contribution and Version audits
             * should reflect the time of committal to an EHR server, i.e. the time of
             * availability to other users in the same system. It should therefore be
             * computed on the server in implementations where the data are created
             * in a separate client context.
             */
            time_committed {
               value('[[TIME_COMMITTED:::DATETIME:::NOW]]')
            }

            change_type() {
               value( 'creation' ) // just new versions are generated by now
               defining_code() {
                  terminology_id() {
                     value('openehr')
                  }
                  code_string('249')
               }
            }
         } // commit_audit

         /**
          * version.uid is mandatory by the schema.
          */
         uid('xsi:type': 'OBJECT_VERSION_ID') {
            // just new versions are generated by now
            value('[[VERSION_ID:::VERSION_ID:::ANY]]')
         }

         data('xsi:type': 'COMPOSITION', archetype_node_id: opt.definition.archetypeId) {

            generateCompositionHeader() // name, language, territory, ...
            generateCompositionContent(opt.definition.archetypeId)
         }

         lifecycle_state() {
            value('complete')
            defining_code() {
               terminology_id() {
                  value('openehr')
               }
               code_string('532')
            }
         }
      }

      return writer.toString()
   }

   /**
    * generates just the composition, no version info
    */
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
         value( opt.getTerm(opt.definition.archetypeId, opt.definition.nodeId) )
      }
      builder.uid('xsi:type':'HIER_OBJECT_ID') { // this is optional, if not included is set by the server
         value('[[COMPOSITION:::UUID:::ANY]]')
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

      def category_code = opt.getNodes('/category/defining_code')[0].codeList[0]

      builder.category() {
         value(terminology.getRubric(opt.langCode, category_code))
         defining_code() {
            terminology_id() {
               value('openehr')
            }
            code_string(category_code)
         }
      }

      builder.composer('xsi:type':'PARTY_IDENTIFIED') {

         external_ref {
            id('xsi:type': 'HIER_OBJECT_ID') {
               value('[[COMPOSER_ID:::UUID:::ANY]]')
            }
            namespace('DEMOGRAPHIC')
            type('PERSON')
         }
         name('[[COMPOSER_NAME:::STRING:::Dr. House]]')
         // identifiers DV_IDENTIFIER
      }

      // context is declared on the OPT only if it contains constraints for other_context
      def context = opt.definition.attributes.find{ it.rmAttributeName == 'context' }

      if (category_code == '431' && context)
      {
         throw new Exception("Error: COMPOSITION is persistent but contains context.")
      }

      if (category_code == '433') // event
      {
         def setting_entry
         if (!composition_settings[this.opt.langCode]) setting_entry = composition_settings['en'].pick()
         else setting_entry = composition_settings[this.opt.langCode].pick()
         
         def m = builder.mkp
         
            
         builder.context() {
            start_time() {
               value('[[COMPOSITION_DATE:::DATETIME:::NOW]]')
            }
            setting() {
               // this codes the setting allowing the EhrCommitter user to select one
               m.yield("[[SETTING:::CODEDTEXT:::(home::225::openehr,,,emergency care::227::openehr,,,primary medical care::228::openehr,,,primary nursing care::229::openehr,,,primary allied care::230::openehr)]]")
               /*
               value(setting_entry.value)
               defining_code() {
                  terminology_id() {
                     value('openehr') // all openehr terminology should be handled from this.terminology
                  }
                  code_string(setting_entry.key)
               }
               */
            }
            // health_care_facility

            if (context)
            {
               def other_context = context.children[0].attributes.find{ it.rmAttributeName == 'other_context' }
               if (other_context)
               {
                  processAttributeChildren(other_context, opt.definition.archetypeId)
               }
            }
         }
      }
   }

   private generateCompositionContent(String parent_arch_id)
   {
      // opt.definition.attributes has attributes category, context and content of the COMPOSITION
      // category and context where already processed on generateCompositionHeader
      def a = opt.definition.attributes.find{ it.rmAttributeName == 'content' }

      if (!a) throw new Exception("The OPT doesn't have a structure for COMPOSITION.content")

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
            builder.mkp.comment('SLOT IN '+ obj.path +' NOT PROCESSED')
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
      <children xsi:type="C_CODE_PHRASE">
        <rm_type_name>CODE_PHRASE</rm_type_name>
        <occurrences>
          <lower_included>true</lower_included>
          <upper_included>true</upper_included>
          <lower_unbounded>false</lower_unbounded>
          <upper_unbounded>false</upper_unbounded>
          <lower>1</lower>
          <upper>1</upper>
        </occurrences>
        <node_id />
        <terminology_id>
          <value>local</value>
        </terminology_id>
        <code_list>at0037</code_list>
        <code_list>at0038</code_list>
        <code_list>at0039</code_list>
        ...
      */
      def codes = [:]
      def code
      def code_phrase = opt.getNodes(o.templatePath + '/defining_code')[0] // nodes are indexed by template path not by archetype path
      /*
      opt.nodes.findAll{it.key.contains('problem_list')}.sort{it.key}.each {println it.toString() +' / '+ it.value.rmTypeName}
      println o.templatePath
      println code_phrase.templatePath
      */
      if (!code_phrase)
      {
         println "Avoid generating DV_CODED_TEXT because it has no constraints for codes or terminology"
         return
      }
      /*
            println code_phrase.getClass()
            println code_phrase.nodes
      */
      def def_code = o.attributes.find { it.rmAttributeName == 'defining_code' }
      def terminology
      if (def_code)
      {
         terminology = code_phrase.terminologyId
      }

      if (!terminology)
      {
         // format terminology:LOINC?subset=laboratory_services
         def externalTerminologyRef = def_code.children[0].terminologyRef // CCodePhrase.terminologyRef
         if (!externalTerminologyRef)
         {
            terminology = "terminology_not_specified_as_constraint_or_referenceSetUri_in_opt"
         }
         else
         {
            terminology = externalTerminologyRef.split("\\?")[0].split(":")[1]
         }

         // TODO:
         // The instance shouldn't have a list of codes on this case since the constraint is
         // an external terminology. Since this is for the committed, the TAG should help
         // the committer to create a search input on this case, instead of a eslect of codes.
      }
      // get texts from archetype ontology
      else if (terminology == 'local')
      {
         //name = this.opt.getTerm(parent_arch_id, first_code)
         code_phrase.codeList.each {

            code = it
            codes[code] = opt.getTerm(parent_arch_id, code) // at00XX -> name
         }
      }
      // get texts form openehr terminology
      else if (terminology == 'openehr')
      {
         //name = this.terminology.getRubric(opt.langCode, first_code)
         code_phrase.codeList.each {

            code = it
            codes[code] = this.terminology.getRubric(opt.langCode, code) // at00XX -> name
         }
      }

      // FIXME: this works only if the CODED_TEXT is in an ELEMENT, where the
      //        the attribute is assumed to be "value" in the EhrCommitter.
      //        For a generic solution, we should include the name of the tag
      //        that is of type CODED_TEXT, like math_function for INTERVAL_EVENT.

      def label = this.label(o, parent_arch_id)

      // Adds a text node inside the current parent
      // [[STATUS:::CODEDTEXT:::(Interim::at0037::local,,,Final::at0038::local,,,Never performed::at0079::local)]]
      def tag = '[[' + label +':::CODEDTEXT:::('

      // If the coded text has a terminologic constraint it will not have codes.
      if (codes)
      {
         codes.each { _code, _text ->
            tag += _text +'::'+ _code +'::'+ terminology +',,,' // separator is ,,, because the text can have ,
         }
         tag = tag[0..-4]
      }
      tag += ')]]'

      def m = builder.mkp
      m.yield( tag )
   }

   private generate_DV_TEXT(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_TEXT">
        <value>....</value>
      </value>
      */

      def label = this.label(o, parent_arch_id)
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_TEXT') {
         value('[['+ label +':::STRING:::'+ String.random( (('A'..'Z')+('a'..'z')+' ,.').join(), 20 ) +']]') // TODO: improve using word / phrase dictionary
      }
   }

   private generate_DV_DATE_TIME(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_DATE_TIME">
         <value>20150515T183951,000-0300</value>
      </value>
      */

      def label = this.label(o, parent_arch_id)
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_DATE_TIME') {
         value('[['+ label +':::DATETIME:::NOW]]')
      }
   }
   
   private generate_DV_TIME(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_TIME">
         <value>183951</value>
      </value>
      */

      def label = this.label(o, parent_arch_id)
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_TIME') {
         value('[['+ label +':::TIME:::NOW]]')
      }
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
         value(new Date().toOpenEHRDateTime())
      }
   }

   private generate_attr_CODE_PHRASE(String attr, String terminology, String code)
   {
      builder."${attr}"() {
         terminology_id() {
            value( terminology )
         }
         code_string(code)
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
         value( new Date().toOpenEHRDate() ) // TODO: this should be tagged
      }
   }

   private generate_DV_COUNT(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_COUNT">
         <magnitude>3</magnitude>
      </value>
      */
      def label = this.label(o, parent_arch_id)
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_COUNT') {
         magnitude('[['+ label +':::INTEGER:::RANGE_0..100]]') // TODO: consider constraints
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
         value(true) // TODO: this should be tagged
      }
   }

   private generate_DV_MULTIMEDIA(ObjectNode o, String parent_arch_id)
   {
      /* http://www.cabolabs.com/CaboLabs%20New%20Logo%20Horizontal%20300dpi%20421.png
       <value xsi:type="DV_MULTIMEDIA">
         <alternate_text>Es una imagen!</alternate_text>
         <data>iVBORw0KGgoAAAANSUhEUgAAAmwAAAJYCAYAAADff...</data>
         <media_type>
           <terminology_id>
             <value>IANA_media-types</value>
           </terminology_id>
           <code_string></code_string>
         </media_type>
         <size>1024</size>
       </value>
      */

      def _dataf = getClass().getResourceAsStream(PS +"images"+ PS +"cabolabs_logo.png").getBytes()
      def _datab64 = _dataf.encodeBase64().toString()

      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_MULTIMEDIA') {
         data(_datab64)
         generate_attr_CODE_PHRASE('media_type', 'IANA_media-types', 'image/png') // TODO: grab the terminology from the ObjectNode
         size(_datab64.size())
      }
   }

   private generate_DV_PARSABLE(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_PARSABLE">
       <value>20170629</value>
       <formalism>ISO8601</formalism>
      </value>
      */
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_PARSABLE') {
         // TODO: consider formalisms from OPT to generate a valid value, hardcoded for now.
         value('20170629') // TODO: this should be tagged
         formalism('ISO8601')
      }
   }

   private generate_DV_PROPORTION(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_PROPORTION">
       <numerator>1.5</numerator>
       <denominator>1</denominator>
       <type>1</type>
       <precision>0</precision>
      </value>
      */
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_PROPORTION') {
         // TODO: consider proportion type from OPT to generate valid values, hardcoded for now.
         numerator('1.5') // TODO: this should be tagged
         denominator('1')
         type('0')
         //precision('0')
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
      def constraint = o.list[0] // CQuantityItem
      def lo, hi
      if (!constraint || !constraint.magnitude)
      {
         lo = 0
         hi = 1000
      }
      else
      {
         //println o.path
         //println constraint.magnitude
         lo = (constraint.magnitude.lowerUnbounded ? 0 : constraint.magnitude.lower)
         hi = (constraint.magnitude.upperUnbounded ? 1000 : constraint.magnitude.upper)
      }

      def _units = []
      if (o.list.units)
      {
         _units = o.list.units
      }
      else
      {
         _units << '_no_constraint_defined_'
      }
      String units_string = _units.toString()

      def label = this.label(o, parent_arch_id)
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_QUANTITY') {
         magnitude('[['+ label +'_magnitude:::REAL:::RANGE_'+ lo +'..'+ hi +']]') // TODO: should be BigDecinal not just Integer
         units('[['+ label +'_units:::LIST:::'+ units_string +']]') // TODO: select units
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
         issuer('Hospital de Clinicas') // TODO: should be coded
         assigner('Hospital de Clinicas')
         id(String.randomNumeric(8))
         type('LOCALID')
      }
   }

   private generate_DV_ORDINAL(ObjectNode o, String parent_arch_id)
   {
      /*
      <children xsi:type="C_DV_ORDINAL">
       <rm_type_name>DV_ORDINAL</rm_type_name>
       <occurrences>
         <lower_included>true</lower_included>
         <upper_included>true</upper_included>
         <lower_unbounded>false</lower_unbounded>
         <upper_unbounded>false</upper_unbounded>
         <lower>1</lower>
         <upper>1</upper>
       </occurrences>
       <node_id />
       <list>
         <value>1</value>
         <symbol>
           <value />
           <defining_code>
             <terminology_id>
               <value>local</value>
             </terminology_id>
             <code_string>at0013</code_string>
           </defining_code>
         </symbol>
       </list>
       <list>
         <value>2</value>
         <symbol>
           <value />
           <defining_code>
             <terminology_id>
               <value>local</value>
             </terminology_id>
             <code_string>at0014</code_string>
           </defining_code>
         </symbol>
       </list>
       <list>
         <value>3</value>
         <symbol>
           <value />
           <defining_code>
             <terminology_id>
               <value>local</value>
             </terminology_id>
             <code_string>at0015</code_string>
           </defining_code>
         </symbol>
       </list>
     </children>
      */
      def constraints = []
      def text, code, terminology_id, ordinal
      o.list.each { list_item -> // CDvOrdinalItem

         code = list_item.symbol.codeString
         text = opt.getTerm(parent_arch_id, code)
         terminology_id = list_item.symbol.terminologyId
         ordinal = list_item.value

         constraints << [text: text, code: code, terminology_id: terminology_id, ordinal: ordinal]
      }

      def label = this.label(o, parent_arch_id)

      def tag = '[[' + label +':::ORDINAL:::('

      // (text::atcode::terminologyid::ordinal, text::atcode::terminologyid::ordinal, text::atcode::terminologyid::ordinal, ...)
      if (constraints)
      {
         constraints.each { item ->
            tag += item.text +'::'+ item.code +'::'+ item.terminology_id +'::'+ item.ordinal +','
         }
         tag = tag[0..-2]
      }
      tag += ')]]'

      def m = builder.mkp
      m.yield( tag )
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
         println "children attrs path: "+ name_constraint.children[0].rmTypeName +" "+ name_constraint.children[0].path
         println "children attrs attr: "+ name_constraint.children[0].attributes.rmAttributeName

         if (name_constraint.children[0].rmTypeName == 'DV_TEXT')
         {
            // childen[0] DV_TEXT
            //   for the DV_TEXT.value constraint
            //     the first children can be a STRING constraint
            //       check if there is a list constraint and get the first value as the name
            def value_constraint = name_constraint.children[0].attributes.find { it.rmAttributeName == 'value' }

            // there is a constraint for the name but doesnt have a specific value
            if (!value_constraint)
            {
               builder.name() {
                  value( this.opt.getTerm(parent_arch_id, o.nodeId) )
               }
            }
            else
            {
               def name_value = value_constraint.children[0].item.list[0]
               builder.name() {
                  value( name_value )
               }
            }
         }
         else // DV_CODED_TEXT
         {
            def code_phrase = name_constraint.children[0].attributes.find { it.rmAttributeName == 'defining_code' }.children[0]

            if (!code_phrase)
            {
               println "Avoid generating DV_CODED_TEXT because it has no constraints for codes or terminology"
               return
            }

            /*
             <attributes xsi:type="C_SINGLE_ATTRIBUTE">
                <rm_attribute_name>defining_code</rm_attribute_name>
                <existence>
                  ...
                </existence>
                <children xsi:type="C_CODE_PHRASE">
                  <rm_type_name>CODE_PHRASE</rm_type_name>
                  <occurrences>
                   ...
                  </occurrences>
                  <node_id />
                  <terminology_id>
                    <value>local</value>
                  </terminology_id>
                  <code_list>at1045</code_list>
                  <code_list>at1046</code_list>
                </children>
              </attributes>
             */
            builder.name('xsi:type':'DV_CODED_TEXT') {
               value( this.opt.getTerm(parent_arch_id, code_phrase.codeList[0]) )
               defining_code() { // use generate_attr_CODE_PHRASE
                  terminology_id() {
                     value(code_phrase.terminologyId)
                  }
                  code_string(code_phrase.codeList[0])
               }
            }
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
            value(this.opt.langTerminology)
         }
         code_string(this.opt.langCode)
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

   private generate_ADMIN_ENTRY(ObjectNode o, String parent_arch_id)
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
            value('[[NARRATIVE:::STRING:::ANY]]')
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
            // action_archetype_id from the OPT
            action_archetype_id( oa.children[0].item.pattern )
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

         if (!attr_ism_transition)
         {
            println "Avoid generating ism_transition for ACTION because there is no constraint for it on the OPT"
            return
         }

         // .children[0] ISM_TRANSITION
         //  .attributes current_state
         //    .children[0] DV_CODED_TEXT
         //      .attributes defining_code
         //       .children[0] CODE_PHRASE
         def code_phrase = attr_ism_transition
                             .children[0].attributes.find { it.rmAttributeName == 'current_state' }
                             .children[0].attributes.find { it.rmAttributeName == 'defining_code' }
                             .children[0]

         //println code_phrase.terminologyId // openehr
         //println code_phrase.codeList[0]   // 524

         ism_transition() {
           current_state() {
              value( terminology.getRubric(opt.langCode, code_phrase.codeList[0]) )
              defining_code() { // use generate_attr_CODE_PHRASE
                 terminology_id() {
                    value( code_phrase.terminologyId )
                 }
                 code_string( code_phrase.codeList[0] )
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
         generate_attr_DV_DATE_TIME('time') // TODO: should be tagged

         o.attributes.each { oa ->

            processAttributeChildren(oa, parent_arch_id)
         }
      }
   }

   private generate_INTERVAL_EVENT(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      AttributeNode a = o.parent

      // is it arcehtyped or not?
      def arch_node_id = (o.archetypeId ?: o.nodeId)

      builder."${a.rmAttributeName}"(archetype_node_id:arch_node_id, 'xsi:type': 'INTERVAL_EVENT') { // dont use EVENT because is abstract

         name() {
            value( opt.getTerm(parent_arch_id, o.nodeId) )
         }
         // IM attribute not present in the OPT
         generate_attr_DV_DATE_TIME('time') // TODO: should be tagged


         // data
         def oa = o.attributes.find { it.rmAttributeName == 'data' }
         if (oa) processAttributeChildren(oa, parent_arch_id)

         /* attributes include math_function from the OPT, that is generated below
         o.attributes.each { oa ->

            processAttributeChildren(oa, parent_arch_id)
         }
         */

         builder.width() { // duration attribute
            value('PT30M') // TODO: Duration String generator
         }

         oa = o.attributes.find { it.rmAttributeName == 'math_function' }
         if (oa)
         {
            //processAttributeChildren(oa, parent_arch_id)

            // ==================================
            // Quick fix for issue on DV_CODED_TEXT because of the missing attr name on the generated tag
            def oao = oa.children[0]
            def def_code = oao.attributes.find { it.rmAttributeName == 'defining_code' }
            def first_code = def_code.children[0].codeList[0] // can be null if there are no code constraints in the OPT
            def name = this.terminology.getRubric(opt.langCode, first_code)
            builder."${oa.rmAttributeName}"('xsi:type':'DV_CODED_TEXT') {
               value( name )
               defining_code {
                  terminology_id {
                     value('openehr')
                  }
                  code_string(first_code)
               }
            }
            // /quick fix
            // ==================================
         }
         else
         {
            println "Interval event math function constraint not found, generating one"
            builder.math_function() { // coded text attribute
               value("maximum")
               defining_code {
                  terminology_id {
                     value('openehr')
                  }
                  code_string('144')
               }
            }
         }
      }
   }

   private generate_POINT_EVENT(ObjectNode o, String parent_arch_id)
   {
      generate_EVENT(o, parent_arch_id)
   }

   // these are not different than ITEM_TREE processing since it is generic
   private generate_ITEM_SINGLE(ObjectNode o, String parent_arch_id)
   {
      generate_ITEM_TREE(o, parent_arch_id)
   }
   private generate_ITEM_TABLE(ObjectNode o, String parent_arch_id)
   {
      generate_ITEM_TREE(o, parent_arch_id)
   }
   private generate_ITEM_LIST(ObjectNode o, String parent_arch_id)
   {
      generate_ITEM_TREE(o, parent_arch_id)
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

   def methodMissing(String name, args)
   {
      // Intercept method that starts with find.
      if (name.startsWith("generate_"))
      {
          println name - "generate_" + " is not supported yet"
      }
      else
      {
         throw new MissingMethodException(name, this.class, args)
      }
   }

   private String label(ObjectNode o, String parent_arch_id)
   {
      // value nodes dont have nodeId
      def nodeId = o.nodeId
      if (!nodeId)
      {
         // .parent es attributes 'value', .parent.parent es 'ELEMENT'
         nodeId = o.parent.parent.nodeId
      }

      // avoid spaces in the label becaus it is used as input name in the committer
      def label = opt.getTerm(parent_arch_id, nodeId) //.replaceAll(' ', '_')

      // remove spaces and parenthesis (this mades the data binding not to work because
      // parenthesis affect the reges used for data binding in the committer)
      def replacement = {
         if ([' ' as char, '(' as char, ')' as char, '.' as char].contains(it))
         {
            '_'
         }
         // Do not transform
         else {
             null
         }
      }

      label = label.collectReplacements(replacement)

      // return unique labels
      if (field_names.contains(label))
      {
         label += '_'+ counter
         counter++
      }

      field_names << label

      return label
   }
}
