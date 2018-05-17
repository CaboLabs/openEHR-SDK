package test

import groovy.util.GroovyTestCase

import com.cabolabs.openehr.opt.parser.*
import com.cabolabs.openehr.opt.ui_generator.OptUiGenerator
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.model.primitive.*
import com.cabolabs.openehr.opt.model.domain.*
import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.opt.instance_generator.*
import com.cabolabs.openehr.terminology.TerminologyParser
import com.cabolabs.openehr.opt.instance_validation.XmlInstanceValidation
import com.cabolabs.openehr.opt.instance_generator.*
import com.cabolabs.openehr.opt.serializer.*

class OPTParserTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")

   OperationalTemplate loadAndParse(String path)
   {
      def parser = new OperationalTemplateParser()

      def optFile = new File( path )
      def text = optFile.getText()

      assertNotNull(text)
      assert text != ''

      return parser.parse( text )
   }


/*
   void testAttributeParentNode()
   {
      println "====== testAttributeParentNode ======"
      def path = "resources"+ PS +"opts"+ PS + OptManager.DEFAULT_NAMESPACE + PS +"Referral.opt"
      def opt = loadAndParse(path)
      opt.definition.attributes.each { attr ->
         println attr.rmAttributeName
         assert attr.parent == opt.definition
      }
   }
*/

   void testParseNodesCDvQuantity()
   {
      println "====== testParseNodesCDvQuantity ======"
      def path = "resources"+ PS +"opts"+ PS + OptManager.DEFAULT_NAMESPACE + PS +"LabResults1.opt"
      def opt = loadAndParse(path)

      def cdi = opt.getNode('/content[archetype_id=openEHR-EHR-OBSERVATION.lab_test-full_blood_count.v1]/data[at0001]/events[at0002]/data[at0003]/items[at0078.4]/value')

      assert cdi instanceof CDvQuantity

      assert cdi.isValid('gm/l', 50.5)
      assert !cdi.isValid('qweerty', 50.5)
      assert !cdi.isValid('gm/l', -50.5)
      assert !cdi.isValid('qweerty', -50.5)

      assert cdi.isValid('qweerty', 50.5).message == 'CDvQuantity.validation.error.noMatchingUnits'

      opt.nodes.each {

         if (it.value instanceof CDvQuantity)
         {
            println it.key
            println it.value.property.codeString // TODO: get the value from the openehr terminology

            //println it.key +": "+ it.value
            it.value.list.each { qti ->
               println qti.units +" "+ qti.magnitude
            }
         }
      }
   }


   void testParseArchetypeSlot()
   {
      println "====== testParseArchetypeSlot ======"
      def path = "resources"+ PS +"opts"+ PS + OptManager.DEFAULT_NAMESPACE + PS +"Signos.opt"
      def opt = loadAndParse(path)

/*
      def c = opt.getNode('/content[archetype_id=openEHR-EHR-OBSERVATION.glasgow_coma_scale.v1]/data[at0001]/events[at0002]/data[at0003]/items[at0026]/value/magnitude')


      assert c.item.isValid(5)
      assert !c.item.isValid(0)
      assert !c.item.isValid(666)
*/

      opt.nodes.each {

         if (it.value instanceof ArchetypeSlot)
         {
            //println it.key +": "+ it.value

	    println it.value.includes
         }
      }
   }

   void testParseNodesCInteger()
   {
      println "====== testParseNodesCInteger ======"
      def path = "resources"+ PS +"opts"+ PS + OptManager.DEFAULT_NAMESPACE + PS +"Review.opt"
      def opt = loadAndParse(path)

      def c = opt.getNode('/content[archetype_id=openEHR-EHR-OBSERVATION.glasgow_coma_scale.v1]/data[at0001]/events[at0002]/data[at0003]/items[at0026]/value/magnitude')


      assert c.item.isValid(5)
      assert !c.item.isValid(0)
      assert !c.item.isValid(666)

      opt.nodes.each {

         if (it.value instanceof PrimitiveObjectNode)
         {
            //println it.key +": "+ it.value

            if (it.value.item instanceof CInteger)
            {
               println it.value.item.range
            }
         }
      }
   }

   void testParseNodesCDateTime()
   {
      println "====== testParseNodesCDateTime ======"
      def path = "resources"+ PS +"opts"+ PS + OptManager.DEFAULT_NAMESPACE + PS +"Test all datatypes_en.opt"
      def opt = loadAndParse(path)


      def c = opt.getNode('/content[archetype_id=openEHR-EHR-OBSERVATION.test_all_datatypes.v1]/data[at0001]/events[at0002]/data[at0003]/items[at0012]/value/value')

      assert c.item instanceof CDateTime
      assert c.item.pattern == 'yyyy-mm-ddTHH:MM:SS'

      assert c.item.isValid('1981-10-24T09:59:56')
      assert c.item.isValid('1981-10-24T09:59:56Z')
      assert c.item.isValid('1981-10-24T09:59:56-03:00')
      assert c.item.isValid('1981-10-24T09:59:56.666')
      assert !c.item.isValid('1981-10-24T09:59')

      /*
      opt.nodes.each {

         if (it.value instanceof PrimitiveObjectNode)
         {
            //println it.key +": "+ it.value

            if (it.value.item instanceof CDateTime)
            {
               println it.value.item.pattern
            }
         }
      }
      */
   }

   void testParseNodesCDuration()
   {
      println "====== testParseNodesCDuration ======"
      def path = "resources"+ PS +"opts"+ PS + OptManager.DEFAULT_NAMESPACE + PS +"Test all datatypes_en.opt"
      def opt = loadAndParse(path)


      def c = opt.getNode('/content[archetype_id=openEHR-EHR-OBSERVATION.test_all_datatypes.v1]/data[at0001]/events[at0002]/data[at0003]/items[at0018]/value/value')

      assert c.item instanceof CDuration
      assert c.item.range.lower.value == 'PT0H'
      assert c.item.range.upper.value == 'PT5H'


      assert c.item.isValid('PT0H')
      assert c.item.isValid('PT1H')
      assert c.item.isValid('PT5H')
      assert !c.item.isValid('PT10H')
      //assert !c.item.isValid('P2Y') this fails since the Java Duration only allows from Days to Seconds



/*
      opt.nodes.each {

         if (it.value instanceof PrimitiveObjectNode)
         {
            if (it.value.item instanceof CDuration)
            {
               println it.key +": "+ it.value
               println it.value.item.range
            }
         }
      }
*/
   }

