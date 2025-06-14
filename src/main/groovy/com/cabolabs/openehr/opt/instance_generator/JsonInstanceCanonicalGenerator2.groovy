package com.cabolabs.openehr.opt.instance_generator

import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.terminology.TerminologyParser
import java.text.SimpleDateFormat
import groovy.json.*
import com.cabolabs.openehr.opt.model.primitive.*
import java.util.jar.JarFile

/**
 * @author Pablo Pazos <pablo.pazos@cabolabs.com>
 *
 */
class JsonInstanceCanonicalGenerator2 {

   static String PS = File.separator

   def opt
   def builder
   def out

   def terminology

   Random random_gen = new Random() // TODO: use this one for all generations

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

   def JsonInstanceCanonicalGenerator2(
      String datetime_format = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
      String date_format = "yyyy-MM-dd",
      String time_format = "HH:mm:ss")
   {
      /* THIS CANT BE USED UNTIL Groovy 2.5.x, since Grails 3.3.10 uses 2.4.17 we keep building under that version
         OLD javadocs by Groovy version
      // https://mrhaki.blogspot.com/2018/06/groovy-goodness-customizing-json-output.html
      // https://docs.groovy-lang.org/latest/html/gapi/groovy/json/JsonGenerator.Options.html
      def options = new JsonGenerator.Options()
        .excludeNulls()
        .disableUnicodeEscaping()
        .build()

      builder = new JsonBuilder(options)
      */

      //builder = new JsonBuilder()

      out = [:]

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
         new Random( System.currentTimeMillis() ).nextInt( max - from + 1 ) + from
      }

      // Double.metaClass.static.random = { double max, double from ->
      //    new Random( System.currentTimeMillis() ).nextDouble() * (max - from ) + from
      // }

      BigDecimal.metaClass.static.random = { BigDecimal max, BigDecimal from ->
         BigDecimal randomBigDecimal = from.add(new BigDecimal(Math.random()).multiply(max.subtract(from)))
         return randomBigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP)
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

