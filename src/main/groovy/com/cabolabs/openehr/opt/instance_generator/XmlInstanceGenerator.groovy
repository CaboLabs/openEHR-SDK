package com.cabolabs.openehr.opt.instance_generator

import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.terminology.TerminologyParser
import groovy.xml.MarkupBuilder
import java.text.SimpleDateFormat

import java.util.jar.JarFile

/**
 * Based on EMRApp xml.XmlSerializer
 * @author Pablo Pazos <pablo.pazos@cabolabs.com>
 * Design note: the xsi attributes should appear first in the attribute list of each element
 */
class XmlInstanceGenerator {

   static String PS = File.separator

   def opt
   def writer
   def builder

   def terminology

   // Formats: https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html
   String datetime_format = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" // https://www.w3.org/TR/xmlschema-2/#dateTime
   String date_format = "yyyy-MM-dd"
   String time_format = "HH:mm:ss"

   Random random_gen = new Random()

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

   def participations = [
      [name: 'Alexandra Alamo', function: 'legal guardian consent author', relationship: [rubric:'mother', code:'10']],
      [name: 'Betty Bix', function: 'companion', relationship: [rubric:'sister', code:'24']],
      [name: 'Charles Connor', function: 'legal guardian consent author', relationship: [rubric:'father', code:'9']],
      [name: 'Daniel Duncan', function: 'companion', relationship: [rubric:'bother', code:'23']]
   ]

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
         def format_oehr = new SimpleDateFormat(datetime_format)
         return format_oehr.format(delegate) // string, delegate is the Date instance
      }

      Date.metaClass.toOpenEHRDate = {
         def format_oehr = new SimpleDateFormat(date_format)
         return format_oehr.format(delegate) // string, delegate is the Date instance
      }

      Date.metaClass.toOpenEHRTime = {
         def format_oehr = new SimpleDateFormat(time_format)
         return format_oehr.format(delegate) // string, delegate is the Date instance
      }

      // ---------------------------------------------------------------------------------

      terminology = TerminologyParser.getInstance()

      // TODO: move code to a terminology manager
      // web environment?
      /*
      def terminology_repo_path = "resources"+ PS +"terminology"+ PS
      def terminology_repo = new File(terminology_repo_path)
      if (!terminology_repo.exists()) // try to load from resources
      {
         //def folder_path = Holders.grailsApplication.parentContext.getResource("resources"+ PS +"terminology"+ PS).getLocation().getPath()
         //println "Terminology not found in file system"

         // absolute route to the JAR File
         //println new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath())

         def jar = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath())
         if (jar.isFile())
         {
            def real_jar_file = new JarFile(jar)
            def entries = real_jar_file.entries()
            def e, is
            while (entries.hasMoreElements())
            {
               e = entries.nextElement()
               if (!e.isDirectory() && e.name.startsWith(terminology_repo_path))
               {
                  println e.name
                  is = real_jar_file.getInputStream(e)
                  this.terminology.parseTerms(is) // This is loading every XML in the folder!
               }
            }
            real_jar_file.close()
         }
      }
      else
      {
         terminology.parseTerms(new File("resources"+ PS +"terminology"+ PS +"openehr_terminology_en.xml"))
         terminology.parseTerms(new File("resources"+ PS +"terminology"+ PS +"openehr_terminology_es.xml"))
         terminology.parseTerms(new File("resources"+ PS +"terminology"+ PS +"openehr_terminology_pt.xml"))
      }
      */
      terminology.parseTerms(getClass().getResourceAsStream(PS +"terminology"+ PS +"openehr_terminology_en.xml")) // this works to load the resource from the jar
      terminology.parseTerms(getClass().getResourceAsStream(PS +"terminology"+ PS +"openehr_terminology_es.xml"))
      terminology.parseTerms(getClass().getResourceAsStream(PS +"terminology"+ PS +"openehr_terminology_pt.xml"))
   }

   /**
    * generates vresion with composition.
    */
   String generateXMLVersionStringFromOPT(OperationalTemplate opt, boolean addParticipations = false)
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
             value( String.uuid() )
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
                     value( String.uuid() )
                  }
                  namespace('DEMOGRAPHIC')
                  type('PERSON')
               }
               name( composition_composers.pick() )
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
               value( new Date().toOpenEHRDateTime() )
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
            value( String.uuid() +'::EMR_APP::1' )
         }

         data('xsi:type': 'COMPOSITION', archetype_node_id: opt.definition.archetypeId) {

            generateCompositionHeader(addParticipations) // name, language, territory, ...
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
   String generateXMLCompositionStringFromOPT(OperationalTemplate opt, boolean addParticipations = false)
   {
      this.opt = opt

      builder.composition(xmlns:'http://schemas.openehr.org/v1',
         'xmlns:xsi':'http://www.w3.org/2001/XMLSchema-instance',
         'xsi:type':'COMPOSITION',
         archetype_node_id: opt.definition.archetypeId)
      {
         generateCompositionHeader(addParticipations) // name, language, territory, ...
         generateCompositionContent(opt.definition.archetypeId)
      }

      return writer.toString()
   }

   private generateCompositionHeader(boolean addParticipations = false)
   {
      // FIXME: this should only generate what doesnt comes from the OPT (category and context are in the OPT!)

      // Campos heredados de LOCATABLE
      add_LOCATABLE_elements(opt.definition, opt.definition.archetypeId, true)

      // Campos de COMPOSITION
      builder.language() {
         terminology_id() {
            value(this.opt.langTerminology)
         }
         code_string(this.opt.langCode)
      }
      builder.territory() {
         terminology_id() {
            value('ISO_3166-1')
         }
         code_string('UY') // TODO: deberia salir de una config global o de parametros
      }

      // path is to attr, codeList is in the node
      def category_code = opt.getNodes('/category/defining_code')[0].codeList[0]

      def _value = terminology.getRubric(opt.langCode, category_code)

      // fallback to 'en' if the code was not found for the language
      if (!_value) _value = terminology.getRubric('en', category_code)

      builder.category() {
         value(_value)
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
               value( String.uuid() )
            }
            namespace('DEMOGRAPHIC')
            type('PERSON')
         }
         name( composition_composers.pick() )
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
         
         builder.context() {
            start_time() {
               value( new Date().toOpenEHRDateTime() )
            }
            setting() {
               value(setting_entry.value)
               defining_code() {
                  terminology_id() {
                     value('openehr') // all openehr terminology should be handled from this.terminology
                  }
                  code_string(setting_entry.key.toString())
               }
            }

            // other_context
            if (context)
            {
               def other_context = context.children[0].attributes.find{ it.rmAttributeName == 'other_context' }
               if (other_context)
               {
                  processAttributeChildren(other_context, opt.definition.archetypeId)
               }
            }

            // health_care_facility

            // participations
            if (addParticipations)
            {
               def participation = participations[Math.abs(new Random().nextInt() % participations.size())]

               participations() {
                  function('xsi:type': 'DV_TEXT') {
                     value(participation.function) // HL7v3:ParticipationFunction https://www.hl7.org/fhir/v3/ParticipationFunction/cs.html
                  }
                  performer('xsi:type':'PARTY_RELATED') { // TODO: random P_RELATED or P_IDENTIFIED
                     name(participation.name)
                     relationship() { // Only for P_RELATED, coded text
                        value(participation.relationship.rubric)
                        defining_code() {
                           terminology_id() {
                              value('openehr')
                           }
                           code_string(participation.relationship.code)
                        }
                     }
                  }
                  mode() {
                     value('not specified')
                     defining_code() {
                        terminology_id() {
                           value('openehr')
                        }
                        code_string('193')
                     }
                  }
               }
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
   private add_LOCATABLE_elements(ObjectNode o, String parent_arch_id, boolean add_archetype_details = false)
   {
      def name_constraint = o.attributes.find { it.rmAttributeName == 'name' }
      if (name_constraint)
      {
         // Check for prmitive constraint over DV_TEXT.value, that is CString.
         // In our model this is just another ObjectNode, just for reference:
         // https://github.com/openEHR/java-libs/blob/master/openehr-aom/src/main/java/org/openehr/am/archetype/constraintmodel/primitive/CString.java
         //println "NAME CONSTRAINT: " + name_constraint +" "+ parent_arch_id + o.path

         def name_constraint_type = name_constraint.children[0].rmTypeName

         if (name_constraint_type == 'DV_TEXT')
         {
            // childen[0] DV_TEXT
            //   for the DV_TEXT.value constraint
            //     the first children can be a STRING constraint
            //       check if there is a list constraint and get the first value as the name
            def value_constraint = name_constraint.children[0].attributes.find { it.rmAttributeName == 'value' }

            // there is a constraint for the name but doesnt have a specific value
            if (!value_constraint)
            {
               builder.name('xsi:type':'DV_TEXT') {
                  value( this.opt.getTerm(parent_arch_id, o.nodeId) )
               }
            }
            else
            {
               def name_value = value_constraint.children[0].item.list[0]
               builder.name('xsi:type':'DV_TEXT') {
                  value( name_value )
               }
            }

            // TODO: call generate_DV_TEXT
         }
         else if (name_constraint_type == 'DV_CODED_TEXT')
         {
            generate_DV_CODED_TEXT(name_constraint.children[0], parent_arch_id)
         }
      }
      else // just add the name based on the archetype ontology terms
      {
         builder.name('xsi:type':'DV_TEXT') {
            value( this.opt.getTerm(parent_arch_id, o.nodeId) )
         }

         // TODO: call generate_DV_TEXT
      }

      if (add_archetype_details)
      {
         builder.archetype_details() { // ARCHETYPED
            archetype_id() { // ARCHETYPE_ID
               value(o.archetypeId)
            }
            template_id() { // TEMPLATE_ID
               value(opt.templateId)
            }
            rm_version('1.0.2')
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

         // generate_DV_INTERVAL<DV_COUNT> => generate_DV_INTERVAL__DV_COUNT
         obj_type = obj_type.replace('<','__').replace('>','')

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

      def def_code = o.attributes.find { it.rmAttributeName == 'defining_code' }
      def first_code, terminology
      if (def_code)
      {
         first_code = def_code.children[0].codeList[0] // can be null if there are no code constraints in the OPT
         terminology = def_code.children[0].terminologyId
      }

      if (!terminology)
      {
         // format terminology:LOINC?subset=laboratory_services
         def externalTerminologyRef
         if (def_code) externalTerminologyRef = def_code.children[0].terminologyRef
         if (!externalTerminologyRef)
         {
            terminology = "terminology_not_specified_as_constraint_or_referenceSetUri_in_opt"
         }
         else
         {
            terminology = externalTerminologyRef.split("\\?")[0].split(":")[1]
         }
      }


      // random name data bu default
      def _value = String.random( (('A'..'Z')+('a'..'z')+' ,.').join(), 30 )

      if (!first_code)
      {
         first_code = Integer.random(10000, 1000000)
      }
      else
      {
         // get value from archetype ontology
         if (terminology == 'local')
         {
            _value = this.opt.getTerm(parent_arch_id, first_code)
         }
         // get value form openehr terminology
         else if (terminology == 'openehr')
         {
            _value = this.terminology.getRubric(opt.langCode, first_code)

            // fallback to english
            if (!_value) _value = this.terminology.getRubric('en', first_code)
         }
      }



      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_CODED_TEXT') {
         value(_value)
         defining_code {
            terminology_id {
               value(terminology)
            }
            code_string(first_code)
         }
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
   private generate_attr_DV_DATE_TIME(String attr, boolean addType = true)
   {
      /*
      <attr xsi:type="DV_DATE_TIME">
         <value>20150515T183951,000-0300</value>
      </attr>
      */
      if (addType)
      {
         builder."${attr}"('xsi:type':'DV_DATE_TIME') {
            value( new Date().toOpenEHRDateTime() )
         }
      }
      else
      {
         builder."${attr}"() {
            value( new Date().toOpenEHRDateTime() )
         }
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
   private generate_DV_TIME(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_TIME">
         <value>053442,950</value>
      </value>
      */
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_TIME') {
         value( new Date().toOpenEHRTime() )
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

      def _magnitude, lo, hi
      def c_magnitude = o.attributes.find { it.rmAttributeName == 'magnitude' }
      if (c_magnitude)
      {
         //println c_magnitude.children[0].item.getClass() //children 0 is primitive, .item is CInteger
         //println c_magnitude.children[0].item.list
         //println c_magnitude.children[0].item.range

         def primitive = c_magnitude.children[0].item

         if (primitive.range)
         {
            lo = ((primitive.range.lowerUnbounded) ? 0 : primitive.range.lower)
            hi = ((primitive.range.upperUnbounded) ? 100 : primitive.range.upper)

            if (!primitive.range.lowerIncluded) lo++
            if (!primitive.range.upperIncluded) hi--

            _magnitude = new Random().nextInt(hi - lo) + lo // random between lo .. hi
         }
         else
         {
            _magnitude = primitive.list[0]
         }
      }
      else // no constraints
      {
         _magnitude = Integer.random(10, 1)
      }

      builder."${a.rmAttributeName}"('xsi:type':'DV_COUNT') {
         magnitude(_magnitude)
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
         value(true) // TODO: check constraint
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

      def inputStream = getClass().getResourceAsStream(PS +"images"+ PS +"cabolabs_logo.png")
      def bytes = inputStream.bytes
      def _datab64 = bytes.encodeBase64().toString()

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
         value('20170629')
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

      def attr_numerator = o.attributes.find { it.rmAttributeName == 'numerator' }
      def attr_denominator = o.attributes.find { it.rmAttributeName == 'denominator' }
      def attr_type = o.attributes.find { it.rmAttributeName == 'type' }

      def num_hi, num_lo, den_hi, den_lo, _type

      if (attr_numerator)
      {
         // TODO: refactor to generate_REAL
         //println attr_numerator.children[0].item // CReal
         def num_constraint = attr_numerator.children[0].item.range
         
         num_lo = (num_constraint.lowerUnbounded ?    0.0f : num_constraint.lower)
         num_hi = (num_constraint.upperUnbounded ? 1000.0f : num_constraint.upper)

         // check the upper and lower included
         if (!num_constraint.lowerIncluded) num_lo++ // it would be enough to add 0.1
         if (!num_constraint.upperIncluded) num_hi--
      }
      else
      {
         num_hi = 1000.0f
         num_lo = 0.0f
      }

      if (attr_denominator)
      {
         // TODO: refactor to generate_REAL
         def den_constraint = attr_denominator.children[0].item.range
         
         den_lo = (den_constraint.lowerUnbounded ?    0.0f : den_constraint.lower)
         den_hi = (den_constraint.upperUnbounded ? 1000.0f : den_constraint.upper)

         // check the upper and lower included
         if (!den_constraint.lowerIncluded) den_lo++ // it would be enough to add 0.1
         if (!den_constraint.upperIncluded) den_hi--
      }
      else
      {
         den_hi = 1000.0f
         den_lo = 0.0f
      }

      if (attr_type)
      {
         // CPrimitive . CInteger
         _type = attr_type.children[0]?.item?.list[0]
         if (!_type) _type = 0 // ratio
      }
      else
      {
         _type = 0 // ratio
      }

      def _numerator, _denominator
      switch (_type)
      {
         case 1: // unitary
            _numerator = (random_gen.nextFloat() * (num_hi - num_lo) + num_lo).round(1)
            _denominator = 1
         break
         case 2: // percent
            _numerator = (random_gen.nextFloat() * (num_hi - num_lo) + num_lo).round(1)
            _denominator = 100
         break
         case [3,4]: // fraction, integer_fraction
            _numerator = (random_gen.nextFloat() * (num_hi - num_lo) + num_lo).round(0)
            _denominator = (random_gen.nextFloat() * (den_hi - den_lo) + den_lo).round(0)
         break
         default:
            _numerator = (random_gen.nextFloat() * (num_hi - num_lo) + num_lo).round(1)
            _denominator = (random_gen.nextFloat() * (den_hi - den_lo) + den_lo).round(1)
      }

      builder."${a.rmAttributeName}"('xsi:type':'DV_PROPORTION') {
         numerator(_numerator)
         denominator(_denominator)
         type(_type)
         //precision('-1') // -1 implies no limit, i.e. any number of decimal places.
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
      def constraint = o.list[0]
      def lo, hi
      def _units
      if (!constraint)
      {
         lo = 0.0f
         hi = 1000.0f
         _units = "_no_constraint_defined_"
      }
      else
      {
         if (!constraint.magnitude)
         {
            lo = 0.0f
            hi = 1000.0f
         }
         else
         {
            // TODO: generate rantom floats!
            lo = (constraint.magnitude.lowerUnbounded ?    0.0f : constraint.magnitude.lower)
            hi = (constraint.magnitude.upperUnbounded ? 1000.0f : constraint.magnitude.upper)
         }

         if (!constraint.units) _units = "_no_constraint_defined_"
         else _units = constraint.units
      }

      AttributeNode a = o.parent
      Random rand = new Random()

      builder."${a.rmAttributeName}"('xsi:type':'DV_QUANTITY') {
         magnitude((rand.nextFloat() * (hi - lo) + lo).round(1)) //Integer.random(hi, lo) ) // TODO: should be BigDecinal not just Integer
         units(_units)
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

      def c_value = o.attributes.find{ it.rmAttributeName == 'value' }

      if (c_value && c_value.children[0].item && c_value.children[0].item instanceof com.cabolabs.openehr.opt.model.primitive.CDuration)
      {
         def c_duration = c_value.children[0].item
         if (c_duration.pattern)
         {
            builder."${a.rmAttributeName}"('xsi:type':'DV_DURATION') {
               value(DataGenerator.duration_value_from_pattern(c_duration.pattern))
            }
         }
         else if (c_duration.range)
         {
            builder."${a.rmAttributeName}"('xsi:type':'DV_DURATION') {
               value(DataGenerator.duration_in_interval(c_duration.range))
            }
         }
      }
      else
      {
         // No constraint, return hardcoded
         builder."${a.rmAttributeName}"('xsi:type':'DV_DURATION') {
            value('PT30M')
         }
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
      def identifier = [
         issuer: 'Hospital de Clinicas',
         assigner: 'Hospital de Clinicas',
         id: String.randomNumeric(8),
         type: 'LOCALID'
      ]

      // since all constraints are the same, we do this dynamically

      def attrs = ['issuer', 'assigner', 'type', 'id']

      attrs.each { attr ->

         def c_attr = o.attributes.find{ it.rmAttributeName == attr }

         if (c_attr && c_attr.children && c_attr.children[0].item instanceof com.cabolabs.openehr.opt.model.primitive.CString)
         {
            if (c_attr.children[0].item.pattern) identifier."${attr}" = c_attr.children[0].item.pattern
            else if (c_attr.children[0].item.list)
            {
               identifier."${attr}" = c_attr.children[0].item.list[0]
            }
         }
      }

      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_IDENTIFIER') {
         issuer  (identifier.issuer)
         assigner(identifier.assigner)
         id      (identifier.id)
         type    (identifier.type)
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

      if (o.list) // list <CDvOrdinalItem>
      {
         /*
         println o.list[0].value
         println o.list[0].symbol.codeString // CodePhrase
         println o.list[0].symbol.terminologyId
         */

         def _value = ''

         if (o.list[0].symbol.terminologyId == 'local')
         {
            _value = opt.getTerm(parent_arch_id, o.list[0].symbol.codeString)
         }
         else
         {
            _value = String.random(('A'..'Z').join(), 15)
         }

         builder."${a.rmAttributeName}"('xsi:type':'DV_ORDINAL') {
            value(o.list[0].value)
            symbol() {
               value(_value)
               defining_code() {
                  terminology_id() {
                     value(o.list[0].symbol.terminologyId)
                  }
                  code_string(o.list[0].symbol.codeString)
               }
            }
         }
      }
      else // no constraints in the ordinal, values should be autogenerated
      {
         builder."${a.rmAttributeName}"('xsi:type':'DV_ORDINAL') {
            value(1)
            symbol() {
               value("autogenerated (node doesn't have a constraint)")
               defining_code() {
                  terminology_id() {
                     value('com.cabolabs.openehr.opt')
                  }
                  code_string(String.random(('A'..'Z').join(), 10))
               }
            }
         }
      }
   }

   private generate_DV_URI(ObjectNode o, String parent_arch_id)
   {
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_URI') {
         value('https://cabolabs.com')
      }
   }

   private generate_DV_EHR_URI(ObjectNode o, String parent_arch_id)
   {
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_EHR_URI') {
         value('ehr://cabolabs.com')
      }
   }


   /**
    * helper to add language, encoding and subject to ENTRY nodes.
    */
   private add_ENTRY_elements(ObjectNode o, String parent_arch_id)
   {
      add_LOCATABLE_elements(o, parent_arch_id, o.type == 'C_ARCHETYPE_ROOT')

      builder.language() {
         terminology_id() {
            value(this.opt.langTerminology)
         }
         code_string(this.opt.langCode)
      }
      builder.encoding() {
         terminology_id() {
            value('IANA_character-sets')
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

      // only if the aom type is ARCHETYPE_ROOT, the arch_node_id is archetypeId, else should be nodeId
      def arch_node_id = (o.archetypeId ?: o.nodeId)

      builder."${a.rmAttributeName}"('xsi:type': o.rmTypeName, archetype_node_id: arch_node_id) {
         name('xsi:type':'DV_TEXT') {
            value( opt.getTerm(parent_arch_id, o.nodeId) )
         }

         oa = o.attributes.find { it.rmAttributeName == 'items' }

         // FIXME: it is possible the cardinality upper is lower than the items generated because there are more alternatives
         //        defined than the upper, here we cut the elements to the upper, this check should be on any collection attribute:
         //        COMPOSITION.content, SECTION.items, HISTORY.events, ITEM_TREE.items, CLUSTER.items, etc.
         if (oa) processAttributeChildren(oa, parent_arch_id)
      }
   }

   private generate_OBSERVATION(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type': o.rmTypeName, archetype_node_id: o.archetypeId) {

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
      builder."${a.rmAttributeName}"('xsi:type': o.rmTypeName, archetype_node_id: o.archetypeId) {

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
      builder."${a.rmAttributeName}"('xsi:type': o.rmTypeName, archetype_node_id: o.archetypeId) {

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
      builder."${a.rmAttributeName}"('xsi:type': o.rmTypeName, archetype_node_id: o.archetypeId) {

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

         name('xsi:type':'DV_TEXT') {
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
      builder."${a.rmAttributeName}"('xsi:type': o.rmTypeName, archetype_node_id: o.archetypeId) {

         add_ENTRY_elements(o, parent_arch_id)


         // ACTION.time (not in the OPT, is an IM attribute)
         generate_attr_DV_DATE_TIME('time', false)

         // description
         def oa = o.attributes.find { it.rmAttributeName == 'description' }
         if (oa) processAttributeChildren(oa, parent_arch_id)


         // add one of the ism_transition in the OPT
         def attr_ism_transition = o.attributes.find { it.rmAttributeName == 'ism_transition' }
         def code_phrase

         if (!attr_ism_transition)
         {
            // create dummy ism_transition data if there is no definition in the OPT
            code_phrase = [
               codeList: ['526'],
               terminologyId: 'openehr'
            ]
         }
         else
         {
            // check for missing info
            if (!attr_ism_transition.children[0] ||
               !attr_ism_transition.children[0].attributes ||
               !attr_ism_transition.children[0].attributes.find { it.rmAttributeName == 'current_state' } ||
               !attr_ism_transition.children[0].attributes.find { it.rmAttributeName == 'current_state' }.children[0] ||
               !attr_ism_transition.children[0].attributes.find { it.rmAttributeName == 'current_state' }.children[0].attributes ||
               !attr_ism_transition.children[0].attributes.find { it.rmAttributeName == 'current_state' }.children[0].attributes.find { it.rmAttributeName == 'defining_code' } ||
               !attr_ism_transition.children[0].attributes.find { it.rmAttributeName == 'current_state' }.children[0].attributes.find { it.rmAttributeName == 'defining_code' }.children[0]
               )
            {
               // create dummy ism_transition data if there is no complete definition in the OPT
               code_phrase = [
                  codeList: ['526'],
                  terminologyId: 'openehr'
               ]
            }
            else
            {
               // .children[0] ISM_TRANSITION
               //  .attributes current_state
               //    .children[0] DV_CODED_TEXT
               //      .attributes defining_code
               //       .children[0] CODE_PHRASE
               code_phrase = attr_ism_transition
                                 .children[0].attributes.find { it.rmAttributeName == 'current_state' }
                                 .children[0].attributes.find { it.rmAttributeName == 'defining_code' }
                                 .children[0]
            }
         }

         def _value = terminology.getRubric(opt.langCode, code_phrase.codeList[0])

         // fallback to english
         if (!_value) _value = terminology.getRubric('en', code_phrase.codeList[0])

         ism_transition() {
           current_state() {
              value( _value )
              defining_code() { // use generate_attr_CODE_PHRASE
                 terminology_id() {
                    value(code_phrase.terminologyId)
                 }
                 code_string(code_phrase.codeList[0])
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

      builder."${a.rmAttributeName}"(archetype_node_id:arch_node_id) {

         name('xsi:type':'DV_TEXT') {
            value( opt.getTerm(parent_arch_id, o.nodeId) )
         }
         // IM attribute not present in the OPT
         generate_attr_DV_DATE_TIME('origin', false)

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

      builder."${a.rmAttributeName}"('xsi:type': 'POINT_EVENT', archetype_node_id:arch_node_id) { // dont use EVENT because is abstract

         name('xsi:type':'DV_TEXT') {
            value( opt.getTerm(parent_arch_id, o.nodeId) )
         }
         // IM attribute not present in the OPT
         generate_attr_DV_DATE_TIME('time', false)

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

      builder."${a.rmAttributeName}"('xsi:type': 'INTERVAL_EVENT', archetype_node_id:arch_node_id) { // dont use EVENT because is abstract

         name('xsi:type':'DV_TEXT') {
            value( opt.getTerm(parent_arch_id, o.nodeId) )
         }
         // IM attribute not present in the OPT
         generate_attr_DV_DATE_TIME('time', false)


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
            processAttributeChildren(oa, parent_arch_id)
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

      builder."${a.rmAttributeName}"('xsi:type': o.rmTypeName, archetype_node_id:arch_node_id) {

         add_LOCATABLE_elements(o, parent_arch_id, o.type == 'C_ARCHETYPE_ROOT')

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

      builder."${a.rmAttributeName}"('xsi:type': o.rmTypeName, archetype_node_id:arch_node_id) {

         add_LOCATABLE_elements(o, parent_arch_id, o.type == 'C_ARCHETYPE_ROOT')

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

      builder."${a.rmAttributeName}"('xsi:type': o.rmTypeName, archetype_node_id:arch_node_id) {

         add_LOCATABLE_elements(o, parent_arch_id, o.type == 'C_ARCHETYPE_ROOT')

         o.attributes.each { oa ->
            if (oa.rmAttributeName == 'name') return // avoid processing name constraints, thos are processde by add_LOCATABLE_elements
            processAttributeChildren(oa, parent_arch_id)
         }
      }
   }

   private generate_DV_INTERVAL__DV_COUNT(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_INTERVAL"><!-- note specific type is not valid here: DV_INERVAL<DV_COUNT> doesn't exist in the XSD -->
         <lower xsi:type="DV_COUNT">
           <magnitude>123</magnitude>
         </lower>
         <upper xsi:type="DV_COUNT">
           <magnitude>234</magnitude>
         </upper>
         <lower_unbounded>false</lower_unbounded>
         <upper_unbounded>false</upper_unbounded>
      </value>
      */
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_INTERVAL') {

         // Need to ask for the attributes explicitly since order matters for the XSD

         def lower = o.attributes.find { it.rmAttributeName == 'lower' }
         builder."${lower.rmAttributeName}"('xsi:type':'DV_COUNT') {
            magnitude( Integer.random(10, 1) ) // TODO: consider constraints
         }

         def upper = o.attributes.find { it.rmAttributeName == 'upper' }
         builder."${upper.rmAttributeName}"('xsi:type':'DV_COUNT') {
            magnitude( Integer.random(100, 10) ) // TODO: consider constraints
         }

         // lower_unbounded and upper_unbounded are required
         // lower_unbounded: no constraint is defined for upper or lower.lower is not defined
         // upper_unbounded: no constraint is defined for upper or upper.upper is not defined

         def ccount
         def attr_magnitude
         def cprimitive
         def cint

         if (!lower)
         {
            builder.lower_unbounded(true)
         }
         else
         {
            ccount = lower.children[0]
            if (!ccount)
            {
               builder.lower_unbounded(true)
            }
            else
            {
               attr_magnitude = ccount.attributes[0]
               if (!attr_magnitude)
               {
                  builder.lower_unbounded(true)
               }
               else
               {
                  cprimitive = attr_magnitude.children[0]
                  cint = cprimitive.item

                  if (cint.range && !cint.range.lowerUnbounded)
                  {
                     builder.lower_unbounded(false)
                  }
                  else
                  {
                     builder.lower_unbounded(true)
                  }
               }
            }
         }


         if (!upper)
         {
            builder.upper_unbounded(true)
         }
         else
         {
            ccount = upper.children[0]
            if (!ccount)
            {
               builder.upper_unbounded(true)
            }
            else
            {
               attr_magnitude = ccount.attributes[0]
               if (!attr_magnitude)
               {
                  builder.upper_unbounded(true)
               }
               else
               {
                  cprimitive = attr_magnitude.children[0]
                  cint = cprimitive.item

                  if (cint.range && !cint.range.upperUnbounded)
                  {
                     builder.upper_unbounded(false)
                  }
                  else
                  {
                     builder.upper_unbounded(true)
                  }
               }
            }
         }
      }
   }

   private generate_DV_INTERVAL__DV_QUANTITY(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_INTERVAL"><!-- note specific type is not valid here: DV_INERVAL<DV_COUNT> doesn't exist in the XSD -->
      <lower xsi:type="DV_QUANTITY">
         <magnitude>123.123</magnitude>
         <units>mm[H20]</units>
      </lower>
      <upper xsi:type="DV_QUANTITY">
         <magnitude>234.234</magnitude>
         <units>mm[H20]</units>
      </upper>
      <lower_unbounded>false</lower_unbounded>
      <upper_unbounded>false</upper_unbounded>
      </value>
      */

      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_INTERVAL') {

         def lower = o.attributes.find { it.rmAttributeName == 'lower' }
         generate_DV_QUANTITY(lower.children[0], parent_arch_id)
         /*
         builder.lower('xsi:type':'DV_QUANTITY') {
            magnitude('[[lower.magnitude:::DV_QUANTITY_MAGNITUDE]]')
            units('[[lower.units:::DV_QUANTITY_UNITS]]')
         }
         */

         def upper = o.attributes.find { it.rmAttributeName == 'upper' }
         generate_DV_QUANTITY(upper.children[0], parent_arch_id)
         /*
         builder.upper('xsi:type':'DV_QUANTITY') {
            magnitude('[[upper.magnitude:::DV_QUANTITY_MAGNITUDE]]')
            units('[[upper.units:::DV_QUANTITY_UNITS]]')
         }
         */

         // lower_unbounded and upper_unbounded are required
         // for tagged DV_INTERVALs the only way to check this,
         // since the boundaries depend on the constraint for a
         // specific unit, is to check if all units have min or max
         // boundaries, if all have, that will bounded, if some don't
         // have, that will be unbounded.
         // Also, if there are no constraints (empty list), both limits
         // will be unbounde

         // lower
         def cqty = lower.children[0] // CDvQuantity

         if (!cqty.list)
         {
            builder.lower_unbounded(true)
         }
         else
         {
            // if one lower limit is unbounded, then the tagged will be unbounded
            def lowerUnbounded = false
            cqty.list.each { cqitem->
               if (cqitem.magnitude.lowerUnbounded)
               {
                  lowerUnbounded = true
               }
            }
            builder.lower_unbounded(lowerUnbounded)
         }

         // upper
         cqty = upper.children[0]

         if (!cqty.list)
         {
            builder.upper_unbounded(true)
         }
         else
         {
            // if one upper limit is unbounded, then the tagged will be unbounded
            def upperUnbounded = false
            cqty.list.each { cqitem->
               if (cqitem.magnitude.upperUnbounded)
               {
                  upperUnbounded = true
               }
            }
            builder.upper_unbounded(upperUnbounded)
         }
      }
   }

   private generate_DV_INTERVAL__DV_DATE_TIME(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_INTERVAL"><!-- note specific type is not valid here: DV_INERVAL<DV_COUNT> doesn't exist in the XSD -->
         <lower xsi:type="DV_DATE_TIME">
           <value>20190114T183649,426+0000</value>
         </lower>
         <upper xsi:type="DV_DATE_TIME">
           <value>20190114T183649,426+0000</value>
         </upper>
         <lower_unbounded>false</lower_unbounded>
         <upper_unbounded>false</upper_unbounded>
      </value>
      */
      AttributeNode a = o.parent
      builder."${a.rmAttributeName}"('xsi:type':'DV_INTERVAL') {

         def lower = o.attributes.find { it.rmAttributeName == 'lower' }
         generate_attr_DV_DATE_TIME(lower.rmAttributeName)

         def upper = o.attributes.find { it.rmAttributeName == 'upper' }
         generate_attr_DV_DATE_TIME(upper.rmAttributeName)

         // there are no constraints for date time to establish unbounded,
         // so it is always unbounded for both limits.
         builder.lower_unbounded(true)
         builder.upper_unbounded(true)
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
}