/*
   void testParseToJSON()
   {
      println "====== testParseToJSON ======"
      def path = "resources"+ PS +"opts"+ PS + OptManager.DEFAULT_NAMESPACE + PS +"Review.opt"
      def opt = loadAndParse(path)

      // cant handle loops to parent
      //println groovy.json.JsonOutput.toJson( opt )​
      def xml = new File(path).text
      def json = JsonInstanceGenerator.xmlToJson(xml)

      println xml.size()
      println json.size()

      def toJson = new JsonSerializer()
      toJson.serialize(opt)
      println toJson.get(true)
   }
*/

   void testParseToJSON2()
   {
      println "====== testParseToJSON ======"
      def path = "resources"+ PS +"opts"+ PS + OptManager.DEFAULT_NAMESPACE + PS +"Test all datatypes_en.opt"
      def opt = loadAndParse(path)

      def toJson = new JsonSerializer()
      toJson.serialize(opt)
      println toJson.get(true)
   }

   void testParseNodesCDvOrdinal()
   {
      println "====== testParseNodesCDvOrdinal ======"
      def path = "resources"+ PS +"opts"+ PS + OptManager.DEFAULT_NAMESPACE + PS +"Review.opt"
      def opt = loadAndParse(path)


      def cdo = opt.getNode('/content[archetype_id=openEHR-EHR-OBSERVATION.glasgow_coma_scale.v1]/data[at0001]/events[at0002]/data[at0003]/items[at0009]/value')

      assert cdo instanceof CDvOrdinal
      assert cdo.isValid(1, 'at0010', 'local')
      assert !cdo.isValid(2, 'at0010', 'local') // value and code exists, but the code is not for this value
      assert !cdo.isValid(1, 'at0010', 'SNOMED') // value and code exists, but terminology is not for those value and code
      assert !cdo.isValid(666, 'at0010', 'local') // value doesnt exists
      assert !cdo.isValid(1, 'a6666', 'local') // code doesnt exists

      opt.nodes.each {

         if (it.value instanceof CDvOrdinal)
         {
            println it.key

            //println it.key +": "+ it.value
            it.value.list.each { oti ->
               println oti.value +" "+ oti.symbol.codeString +' '+ oti.symbol.terminologyId
            }
         }
      }
   }

   void testParserCodedTextConstraint()
   {
      println "====== testParserCodedTextConstraint ======"

      def path = "resources"+ PS +"opts"+ PS + OptManager.DEFAULT_NAMESPACE + PS +"Referral.opt"
      def opt = loadAndParse(path)

      assertToString(opt.concept, 'Referral')

      assertNotNull(opt.definition)


      // opt.nodes is a map path->ObjectNode
      def termConstraintsMap = opt.nodes.findAll { it.value.rmTypeName == 'CODE_PHRASE' }

      assertNotNull(termConstraintsMap)

      termConstraintsMap.each { tpath, node ->

         println tpath

         if (tpath == '/context/participations/function/defining_code')
         {
            assert node.codeList.size() == 5

            node.codeList.each {
               println it // at00XX
            }
         }
      }

      //assert opt.getTerm('openEHR-EHR-OBSERVATION.terminology_ref.v1', 'at0004') == 'Terminology ref'
      //opt.definition.attributes.each { println it.rmAttributeName }
   }

   void testParserConstraintRefConstraint()
   {
      println "====== testParserConstraintRefConstraint ======"

      def path = "resources"+ PS +"opts"+ PS + OptManager.DEFAULT_NAMESPACE + PS +"eReferral.opt"
      def opt = loadAndParse(path)

      assertToString(opt.concept, 'eReferral')
      assertNotNull(opt.definition)

      // opt.nodes is a map path->ObjectNode

      // Get the only CONSTRAINT_REF in the OPT
      // Check the reference is set to the value that is on the OPT
      // Check the constraint type is CONSTRAINT_REF

      def constraint_refs = opt.nodes.findAll{ it.key == '/content[archetype_id=openEHR-EHR-SECTION.problem_list.v1]/items[archetype_id=openEHR-EHR-EVALUATION.problem-diagnosis.v1]/data[at0001]/items[at0002.1]/value/defining_code' }
      assert constraint_refs.size() == 1
      assert constraint_refs.values()[0].reference == 'ac0.1'
      assert constraint_refs.values()[0].type == 'CONSTRAINT_REF'
   }