      // FIXME: load all resources in folder
      // https://www.logicbig.com/how-to/java/list-all-files-in-resouce-folder.html
      terminology.parseTerms(getClass().getResourceAsStream("/terminology/openehr_terminology_en.xml")) // this works to load the resource from the jar
      terminology.parseTerms(getClass().getResourceAsStream("/terminology/openehr_terminology_es.xml"))
      terminology.parseTerms(getClass().getResourceAsStream("/terminology/openehr_terminology_pt.xml"))
   }

   /**
    * generates vresion with composition.
    */
   String generateJSONVersionStringFromOPT(OperationalTemplate opt, boolean addParticipations = false, boolean prettyOutput = false)
   {
      this.opt = opt

      out = [
         _type: 'ORIGINAL_VERSION',
         contribution: [
            id: [
               _type: 'HIER_OBJECT_ID',
               value: String.uuid()
            ],
            namespace: 'EHR::COMMON',
            type: 'CONTRIBUTION'
         ],
         commit_audit: [
            _type: 'AUDIT_DETAILS', // TODO: support ATTESTATION
            system_id: 'CABOLABS_EHRSERVER', // TODO: make it configurable
            time_committed: [
               value: new Date().toOpenEHRDateTime()
            ],
            change_type: [
               value: 'creation',
               defining_code: [
                  terminology_id: [
                     value: 'openehr'
                  ],
                  code_string: '249'
               ]
            ],
            committer: [
               _type: 'PARTY_IDENTIFIED',
               external_ref: [
                  id: [
                     _type: 'HIER_OBJECT_ID',
                     value: String.uuid()
                  ],
                  namespace: 'DEMOGRAPHIC',
                  type: 'PERSON'
               ],
               name: composition_composers.pick()
            ]
         ],
         uid: [
            _type: 'OBJECT_VERSION_ID',
            value: String.uuid() +'::EMR_APP::1'
         ],
         data: generateComposition(addParticipations),
         lifecycle_state: [
            value: 'complete',
            defining_code: [
               terminology_id: [
                  value: 'openehr'
               ],
               code_string: '532'
            ]
         ]
      ]

      if (prettyOutput)
      {
         //StringEscapeUtils.unescapeJavaScript( // avoid unicode escaping
           JsonOutput.prettyPrint(JsonOutput.toJson(out))
         //)
      }
      else
      {
         //StringEscapeUtils.unescapeJavaScript( // avoid unicode escaping
            JsonOutput.toJson(out)
         //)
      }
   }


   /**
    * generates just the composition, no version info
    */
   String generateJSONCompositionStringFromOPT(OperationalTemplate opt, boolean addParticipations = false, boolean prettyOutput = false)
   {
      this.opt = opt

      out = generateComposition(addParticipations)

      if (prettyOutput)
      {
         //StringEscapeUtils.unescapeJavaScript( // avoid unicode escaping
           JsonOutput.prettyPrint(JsonOutput.toJson(out))
         //)
      }
      else
      {
         //StringEscapeUtils.unescapeJavaScript( // avoid unicode escaping
            JsonOutput.toJson(out)
         //)
      }
   }

   Map generateComposition(boolean addParticipations = false)
   {
      def compo = add_LOCATABLE_elements(opt.definition, opt.definition.archetypeId, true)

      compo.language = [
         terminology_id: [
            value: opt.langTerminology
         ],
         code_string: opt.langCode
      ]

      compo.territory = [
         terminology_id: [
            value: 'ISO_3166-1'
         ],
         code_string: 'UY' // TODO: get from configuration file
      ]

      // path is to attr, codeList is in the node
      def category_code = opt.getNodes('/category/defining_code')[0].codeList[0]

      def value = terminology.getRubric(opt.langCode, category_code)

      compo.category = [
         value: value,
         defining_code: [
            terminology_id: [
               value: 'openehr'
            ],
            code_string: category_code
         ]
      ]

      compo.composer = [
         _type: 'PARTY_IDENTIFIED',
         external_ref: [
            id: [
               _type: 'HIER_OBJECT_ID',
               value: String.uuid()
            ],
            namespace: 'DEMOGRAPHIC',
            type: 'PERSON'
         ],
         name: composition_composers.pick()
         // identifiers DV_IDENTIFIER
      ]


      // context is declared on the OPT only if it contains constraints for other_context
      def attr_context = opt.definition.attributes.find{ it.rmAttributeName == 'context' }

      // TODO: we might need to relax this
      if (category_code == '431' && attr_context)
      {
         throw new Exception("Error: COMPOSITION is persistent but contains context.")
      }


      if (category_code == '433') // event
      {
         def setting_entry
         if (!composition_settings[this.opt.langCode]) setting_entry = composition_settings['en'].pick()
         else setting_entry = composition_settings[this.opt.langCode].pick()

         compo.context = [
            start_time: [
               value: new Date().toOpenEHRDateTime()
            ],
            setting: [
               value: setting_entry.value,
               defining_code: [
                  terminology_id: [
                     value: 'openehr' // all openehr terminology should be handled from this.terminology
                  ],
                  code_string: setting_entry.key.toString()
               ]
            ]
         ]
         // health_care_facility
         // participations

         if (addParticipations)
         {
            def participation = participations[Math.abs(new Random().nextInt() % participations.size())]

            compo.context.participations = [
               [
                  function: [
                     value: participation.function // HL7v3:ParticipationFunction https://www.hl7.org/fhir/v3/ParticipationFunction/cs.html
                  ],
                  performer: [
                     _type: 'PARTY_RELATED', // TODO: random P_RELATED or P_IDENTIFIED
                     name: participation.name,
                     relationship: [ // Only for P_RELATED, coded text
                        value: participation.relationship.rubric,
                        defining_code: [
                           terminology_id: [
                              value: 'openehr'
                           ],
                           code_string: participation.relationship.code
                        ]
                     ]
                  ],
                  mode: [
                     value: 'not specified', // TODO: rangom modes
                     defining_code: [
                        terminology_id: [
                           value: 'openehr'
                        ],
                        code_string: '193'
                     ]
                  ]
               ]
            ]
         }

         if (attr_context)
         {
            def other_context = attr_context.children[0].attributes.find{ it.rmAttributeName == 'other_context' }
            if (other_context)
            {
               this.processAttributeChildren(other_context, opt.definition.archetypeId)
            }
         }
      } // if event



      // content

      // opt.definition.attributes has attributes category, context and content of the COMPOSITION
      // category and context where already processed on generateCompositionHeader
      def oa = opt.definition.attributes.find{ it.rmAttributeName == 'content' }

      if (!oa && !attr_context) throw new Exception("The OPT doesn't have a structure for COMPOSITION.content or COMPOSITION.context, at least it should have one of those")

      if (oa)
      {
         def content = processAttributeChildren(oa, opt.definition.archetypeId)

         // it is possible the cardinality upper is lower than the items generated because there are more alternatives
         // defined than the upper, here we cut the elements to the upper, this check should be on any collection attribute
         if (oa.cardinality && oa.cardinality.interval.upper)
         {
            content = content.take(oa.cardinality.interval.upper)
         }

         compo.content = content
      }

      return compo
   }


   // DEMOGRAPHICS
   Map generatePerson()
   {
      def mobj = add_ACTOR_elements(opt.definition, opt.definition.archetypeId)

      return mobj
   }

   Map generateOrganization()
   {
      def mobj = add_ACTOR_elements(opt.definition, opt.definition.archetypeId)

      return mobj
   }

   Map generateGroup()
   {
      def mobj = add_ACTOR_elements(opt.definition, opt.definition.archetypeId)

      return mobj
   }

   Map generateAgent()
   {
      def mobj = add_ACTOR_elements(opt.definition, opt.definition.archetypeId)

      return mobj
   }

   Map generateRole()
   {
      def mobj = add_PARTY_elements(opt.definition, opt.definition.archetypeId)

      def oa = o.attributes.find{ it.rmAttributeName == 'capabilities' }

      if (oa)
      {
         def capabilities = processAttributeChildren(oa, parent_arch_id)

         // it is possible the cardinality upper is lower than the items generated because there are more alternatives
         // defined than the upper, here we cut the elements to the upper, this check should be on any collection attribute
         if (oa.cardinality && oa.cardinality.interval.upper)
         {
            capabilities = capabilities.take(oa.cardinality.interval.upper)
         }

         mobj.capabilities = capabilities
      }

      // TODO: if there are constraints for time_validity, consider those

      return mobj
   }

   private add_ACTOR_elements(ObjectNode o, String parent_arch_id)
   {
      def mobj = add_PARTY_elements(o, parent_arch_id)

      // TODO: if there are constraints for languages, consider those

      return mobj
   }

   private add_PARTY_elements(ObjectNode o, String parent_arch_id)
   {
      def mobj = add_LOCATABLE_elements(opt.definition, opt.definition.archetypeId, o.type == 'C_ARCHETYPE_ROOT')

      def oa = opt.definition.attributes.find{ it.rmAttributeName == 'contacts' }

      if (oa)
      {
         def contacts = processAttributeChildren(oa, opt.definition.archetypeId)

         // it is possible the cardinality upper is lower than the items generated because there are more alternatives
         // defined than the upper, here we cut the elements to the upper, this check should be on any collection attribute
         if (oa.cardinality && oa.cardinality.interval.upper)
         {
            contacts = contacts.take(oa.cardinality.interval.upper)
         }

         mobj.contacts = contacts
      }

      oa = opt.definition.attributes.find{ it.rmAttributeName == 'identities' }

      if (oa)
      {
         def identities = processAttributeChildren(oa, opt.definition.archetypeId)

         // it is possible the cardinality upper is lower than the items generated because there are more alternatives
         // defined than the upper, here we cut the elements to the upper, this check should be on any collection attribute
         if (oa.cardinality && oa.cardinality.interval.upper)
         {
            identities = identities.take(oa.cardinality.interval.upper)
         }

         mobj.identities = identities
      }

      oa = o.attributes.find{ it.rmAttributeName == 'details' }

      if (oa)
      {
         def details = processAttributeChildren(oa, parent_arch_id)

         mobj.details = details
      }

      return mobj
   }

   private generate_CONTACT(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      def mobj = add_LOCATABLE_elements(o, parent_arch_id, o.type == 'C_ARCHETYPE_ROOT')

      def oa = o.attributes.find{ it.rmAttributeName == 'addresses' }

      if (oa)
      {
         def addresses = processAttributeChildren(oa, parent_arch_id)

         // it is possible the cardinality upper is lower than the items generated because there are more alternatives
         // defined than the upper, here we cut the elements to the upper, this check should be on any collection attribute
         if (oa.cardinality && oa.cardinality.interval.upper)
         {
            addresses = addresses.take(oa.cardinality.interval.upper)
         }

         mobj.addresses = addresses
      }
      // TODO: if there is no constraint, there should be a list with one item generated since it's [1..*] in the RM

      // TODO: consider any constraint over CONTACT.time_validity

      return mobj
   }

   private generate_ADDRESS(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      def mobj = add_LOCATABLE_elements(o, parent_arch_id, o.type == 'C_ARCHETYPE_ROOT')

      def oa = o.attributes.find{ it.rmAttributeName == 'details' }

      if (oa)
      {
         def details = processAttributeChildren(oa, parent_arch_id)

         mobj.details = details
      }
      // TODO: if there is no constraint, there should be an ITEM_STRUCTURE generated since it's [1] in the RM

      return mobj
   }

   private generate_PARTY_IDENTITY(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      def mobj = add_LOCATABLE_elements(o, parent_arch_id, o.type == 'C_ARCHETYPE_ROOT')

      def oa = o.attributes.find{ it.rmAttributeName == 'details' }

      if (oa)
      {
         def details = processAttributeChildren(oa, parent_arch_id)

         mobj.details = details
      }
      // TODO: if there is no constraint, there should be an ITEM_STRUCTURE generated since it's [1] in the RM

      return mobj
   }

   private generate_CAPABILITY(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      def mobj = add_LOCATABLE_elements(o, parent_arch_id, o.type == 'C_ARCHETYPE_ROOT')

      def oa = o.attributes.find{ it.rmAttributeName == 'credentials' }

      if (oa)
      {
         def credentials = processAttributeChildren(oa, parent_arch_id)

         mobj.credentials = credentials
      }
      // TODO: if there is no constraint, there should be an ITEM_STRUCTURE generated since it's [1] in the RM

      // TODO: consider if there is a constraint for time_validity

      return mobj
   }


   /**
    * Continues the opt recursive traverse.
    */
   List processAttributeChildren(AttributeNode a, String parent_arch_id)
   {
      //println "processAttributeChildren"

      // some cases might not have a constraint of an attribute, just checking that
      if (!a)
      {
         return
      }

      List attrs = []

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
            //builder.mkp.comment('SLOT IN '+ obj.path +' NOT PROCESSED')
            return
         }

         // wont process all the alternatives from children, just the first
         obj_type = obj.rmTypeName

         // generate_DV_INTERVAL<DV_COUNT> => generate_DV_INTERVAL__DV_COUNT
         obj_type = obj_type.replace('<','__').replace('>','')

         method = 'generate_'+ obj_type

         attrs << "$method"(obj, parent_arch_id) // generate_OBSERVATION(a)
      }

      return attrs
   }


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

      def coded = [:]


      def def_code = o.attributes.find { it.rmAttributeName == 'defining_code' }
      def first_code, terminology_id
      if (def_code)
      {
         first_code = def_code.children[0].codeList[0] // can be null if there are no code constraints in the OPT
         terminology_id = def_code.children[0].terminologyId

         // println def_code.children
         // println def_code.children[0] // CCodePhrase
         // println def_code.children[0].terminologyId
         // println def_code.children[0].codeList
      }

      if (!terminology_id)
      {
         // format terminology:LOINC?subset=laboratory_services
         def externalTerminologyRef
         if (def_code) externalTerminologyRef = def_code.children[0].terminologyRef

         if (!externalTerminologyRef)
         {
            terminology_id = "terminology_not_specified_as_constraint_or_referenceSetUri_in_opt"
         }
         else
         {
            terminology_id = externalTerminologyRef.split("\\?")[0].split(":")[1]
         }
      }

      // random value data by default
      def value = String.random( (('A'..'Z')+('a'..'z')+' ,.').join(), 30 )

      // println "terminology ${terminology_id}"
      // println "first_code ${first_code}"

      if (!first_code)
      {
         first_code = Integer.random(1000000, 1000).toString()
      }
      else
      {
         // get value from archetype ontology
         if (terminology_id == 'local')
         {
            value = this.opt.getTerm(parent_arch_id, first_code)
         }
         // get value form openehr terminology
         else if (terminology_id == 'openehr')
         {
            value = this.terminology.getRubric(opt.langCode, first_code)

            // fallback to 'en' if the code was not found for the language
            if (!value) value = terminology.getRubric('en', first_code)
         }
      }

      //println "value ${value}"

      [
         _type: 'DV_CODED_TEXT',
         value: value,
         defining_code: [
            terminology_id: [
               value: terminology_id
            ],
            code_string: first_code
         ]
      ]
   }

   private generate_attr_CODE_PHRASE(String attr, String terminology, String code)
   {
      [
         "${attr}": [
            terminology_id: [
               value: terminology
            ],
            code_string: code
         ]
      ]
   }

   private generate_DV_TEXT(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_TEXT">
        <value>....</value>
      </value>
      */
      AttributeNode a = o.parent

      [
         _type: 'DV_TEXT',
         value: String.random( (('A'..'Z')+('a'..'z')+' ,.').join(), 300 ) // TODO: improve using word / phrase dictionary
      ]
   }

   private generate_DV_DATE_TIME(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_DATE_TIME">
         <value>20150515T183951,000-0300</value>
      </value>
      */
      AttributeNode a = o.parent
      //generate_attr_DV_DATE_TIME(a.rmAttributeName)
      [
         _type: 'DV_DATE_TIME',
         value: new Date().toOpenEHRDateTime()
      ]
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
      [
         (attr): [
            _type: 'DV_DATE_TIME',
            value: new Date().toOpenEHRDateTime()
         ]
      ]
   }

   private generate_DV_DATE(ObjectNode o, String parent_arch_id)
   {
      /*
      <value xsi:type="DV_DATE">
         <value>20150515</value>
      </value>
      */
      AttributeNode a = o.parent
      /* [
         "${a.rmAttributeName}": [
            _type: 'DV_DATE',
            value: new Date().toOpenEHRDate()
         ]
      ] */
      [
         _type: 'DV_DATE',
         value: new Date().toOpenEHRDate()
      ]
   }
   private generate_DV_TIME(ObjectNode o, String parent_arch_id)
   {
      AttributeNode a = o.parent

      [
         _type: 'DV_TIME',
         value: new Date().toOpenEHRTime()
      ]
   }

   private generate_DV_COUNT(ObjectNode o, String parent_arch_id)
   {
      def magnitude
      def c_magnitude = o.attributes.find { it.rmAttributeName == 'magnitude' }
      if (c_magnitude)
      {
         //println c_magnitude.children[0].item.getClass() //children 0 is primitive, .item is CInteger
         //println c_magnitude.children[0].item.list
         //println c_magnitude.children[0].item.range

         def primitive = c_magnitude.children[0].item

         magnitude = generate_CInteger(primitive)
      }
      else // no constraints
      {
         magnitude = Integer.random(10, 1)
      }

      return [
         _type: 'DV_COUNT',
         magnitude: magnitude
      ]
   }

   // generates a value depending on the primitive constraint
   // TODO: this value generators can be refactored between the JSON and XML generators
   private Integer generate_CInteger(CInteger primitive)
   {
      Integer _magnitude, lo, hi

      if (primitive.range)
      {
         lo = ((primitive.range.lowerUnbounded) ? 0 : primitive.range.lower)
         hi = ((primitive.range.upperUnbounded) ? 100 : primitive.range.upper)

         if (!primitive.range.lowerIncluded) lo++
         if (!primitive.range.upperIncluded) hi--

         // NOTE: same as Integer.random(hi, lo)
         _magnitude = new Random().nextInt(hi - lo) + lo // random between lo .. hi
      }
      else if (primitive.list)
      {
         _magnitude = primitive.list[0]
      }
      else // no constraints
      {
         _magnitude = Integer.random(10, 1)
      }

      return _magnitude
   }

   private generate_DV_BOOLEAN(ObjectNode o, String parent_arch_id)
   {
      [
         _type: 'DV_BOOLEAN',
         value: true // TODO: check constraint
      ]
   }

   private generate_DV_MULTIMEDIA(ObjectNode o, String parent_arch_id)
   {
      /*
      def _dataf, _datab64

      // web environment?
      def img_repo_path = "resources"+ PS +"images"+ PS
      def img_repo = new File(img_repo_path)
      if (!img_repo.exists()) // try to load from resources
      {
         def jar = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath())
         if (jar.isFile())
         {
            def real_jar_file = new JarFile(jar)
            def entries = real_jar_file.entries()
            def e, is
            while (entries.hasMoreElements())
            {
               e = entries.nextElement()
               if (e.name.startsWith(img_repo_path))
               {
                  //println e.name
                  is = real_jar_file.getInputStream(e)
                  _datab64 = is.text.bytes.encodeBase64().toString()
               }
            }
            real_jar_file.close()
         }
      }
      else
      {
         _dataf = new File("resources"+ PS +"images"+ PS +"cabolabs_logo.png")
         _datab64 = _dataf.bytes.encodeBase64().toString()
      }
      */

      def inputStream = getClass().getResourceAsStream("/images/cabolabs_logo.png")
      def bytes = inputStream.bytes
      def _datab64 = bytes.encodeBase64().toString()

      Map mtype = generate_attr_CODE_PHRASE('media_type', 'IANA_media-types', 'image/png') // TODO: grab the terminology from the ObjectNode
      Map mmcontent = [
         _type: 'DV_MULTIMEDIA',
         data: _datab64,
         size: _datab64.size()
      ] + mtype

      return mmcontent
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

      def out = [
         _type: 'DV_PARSABLE',
         // TODO: the value should depend on the formalism, we need to have generators to common formalisms
         value: '20170629',
         formalism: 'ISO8601'
      ]

      def c_formalism = o.attributes.find{ it.rmAttributeName == 'formalism' }
      if (c_formalism)
      {
         def primitive = c_formalism.children[0]
         if (primitive)
         {
            if (primitive.item instanceof com.cabolabs.openehr.opt.model.primitive.CString)
            {
               if (primitive.item.list)
               {
                  out.formalism = primitive.item.list[0]
               }
            }
         }
      }

      return out
   }

   private generate_DV_PROPORTION(ObjectNode o, String parent_arch_id)
   {
      def attr_numerator = o.attributes.find { it.rmAttributeName == 'numerator' }
      def attr_denominator = o.attributes.find { it.rmAttributeName == 'denominator' }
      def attr_type = o.attributes.find { it.rmAttributeName == 'type' }

      def num_hi, num_lo, den_hi, den_lo, type

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
         type = attr_type.children[0]?.item?.list[0]
         if (!type) type = 0 // ratio
      }
      else
      {
         type = 0 // ratio
      }

      def _numerator, _denominator
      switch (type)
      {
         case 1: // unitary
            _numerator = BigDecimal.random(new BigDecimal(num_hi), new BigDecimal(num_lo))
            //_numerator = (random_gen.nextFloat() * (num_hi - num_lo) + num_lo).round(1)
            _denominator = 1
         break
         case 2: // percent
            _numerator = BigDecimal.random(new BigDecimal(num_hi), new BigDecimal(num_lo))
            //_numerator = (random_gen.nextFloat() * (num_hi - num_lo) + num_lo).round(1)
            _denominator = 100
         break
         case [3,4]: // fraction, integer_fraction
            _numerator = BigDecimal.random(new BigDecimal(num_hi), new BigDecimal(num_lo)).setScale(0, BigDecimal.ROUND_HALF_UP)
            //_numerator = (random_gen.nextFloat() * (num_hi - num_lo) + num_lo).round(0)
            _denominator = BigDecimal.random(new BigDecimal(den_hi), new BigDecimal(den_lo)).setScale(0, BigDecimal.ROUND_HALF_UP)
            //_denominator = (random_gen.nextFloat() * (den_hi - den_lo) + den_lo).round(0)
         break
         default:
            _numerator = BigDecimal.random(new BigDecimal(num_hi), new BigDecimal(num_lo))
            //_numerator = (random_gen.nextFloat() * (num_hi - num_lo) + num_lo).round(1)
            _denominator = BigDecimal.random(new BigDecimal(den_hi), new BigDecimal(den_lo))
            //_denominator = (random_gen.nextFloat() * (den_hi - den_lo) + den_lo).round(1)
      }

      [
         _type: 'DV_PROPORTION',
         // TODO: consider proportion type from OPT to generate valid values, hardcoded for now.
         numerator: _numerator,
         denominator: _denominator,
         type: type
         //,
         //precision: '-1' // -1 implies no limit, i.e. any number of decimal places.
      ]
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
            lo = (constraint.magnitude.lowerUnbounded ?    0.0f : constraint.magnitude.lower)
            hi = (constraint.magnitude.upperUnbounded ? 1000.0f : constraint.magnitude.upper)

            // check the upper and lower included
            if (!constraint.magnitude.lowerIncluded) lo++
            if (!constraint.magnitude.upperIncluded) hi--
         }

         if (!constraint.units) _units = "_no_constraint_defined_"
         else _units = constraint.units
      }

      AttributeNode a = o.parent

      [
         _type: 'DV_QUANTITY',
         //magnitude: (rand.nextFloat() * (hi - lo) + lo).round(1), // TODO: take the precision from the OPT
         magnitude: BigDecimal.random(new BigDecimal(hi), new BigDecimal(lo)),
         units: _units
      ]
   }

   private generate_DV_DURATION(ObjectNode o, String parent_arch_id)
   {
      //println "DURATION "+ o.getClass()

      def c_value = o.attributes.find{ it.rmAttributeName == 'value' }

      if (c_value && c_value.children[0].item && c_value.children[0].item instanceof com.cabolabs.openehr.opt.model.primitive.CDuration)
      {
         def c_duration = c_value.children[0].item
         if (c_duration.pattern)
         {
            //println c_duration.pattern // PDTMS
            return [
               _type: 'DV_DURATION',
               value: DataGenerator.duration_value_from_pattern(c_duration.pattern)
            ]
         }

         if (c_duration.range)
         {
            return [
               _type: 'DV_DURATION',
               value: DataGenerator.duration_in_interval(c_duration.range)
            ]
         }
      }

      // No constraint, return hardcoded
      [
         _type: 'DV_DURATION',
         value: 'PT30M'
      ]
   }

   private generate_DV_IDENTIFIER(ObjectNode o, String parent_arch_id)
   {
      def identifier = [
         _type: 'DV_IDENTIFIER',
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

      return identifier
   }

   private generate_DV_ORDINAL(ObjectNode o, String parent_arch_id)
   {
      // o is CDvOrdinal
      if (o.list) // list <CDvOrdinalItem>
      {
         /*
         println o.list[0].value
         println o.list[0].symbol.codeString // CodePhrase
         println o.list[0].symbol.terminologyId
         */

         def value = ''

         if (o.list[0].symbol.terminologyId == 'local')
         {
            value = opt.getTerm(parent_arch_id, o.list[0].symbol.codeString)
         }
         else
         {
            value = String.random(('A'..'Z').join(), 15)
         }

         return [
            _type: 'DV_ORDINAL',
            value: o.list[0].value,
            symbol: [
               value: value,
               defining_code: [
                  terminology_id: [
                     value: o.list[0].symbol.terminologyId
                  ],
                  code_string: o.list[0].symbol.codeString
               ]
            ]
         ]
      }

      // There is no constraint for the ordinal, so the defining_code is generated
      [
         _type: 'DV_ORDINAL',
         value: 1, // TODO: take the ordinal value from the ObjectNode
         symbol: [
            value: "autogenerated (node doesn't have a constraint)",
            defining_code: [
               terminology_id: [
                  value: 'com.cabolabs.openehr.opt'
               ],
               code_string: String.random(('A'..'Z').join(), 10)
            ]
         ]
      ]
   }

   private generate_DV_URI(ObjectNode o, String parent_arch_id)
   {
      AttributeNode a = o.parent
      [
         _type: 'DV_URI',
         value: 'https://cabolabs.com' // TODO: consider the constraints to generate the URI
      ]
   }

   private generate_DV_EHR_URI(ObjectNode o, String parent_arch_id)
   {
      AttributeNode a = o.parent
      [
         _type: 'DV_EHR_URI',
         value: 'ehr://cabolabs.com' // TODO: consider the constraints to generate the URI
      ]
   }

   /**
    * /DATATYPES -----------------------------------------------------------------------------------------------
    */

   /**
    * helper to add name, also checks if the object has a constraint for the name.
    */
   private add_LOCATABLE_elements(ObjectNode o, String parent_arch_id, boolean add_archetype_details = false)
   {
      // LOCATABLE
      // - _type
      // - name
      // - archetype_node_id
      // - uid
      // ....
      // https://github.com/openEHR/specifications-ITS-JSON/blob/master/components/RM/Release-1.0.4/Common/LOCATABLE.json

      // JSON _type
      def node_type = o.rmTypeName

      // prevent using abstract type
      if (node_type == 'EVENT') node_type = 'POINT_EVENT'


      def locatable = [:]

      locatable._type = node_type

      // name
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
            // children[0] DV_TEXT
            //   for the DV_TEXT.value constraint
            //     the first children can be a STRING constraint
            //       check if there is a list constraint and get the first value as the name
            def value_constraint = name_constraint.children[0].attributes.find { it.rmAttributeName == 'value' }

            // there is a constraint for the name but doesnt have a specific value
            if (!value_constraint)
            {
               locatable.name = [
                  _type: 'DV_TEXT',
                  value: this.opt.getTerm(parent_arch_id, o.nodeId)
               ]
            }
            else
            {
               def name_value = value_constraint.children[0].item.list[0] // TEST: attr value can be null?
               locatable.name = [
                  _type: 'DV_TEXT',
                  value: name_value
               ]
            }

            // TODO: call generate_DV_TEXT
         }
         else if (name_constraint_type == 'DV_CODED_TEXT')
         {
            locatable.name = generate_DV_CODED_TEXT(name_constraint.children[0], parent_arch_id)
         }
      }
      else // just add the name based on the archetype ontology terms
      {
         locatable.name = [
            _type: 'DV_TEXT',
            value: this.opt.getTerm(parent_arch_id, o.nodeId)
         ]

         // TODO: call generate_DV_TEXT
      }

      if (add_archetype_details)
      {
         locatable.archetype_details = [
            archetype_id: [
               value: o.archetypeId //opt.definition.archetypeId
            ],
            template_id: [
               value: opt.templateId
            ],
            rm_version: '1.0.2' // TODO: make the RM version configurable
         ]
      }

      // archetype_node_id
      def arch_node_id = (o.archetypeId ?: o.nodeId)

      locatable.archetype_node_id = arch_node_id

      return locatable
   }

   /**
    * helper to add language, encoding and subject to ENTRY nodes.
    */
   private add_ENTRY_elements(ObjectNode o, String parent_arch_id)
   {
      //println "add_ENTRY_elements "+ o.type

      def mobj = add_LOCATABLE_elements(o, parent_arch_id, o.type == 'C_ARCHETYPE_ROOT')

      mobj.language = [
         terminology_id: [
            value: this.opt.langTerminology
         ],
         code_string: this.opt.langCode
      ]

      mobj.encoding = [
         terminology_id: [
            value: 'IANA_character-sets'
         ],
         code_string: 'UTF-8' // TODO: deberia salir de una config global
      ]

      // TODO: party proxy / party self generator
      mobj.subject = [
         _type: 'PARTY_SELF'
      ]

      // ENTRY.protocol (only for CARE_ENTRY not for ADMIN_ENTRY)
      def oa = o.attributes.find { it.rmAttributeName == 'protocol' }
      if (oa)
      {
         // returns a list, take the first obj
         def protocol = processAttributeChildren(oa, parent_arch_id)
         mobj.protocol = protocol[0]
      }

      return mobj
   }


   private generate_SECTION(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id
      def oa
      AttributeNode a = o.parent

      def mobj = add_LOCATABLE_elements(o, parent_arch_id, o.type == 'C_ARCHETYPE_ROOT')

      oa = o.attributes.find { it.rmAttributeName == 'items' }
      if (oa)
      {
         def items = processAttributeChildren(oa, parent_arch_id)

         // it is possible the cardinality upper is lower than the items generated because there are more alternatives
         // defined than the upper, here we cut the elements to the upper, this check should be on any collection attribute
         if (oa.cardinality && oa.cardinality.interval.upper)
         {
            items = items.take(oa.cardinality.interval.upper)
         }

         mobj.items = items
      }

      return mobj
   }

   private generate_OBSERVATION(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      def mobj = add_ENTRY_elements(o, parent_arch_id) // adds LOCATABLE fields

      AttributeNode a = o.parent

      def oa = o.attributes.find { it.rmAttributeName == 'data' }
      if (oa)
      {
         // the result is a list but should contain only one HISTORY, get the first item
         def data = processAttributeChildren(oa, parent_arch_id)
         //println "OBSERVATION data "+ data
         mobj.data = data[0]
      }

      return mobj
   }

   private generate_EVALUATION(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      def mobj = add_ENTRY_elements(o, parent_arch_id) // adds LOCATABLE fields

      AttributeNode a = o.parent

      def oa = o.attributes.find { it.rmAttributeName == 'data' }
      if (oa)
      {
         // this is a list, but data is a single structure, extract the first item
         def data = processAttributeChildren(oa, parent_arch_id)
         mobj.data = data[0]
      }

      return mobj
   }

   private generate_ADMIN_ENTRY(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      def mobj = add_ENTRY_elements(o, parent_arch_id) // adds LOCATABLE fields

      def oa = o.attributes.find { it.rmAttributeName == 'data' }
      if (oa)
      {
         // this is a list, but data is a single structure, extract the first item
         def data = processAttributeChildren(oa, parent_arch_id)
         mobj.data = data[0]
      }

      return mobj
   }


   private generate_INSTRUCTION(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      def mobj = add_ENTRY_elements(o, parent_arch_id) // adds LOCATABLE fields

      AttributeNode a = o.parent

      // DV_TEXT narrative (not in the OPT, is an IM attribute)
      mobj.narrative = [
         value: String.random( (('A'..'Z')+('a'..'z')+' ,.').join(), 255 )
      ]

      def oa = o.attributes.find { it.rmAttributeName == 'activities' }
      if (oa)
      {
         // this is a list, and activities is also a list, we use the full list
         def activities = processAttributeChildren(oa, parent_arch_id)
         mobj.activities = activities
      }

      return mobj
   }

   private generate_ACTIVITY(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      AttributeNode a = o.parent

      def mobj = add_LOCATABLE_elements(o, parent_arch_id)

      def oa = o.attributes.find { it.rmAttributeName == 'description' }
      if (oa)
      {
         // this is a list, description is a single attr, we use the first item
         def description = processAttributeChildren(oa, parent_arch_id)
         mobj.description = description[0]
      }

      // DV_PARSABLE timing (IM attr, not in OPT)
      mobj.timing = [
         value: 'P1D', // TODO: duration string generator,
         formalism: 'ISO8601'
      ]

      // action_archetype_id
      // mandatory and should come from the OPT but some archetypes don't have it,
      // here I check if that exists in the OPT and if not, just generate dummy data
      oa = o.attributes.find { it.rmAttributeName == 'action_archetype_id' }
      if (oa)
      {
         // action_archetype_id from the OPT
         mobj.action_archetype_id = oa.children[0].item.pattern
      }
      else
      {
         mobj.action_archetype_id = 'openEHR-EHR-ACTION\\.sample_action\\.v1'
      }

      return mobj
   }

   private generate_ACTION(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      def mobj = add_ENTRY_elements(o, parent_arch_id) + // adds LOCATABLE fields
                 generate_attr_DV_DATE_TIME('time') // ACTION.time (not in the OPT, is an IM attribute)

      // description
      def oa = o.attributes.find { it.rmAttributeName == 'description' }
      if (oa)
      {
         def description = processAttributeChildren(oa, parent_arch_id)
         mobj.description = description[0]
      }

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

      def value = terminology.getRubric(opt.langCode, code_phrase.codeList[0])

      // fallback to 'en' if the code was not found for the language
      if (!value) value = terminology.getRubric('en', code_phrase.codeList[0])

      mobj.ism_transition = [
         current_state: [
            value: value,
            defining_code: [ // use generate_attr_CODE_PHRASE
               terminology_id: [
                  value: code_phrase.terminologyId
               ],
               code_string: code_phrase.codeList[0]
            ]
         ]
      ]

      return mobj
   }

   private generate_HISTORY(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      AttributeNode a = o.parent

      def mobj = add_LOCATABLE_elements(o, parent_arch_id) +
                 generate_attr_DV_DATE_TIME('origin') // IM attribute not present in the OPT

      /* def mattr
      o.attributes.each { oa ->
         mattr = processAttributeChildren(oa, parent_arch_id)
         mobj[oa.rmAttributeName] = mattr
      } */

      def oa = o.attributes.find { it.rmAttributeName == 'events' }
      if (oa)
      {
         def events = processAttributeChildren(oa, parent_arch_id)

         // it is possible the cardinality upper is lower than the items generated because there are more alternatives
         // defined than the upper, here we cut the elements to the upper, this check should be on any collection attribute
         if (oa.cardinality && oa.cardinality.interval.upper)
         {
            events = events.take(oa.cardinality.interval.upper)
         }

         mobj.events = events
      }

      return mobj
   }

   private generate_EVENT(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      AttributeNode a = o.parent

      def mobj = add_LOCATABLE_elements(o, parent_arch_id) +
                 generate_attr_DV_DATE_TIME('time') // IM attribute not present in the OPT

      def mattrs
      o.attributes.each { oa ->

         // in event there are no lists, so the results will be lists of 1 item
         mattrs = processAttributeChildren(oa, parent_arch_id)

         mobj[oa.rmAttributeName] = mattrs[0]
      }

      return mobj
   }

   private generate_INTERVAL_EVENT(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      AttributeNode a = o.parent

      def mobj = add_LOCATABLE_elements(o, parent_arch_id) +
                 generate_attr_DV_DATE_TIME('time') // IM attribute not present in the OPT


      // data
      def oa = o.attributes.find { it.rmAttributeName == 'data' }
      if (oa)
      {
         def data = processAttributeChildren(oa, parent_arch_id)
         mobj.data = data[0]
      }

      mobj.width = [ // duration attribute
         value: 'PT30M' // TODO: Duration String generator
      ]

      oa = o.attributes.find { it.rmAttributeName == 'math_function' }
      if (oa)
      {
         def math_function = processAttributeChildren(oa, parent_arch_id)
         mobj.math_function = math_function[0]
      }
      else
      {
         // TODO: warning log
         //println "Interval event math function constraint not found, generating one"
         mobj.math_function = [ // coded text attribute
            value: "maximum",
            defining_code: [
               terminology_id: [
                  value: 'openehr'
               ],
               code_string: '144'
            ]
         ]
      }

      return mobj
   }

   private generate_POINT_EVENT(ObjectNode o, String parent_arch_id)
   {
      generate_EVENT(o, parent_arch_id)
   }

   // these are not different than ITEM_TREE processing since it is generic
   private generate_ITEM_SINGLE(ObjectNode o, String parent_arch_id)
   {
     // generate_ITEM_TREE(o, parent_arch_id)

     // parent from now can be different than the parent if if the object has archetypeId
     parent_arch_id = o.archetypeId ?: parent_arch_id

     def mobj = add_LOCATABLE_elements(o, parent_arch_id, o.type == 'C_ARCHETYPE_ROOT')

     def oa_item = o.attributes.find{ it.rmAttributeName == 'item' }

     if (oa_item && oa_item.children)
     {
        mobj.item = generate_ELEMENT(oa_item.children[0], parent_arch_id)
     }

     return mobj
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

      def mobj = add_LOCATABLE_elements(o, parent_arch_id, o.type == 'C_ARCHETYPE_ROOT')

      def mattr
      o.attributes.each { oa ->

         mattr = processAttributeChildren(oa, parent_arch_id) // this is a list!

         // this doesn't work for item single because it's a list and should be an object,
         // but for the other structures it works OK
         mobj[oa.rmAttributeName] = mattr
      }

      return mobj
   }

   private generate_CLUSTER(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      AttributeNode a = o.parent

      def mobj = add_LOCATABLE_elements(o, parent_arch_id, o.type == 'C_ARCHETYPE_ROOT')

      def mattr

      // items is the only attribute
      /*
      o.attributes.each { oa ->
         if (oa.rmAttributeName == 'name') return // avoid processing name constraints, thos are processde by add_LOCATABLE_elements
         mattr = processAttributeChildren(oa, parent_arch_id)
         mobj[oa.rmAttributeName] = mattr
      }
      */

      // since items is a collection, it is assigned directly to the list retrieved
      def oa = o.attributes.find{ it.rmAttributeName == 'items' }
      if (oa)
      {
         mattr = processAttributeChildren(oa, parent_arch_id)

         // it is possible the cardinality upper is lower than the items generated because there are more alternatives
         // defined than the upper, here we cut the elements to the upper, this check should be on any collection attribute
         if (oa.cardinality && oa.cardinality.interval.upper)
         {
            mattr = mattr.take(oa.cardinality.interval.upper)
         }

         //println oa.cardinality.interval //.interval.upper <<<< NULL we are not parsing the cardinality

         mobj.items = mattr
      }

      // no items?
      if (!mobj.items)
      {
         // TODO: log warning about empty cluster
         // add dummy element
         // NOTE: if a dummy element is added it will fail OPT validation! (stupid idea...)
         /*
         mobj.items = [
            [
               _type: 'ELEMENT',
               archetype_node_id: 'at'+ Integer.random(9999, 1000),
               name: [
                  _type: 'DV_TEXT',
                  value: 'dummy value'
               ],
               value: [
                  _type: 'DV_TEXT',
                  value: 'dummy value'
               ]
            ]
         ]
         */
      }

      return mobj
   }

   private generate_ELEMENT(ObjectNode o, String parent_arch_id)
   {
      // parent from now can be different than the parent if if the object has archetypeId
      parent_arch_id = o.archetypeId ?: parent_arch_id

      AttributeNode a = o.parent

      def mobj = add_LOCATABLE_elements(o, parent_arch_id, o.type == 'C_ARCHETYPE_ROOT')
      def mattr

      // the element can have constraints for: name, value and null_flavour
      // the name is already considered in add_LOCATABLE_elements
      // if it has constraint for null_flavour, sometimes generate that instead of the value

      // attr_value chould be null if no constraint is defined!
      def attr_value = o.attributes.find { it.rmAttributeName == 'value' }
      def attr_null_flavour = o.attributes.find { it.rmAttributeName == 'null_flavour' }

      // 20 % of the time generate null_flavour instead of value
      if (attr_null_flavour)
      {
         if (new Random().nextInt(10) > 7) // 0..9 > 7 is 8 or 9 that is 20% of the time
         {
            // returns a list and the value is single using the first item
            mattr = processAttributeChildren(attr_null_flavour, parent_arch_id)
            mobj.null_flavour = mattr[0]
         }
         else
         {
            if (attr_value)
            {
               mattr = processAttributeChildren(attr_value, parent_arch_id)
               mobj.value = mattr[0]
            }
            else // Inv_null_flavour_indicated: is_null() xor null_flavour = Void
            {
               // TODO: generate random null_flavour
               // TODO: i18n
               mobj.null_flavour = [
                  _type: 'DV_CODED_TEXT',
                  value: 'no information',
                  defining_code: [
                     terminology_id: [
                        value: 'openehr'
                     ],
                     code_string: '271'
                  ]
               ]
            }
         }
      }
      else
      {
         if (attr_value)
         {
            mattr = processAttributeChildren(attr_value, parent_arch_id)
            mobj.value = mattr[0]
         }
         else // Inv_null_flavour_indicated: is_null() xor null_flavour = Void
         {
            // TODO: generate random null_flavour
            // TODO: i18n
            mobj.null_flavour = [
               _type: 'DV_CODED_TEXT',
               value: 'no information',
               defining_code: [
                  terminology_id: [
                     value: 'openehr'
                  ],
                  code_string: '271'
               ]
            ]
         }
      }

      return mobj
   }

   private generate_DV_INTERVAL__DV_COUNT(ObjectNode o, String parent_arch_id)
   {
       def mobj = [
         _type: 'DV_INTERVAL', // removed <DV_COUNT> generics because of https://discourse.openehr.org/t/correct-use-of-generic-types-in-xml-and-json/1504/16
         lower_included: true,
         upper_included: true,
         lower_unbounded: false,
         upper_unbounded: false
      ]

      // TODO: refactor with XmlInstanceGenerator since the code is the same
      // get constraints for DV_COUNT limits
      def lower_attr = o.attributes.find { it.rmAttributeName == 'lower' }
      def upper_attr = o.attributes.find { it.rmAttributeName == 'upper' }

      // by default no constraint, which are all the else cases for the ifs below
      def lower_constraint = 'no'
      def upper_constraint = 'no'
      def combined_constraint // this would be list_range, no_no, no_range, etc (lower_constraint +'_'+ upper_constraint)

      // when lower_constraint or upper_constraint are different than 'no', these will be not null
      def lower_primitive, upper_primitive

      if (lower_attr)
      {
         // FIXME: check children has any element, that is actually the object constraint for the DV_COUNT
         def lower_attr_magnitude = lower_attr.children[0].attributes.find { it.rmAttributeName == 'magnitude' }
         if (lower_attr_magnitude)
         {
            def lower_primitive_object = lower_attr_magnitude.children[0]
            if (lower_primitive_object)
            {
               lower_primitive = lower_primitive_object.item
               if (lower_primitive)
               {
                  if (lower_primitive.range)
                  {
                     lower_constraint = 'range'
                  }
                  else if (lower_primitive.list) // already checks for null and empty
                  {
                     lower_constraint = 'list'
                  }
               }
            }
         }
      }

      if (upper_attr)
      {
         // FIXME: check children has any element, that is actually the object constraint for the DV_COUNT
         def upper_attr_magnitude = upper_attr.children[0].attributes.find { it.rmAttributeName == 'magnitude' }
         if (upper_attr_magnitude)
         {
            def upper_primitive_object = upper_attr_magnitude.children[0]
            if (upper_primitive_object)
            {
               upper_primitive = upper_primitive_object.item
               if (upper_primitive)
               {
                  if (upper_primitive.range)
                  {
                     upper_constraint = 'range'
                  }
                  else if (upper_primitive.list) // already checks for null and empty
                  {
                     upper_constraint = 'list'
                  }
               }
            }
         }
      }

      combined_constraint = lower_constraint +'_'+ upper_constraint

      Integer lower_magnitude, upper_magnitude

      switch (combined_constraint)
      {
         case 'no_no':
            lower_magnitude = Integer.random(10, 1)
            upper_magnitude = lower_magnitude + 1 // assure lower < upper
         break
         case 'no_list':
            upper_magnitude = upper_primitive.list.sort{-it}[0] // take the biggest value
            lower_magnitude = upper_magnitude - 1
         break
         case 'no_range':
            upper_magnitude = DataGenerator.int_in_range(upper_primitive.range)
            lower_magnitude = upper_magnitude - 1
         break
         case 'list_no':
            lower_magnitude = lower_primitive.list.sort()[0] // take the lowest value
            upper_magnitude = lower_magnitude + 1
         break
         case 'list_list':
            lower_magnitude = lower_primitive.list.sort()[0] // take the lowest value
            upper_magnitude = upper_primitive.list.sort{-it}[0] // take the biggest value

            if (lower_magnitude > upper_magnitude)
            {
               throw new Exception('The template defines incompatible list constraints to lower and upper attributes of the interval')
            }
         break
         case 'list_range':
            lower_magnitude = lower_primitive.list.sort()[0] // take the lowest value

            if (upper_primitive.range.upper && lower_magnitude > upper_primitive.range.upper)
            {
               throw new Exception('The template defines incompatible list constraint for lower and range constraint for upper on an interval')
            }

            // ensures the upper generated is greater than the lower picker from the list
            def constrained_range = upper_primitive.range.clone()
            constrained_range.lower = lower_magnitude

            upper_magnitude = DataGenerator.int_in_range(constrained_range)
         break
         case 'range_no':
            lower_magnitude = DataGenerator.int_in_range(lower_primitive.range)
            upper_magnitude = lower_magnitude + 1
         break
         case 'range_list':
            upper_magnitude = upper_primitive.list.sort{-it}[0] // take the biggest value

            // These checks below ensure the constrained_range has lower <= upper

            // if lowerIncluded, range.lower should be <= upper_magnitude
            if (lower_primitive.range.lower.lowerIncluded && lower_primitive.range.lower > upper_magnitude)
            {
               throw new Exception('The template defines incompatible range constraint for lower and list constraint for upper on an interval')
            }

            // if !lowerIncluded, range.lower should be < upper_magnitude
            if (!lower_primitive.range.lower.lowerIncluded && lower_primitive.range.lower >= upper_magnitude)
            {
               throw new Exception('The template defines incompatible range constraint for lower and list constraint for upper on an interval')
            }

            // ensures the lower generated is lower than the upper picked from the list
            def constrained_range = lower_primitive.range.clone()
            constrained_range.upper = upper_magnitude
            constrained_range.upperUnbounded = false

            lower_magnitude = DataGenerator.int_in_range(constrained_range)
         break
         case 'range_range':
            // the condition for valid range_range constraints is: (considering also unbounded)
            //
            // (upper.range.upperUnbounded ||
            //  !lower.range.upperUnbounded && lower.range.upper <= upper.range.upper)
            // &&
            // (lower.range.lowerUnbounded ||
            //  !upper,range.lowerUnbounded && lower.range.lower <= upper.range.lower
            // )
            //
            //
            // if those constraints are met, picking the lowest value from lower.range and the
            // highest value from upper.range will generate valid data from the interval

            // limits would be null in the case of unbounded
            if (lower_primitive.range.lowerUnbounded)
            {
               lower_magnitude = 0
            }
            else
            {
               lower_magnitude = lower_primitive.range.lower
               if (!lower_primitive.range.lowerIncluded) lower_magnitude += 1
            }

            if (upper_primitive.range.upperUnbounded)
            {
               upper_magnitude = lower_magnitude + 1
            }
            else
            {
               upper_magnitude = upper_primitive.range.upper
               if (!upper_primitive.range.upperIncluded) upper_magnitude -= 1
            }
         break
      }

      mobj.lower = [
         _type: 'DV_COUNT',
         magnitude: lower_magnitude
      ]

      mobj.upper = [
         _type: 'DV_COUNT',
         magnitude: upper_magnitude
      ]

      return mobj
   }

   private generate_DV_INTERVAL__DV_QUANTITY(ObjectNode o, String parent_arch_id)
   {
      def mobj = [
         _type: 'DV_INTERVAL', // removed <DV_QUANTITY> generics because of https://discourse.openehr.org/t/correct-use-of-generic-types-in-xml-and-json/1504/16
         lower_included: true,
         upper_included: true,
         lower_unbounded: false,
         upper_unbounded: false
      ]

      // TODO: refactor with XmlInstanceGenerator code because it's the same
      // get constraints for DV_QUANTITY limits
      def lower_attr = o.attributes.find { it.rmAttributeName == 'lower' }
      def upper_attr = o.attributes.find { it.rmAttributeName == 'upper' }

      def lower_constraint = 'no'
      def upper_constraint = 'no'
      def combined_constraint // this would be list_range, no_no, no_range, ...

      def lower_qty_item, upper_qty_item

      // when lower_constraint or upper_constraint are different than 'no', these will be not null
      def lower_qty_magnitude_interval, upper_qty_magnitude_interval

      if (lower_attr)
      {
         def cqty_lower = lower_attr.children[0]
         if (cqty_lower && cqty_lower.list)
         {
            lower_qty_item = cqty_lower.list[0]
            if (lower_qty_item.magnitude)
            {
               lower_constraint = 'range'
               lower_qty_magnitude_interval = lower_qty_item.magnitude
            }
         }
      }

      if (upper_attr)
      {
         def cqty_upper = upper_attr.children[0]
         if (cqty_upper && cqty_upper.list)
         {
            // if there is an item selected for the lower, the upper item should have the same units
            if (lower_qty_item)
            {
               upper_qty_item = cqty_upper.list.find{ it.units == lower_qty_item.units }
               if (!upper_qty_item)
               {
                  throw new Exception('There is no constraint for upper with the same units as lower limit for DV_INTERVAL<DV_QUANTITY> in the template')
               }
            }
            else
            {
               upper_qty_item = cqty_upper.list[0]
            }

            if (upper_qty_item.magnitude)
            {
               upper_constraint = 'range'
               upper_qty_magnitude_interval = upper_qty_item.magnitude
            }
         }
      }

      combined_constraint = lower_constraint +'_'+ upper_constraint

      // FIXME: would be better to use BigDecimal
      BigDecimal lower_magnitude, upper_magnitude
      String _units = (lower_qty_item ? lower_qty_item.units : (upper_qty_item ? upper_qty_item.units : 'no_units_constraint'))

      switch (combined_constraint)
      {
         case 'no_no':
            lower_magnitude = BigDecimal.random(new BigDecimal(10.0), new BigDecimal(0.0))
            upper_magnitude = lower_magnitude + 1.0
         break
         case 'no_range':
            upper_magnitude = DataGenerator.bigdecimal_in_range(upper_qty_magnitude_interval)
            lower_magnitude = upper_magnitude - 1.0
         break
         case 'range_no':
            lower_magnitude = DataGenerator.bigdecimal_in_range(lower_qty_magnitude_interval)
            upper_magnitude = lower_magnitude + 1.0
         break
         case 'range_range':
            // the condition for valid range_range constraints is: (considering also unbounded)
            //
            // (upper.range.upperUnbounded ||
            //  !lower.range.upperUnbounded && lower.range.upper <= upper.range.upper)
            // &&
            // (lower.range.lowerUnbounded ||
            //  !upper,range.lowerUnbounded && lower.range.lower <= upper.range.lower
            // )
            //
            //
            // if those constraints are met, picking the lowest value from lower.range and the
            // highest value from upper.range will generate valid data from the interval

             // limits would be null in the case of unbounded
            if (lower_qty_magnitude_interval.lowerUnbounded)
            {
               lower_magnitude = 0.0
            }
            else
            {
               lower_magnitude = lower_qty_magnitude_interval.lower
               if (!lower_qty_magnitude_interval.lowerIncluded) lower_magnitude += 0.1
            }

            if (upper_qty_magnitude_interval.upperUnbounded)
            {
               upper_magnitude = lower_magnitude + 10.0
            }
            else
            {
               upper_magnitude = upper_qty_magnitude_interval.upper
               if (!upper_qty_magnitude_interval.upperIncluded) upper_magnitude -= 0.1
            }
         break
      }

      mobj.lower = [
         _type: 'DV_QUANTITY',
         magnitude: lower_magnitude,
         units: _units
      ]

      mobj.upper = [
         _type: 'DV_QUANTITY',
         magnitude: upper_magnitude,
         units: _units
      ]

      return mobj
   }

   private generate_DV_INTERVAL__DV_DATE_TIME(ObjectNode o, String parent_arch_id)
   {
      def mobj = [
         _type: 'DV_INTERVAL', // removed <DV_DATE_TIME> generics because of https://discourse.openehr.org/t/correct-use-of-generic-types-in-xml-and-json/1504/16
         lower_included: true,
         upper_included: true,
         lower_unbounded: false,
         upper_unbounded: false
      ]

      // TODO: refactor with XmlInstanceGEnerator since the code is the same

      // get constraints for DV_COUNT limits
      def lower_attr = o.attributes.find { it.rmAttributeName == 'lower' }
      def upper_attr = o.attributes.find { it.rmAttributeName == 'upper' }

      // by default no constraint, which are all the else cases for the ifs below
      def lower_constraint = 'no'
      def upper_constraint = 'no'
      def combined_constraint // this would be no_no, no_pattern, pattern_no, pattern_pattern, pattern comes from CDateTime.pattern

      // when lower_constraint or upper_constraint are different than 'no', these will be not null
      def lower_primitive, upper_primitive

      if (lower_attr)
      {
         def cdt_lower = lower_attr.children[0]
         if (cdt_lower)
         {
            def lower_attr_value = cdt_lower.attributes.find { it.rmAttributeName == 'value' }
            if (lower_attr_value)
            {
               def lower_primitive_object = lower_attr_value.children[0]
               if (lower_primitive_object)
               {
                  lower_primitive = lower_primitive_object.item
                  if (lower_primitive && lower_primitive.pattern)
                  {
                     lower_constraint = 'pattern'
                  }
                  // TODO: support list in CDateTime
               }
            }
         }
      }

      if (upper_attr)
      {
         def cdt_upper = upper_attr.children[0]
         if (cdt_upper)
         {
            def upper_attr_value = cdt_upper.attributes.find { it.rmAttributeName == 'value' }
            if (upper_attr_value)
            {
               def upper_primitive_object = upper_attr_value.children[0]
               if (upper_primitive_object)
               {
                  upper_primitive = upper_primitive_object.item
                  if (upper_primitive && upper_primitive.pattern)
                  {
                     upper_constraint = 'pattern'
                  }
                  // TODO: support list in CDateTime
               }
            }
         }
      }

      combined_constraint = lower_constraint +'_'+ upper_constraint

      String lower_value, upper_value

      // TODO: support partial datetimes: https://github.com/CaboLabs/openEHR-SDK/issues/126
      // Note the pattern constraints are for defining the partial parts not about the value itself
      switch (combined_constraint)
      {
         case 'no_no':
            lower_value = new Date().toOpenEHRDateTime()
            upper_value = (new Date() + 1).toOpenEHRDateTime() // +1 day
         break
         case 'no_pattern':
            lower_value = new Date().toOpenEHRDateTime()
            upper_value = (new Date() + 1).toOpenEHRDateTime() // +1 day
         break
         case 'pattern_no':
            lower_value = new Date().toOpenEHRDateTime()
            upper_value = (new Date() + 1).toOpenEHRDateTime() // +1 day
         break
         case 'pattern_pattern':
            lower_value = new Date().toOpenEHRDateTime()
            upper_value = (new Date() + 1).toOpenEHRDateTime() // +1 day
         break
      }

      mobj.lower = [
         _type: 'DV_DATE_TIME',
         value: lower_value
      ]

      mobj.upper = [
         _type: 'DV_DATE_TIME',
         value: upper_value
      ]

      return mobj
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