/*
   void testParseNodesCCodePhrase()
   {
      println "====== testParseNodesCCodePhrase ======"
      def path = "resources"+ PS +"opts"+ PS + OptManager.DEFAULT_NAMESPACE + PS +"Referral.opt"
      def opt = loadAndParse(path)

      def category = opt.getNode('/category/defining_code')

      assert category instanceof CCodePhrase

      println category.codeList
      println category.terminologyIdName


      opt.nodes.each {

         if (it.value instanceof CCodePhrase)
         {
            println it.key +": "+ it.value
            println it.value.codeList
            println it.value.terminologyIdName
         }
      }

   }



*/


/*

   void testXMLGenerator()
   {
      def path = "resources"+ PS +"opts"+ PS + OptManager.DEFAULT_NAMESPACE + PS +"Referral.opt"
      def opt = loadAndParse(path)
      def igen = new XmlInstanceGenerator()
      def ins = igen.generateXMLCompositionStringFromOPT(opt)
      //println ins

      new File( "documents" + PS + new java.text.SimpleDateFormat("'"+ opt.concept+"_'yyyyMMddhhmmss'.xml'").format(new Date()) ) << ins
   }


   void testXMLGenerator2()
   {
      def path = "resources"+ PS +"opts"+ PS + OptManager.DEFAULT_NAMESPACE + PS +"sample template_en.opt"
      def opt = loadAndParse(path)
      def igen = new XmlInstanceGenerator()
      def ins = igen.generateXMLCompositionStringFromOPT(opt)
      //println ins

      new File( "documents" + PS + new java.text.SimpleDateFormat("'"+ opt.concept+"_'yyyyMMddhhmmss'.xml'").format(new Date()) ) << ins
   }

   void testJSONGenerator()
   {
      def path = "resources"+ PS +"opts"+ PS + OptManager.DEFAULT_NAMESPACE + PS +"Referral.opt"
      def opt = loadAndParse(path)
      def igen = new JsonInstanceGenerator()
      def ins = igen.generateJSONCompositionStringFromOPT(opt)
      //println ins

      new File( "documents" + PS + new java.text.SimpleDateFormat("'"+ opt.concept+"_'yyyyMMddhhmmss'.json'").format(new Date()) ) << ins
   }



   void testUIGenerator()
   {
      def path = "resources"+ PS +"opts"+ PS + OptManager.DEFAULT_NAMESPACE + PS +"Encuentro.opt"
      def opt = loadAndParse(path)
      def gen = new OptUiGenerator()
      def ui = gen.generate(opt)

      //println ui

      new File( "html" + PS + new java.text.SimpleDateFormat("'"+ opt.concept +"_'yyyyMMddhhmmss'_"+ opt.langCode +".html'").format(new Date()) ) << ui
   }

   void testUIGenerator2()
   {
      def path = "resources"+ PS +"opts"+ PS + OptManager.DEFAULT_NAMESPACE + PS +"Referral.opt"
      def opt = loadAndParse(path)
      def gen = new OptUiGenerator()
      def ui = gen.generate(opt)

      //println ui

      new File( "html" + PS + new java.text.SimpleDateFormat("'"+ opt.concept +"_'yyyyMMddhhmmss'_"+ opt.langCode +".html'").format(new Date()) ) << ui
   }

   void testUIGeneratorObservationEN()
   {
      def path = "resources"+ PS +"sets"+ PS +"composition_observation_1"+ PS +"composition observation en.opt"
      def opt = loadAndParse(path)
      def gen = new OptUiGenerator()
      def ui = gen.generate(opt)
      new File( "html" + PS + new java.text.SimpleDateFormat("'"+ opt.concept +"_'yyyyMMddhhmmss'_"+ opt.langCode +".html'").format(new Date()) ) << ui
   }
   void testUIGeneratorObservationES()
   {
      def path = "resources"+ PS +"sets"+ PS +"composition_observation_1"+ PS +"composition observation es.opt"
      def opt = loadAndParse(path)
      def gen = new OptUiGenerator()
      def ui = gen.generate(opt)
      new File( "html" + PS + new java.text.SimpleDateFormat("'"+ opt.concept +"_'yyyyMMddhhmmss'_"+ opt.langCode +".html'").format(new Date()) ) << ui
   }


   void testValidacionXSD1()
   {
      def validator = new XmlInstanceValidation('xsd'+ File.separator + 'Version.xsd')

      // Recorre todos los archivos generador en /documents
      new File('documents' + File.separator).eachFileMatch(~/.*.xml/) { xml ->

        if (!validator.validate( xml.text ))
        {
           println xml.name +' NO VALIDA'
           println '====================================='
           validator.errors.each {
              println it
           }
           println '====================================='
        }
        else
           println xml.name +' VALIDA'
      }
   }
*/


   void testTerminologyParser()
   {
      def tm = new TerminologyParser()
      def terms = tm.parseTerms(new File("resources"+ PS +"terminology"+ PS +"openehr_terminology_en.xml"))
      //println terms
      assert tm.getRubric('en', '433') == 'event'
      println tm.getRubric('en', '229')
   }


/*
   void testParser()
   {
      log.info(  new File('').getCanonicalPath() )
      log.info(  System.getProperty("user.dir") )


      //assert 1 == 1
      //assert 2 + 2 == 4 : "We're in trouble, arithmetic is broken"

      def path = "resources"+ PS +"opts"+ PS +"Heart Failure Clinic First Visit Summary.opt"
      def opt = loadAndParse(path)

      assertToString(opt.concept, 'Heart Failure Clinic First Visit Summary')

      assertNotNull(opt.definition)

      assertLength(4, opt.definition.attributes.toArray())

      opt.definition.attributes.each {
         log.info( it.rmAttributeName )
      }
   }

   void testParserTerminologyRefOpt()
   {
      def path = "resources"+ PS +"opts"+ PS +"Terminology ref.opt"
      def opt = loadAndParse(path)

      assertToString(opt.concept, 'Terminology ref')

      assertNotNull(opt.definition)

      assertLength(2, opt.definition.attributes.toArray())

      opt.definition.attributes.each { attr ->
         assert ['category', 'content'].contains(attr.rmAttributeName)
      }

      // opt.nodes is a map path->ObjectNode
      def termConstraintsMap = opt.nodes.findAll { it.value.rmTypeName == 'CODE_PHRASE' }

      assertNotNull(termConstraintsMap)

      termConstraintsMap.each { tpath, node ->

         if (tpath == 'content[archetype_id=openEHR-EHR-OBSERVATION.terminology_ref.v1]/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/defining_code')
         {
            assert node.terminologyRef == 'terminology:SNOMED-CT?subset=motivo_de_consulta'
         }
      }

      println "opt nodes "+ opt.nodes
      assert opt.nodes.size() == 10

      opt.nodes.each { tpath, node ->

         log.info( node.termDefinitions.code.toString() )

         if (tpath == "/") assert node.termDefinitions.size() == 2
         if (tpath == "/content[archetype_id=openEHR-EHR-OBSERVATION.terminology_ref.v1]") assert node.termDefinitions.size() == 5
      }

      def nodeWithTerms = opt.getNode("/content[archetype_id=openEHR-EHR-OBSERVATION.terminology_ref.v1]")
      assert nodeWithTerms.termDefinitions.size() == 5

      assert opt.getTerm('openEHR-EHR-OBSERVATION.terminology_ref.v1', 'at0004') == 'Terminology ref'

      assert ['openEHR-EHR-COMPOSITION.terminology_ref_compo.v1', 'openEHR-EHR-OBSERVATION.terminology_ref.v1'] == opt.getReferencedArchetypes().archetypeId

      //opt.definition.attributes.each { println it.rmAttributeName }
   }

   void testFlatNodes()
   {
      def path = "resources"+ PS +"opts"+ PS +"Terminology ref.opt"
      def opt = loadAndParse(path)
      opt.getNodes().each { tpath, node ->
         println node.nodes
      }
   }



   void testParserQuantityUnits()
   {
      def path = "resources"+ PS +"opts"+ PS +"Encuentro.opt"
      def opt = loadAndParse(path)
      def termConstraintsMap = opt.nodes.findAll { it.value.rmTypeName == 'DV_QUANTITY' }

      termConstraintsMap.each { tpath, node ->

         println tpath

         // /content[archetype_id=openEHR-EHR-SECTION.vital_signs.v1]/items[archetype_id=openEHR-EHR-OBSERVATION.blood_pressure.v1]/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value
         // /content[archetype_id=openEHR-EHR-SECTION.vital_signs.v1]/items[archetype_id=openEHR-EHR-OBSERVATION.blood_pressure.v1]/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value

         if (tpath == '/content[archetype_id=openEHR-EHR-SECTION.vital_signs.v1]/items[archetype_id=openEHR-EHR-OBSERVATION.blood_pressure.v1]/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value')
         {
            assert node.xmlNode.list.size() == 1

            node.xmlNode.list.each {
               println it.units.text() // mm[Hg]
            }
         }
      }

   }

   void testOptManager()
   {
      String PS = File.separator
      def man = OptManager.getInstance('resources'+ PS +'opts')

      assert man.getLoadedOpts().size() == 0

      man.loadAll()

      assert man.getLoadedOpts().size() == 9

      def opt = man.getOpt('Terminology ref')

      assertNotNull(opt)

      println man.getAllReferencedArchetypes().keySet()

      assert opt.nodes.size() == 10
   }
   */


}
