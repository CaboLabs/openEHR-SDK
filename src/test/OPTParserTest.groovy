package test

import groovy.util.GroovyTestCase

import com.cabolabs.openehr.opt.parser.*
import com.cabolabs.openehr.opt.ui_generator.OptUiGenerator
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.opt.instance_generator.*
import com.cabolabs.openehr.terminology.TerminologyParser

class OPTParserTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")


   void testXMLGenerator()
   {
      def path = "resources"+ PS +"opts"+ PS +"Referral.opt"
      def opt = loadAndParse(path)
      def igen = new XmlInstanceGenerator()
      def ins = igen.generateXMLCompositionStringFromOPT(opt)
      //println ins
      
      new File( "documents" + PS + new java.text.SimpleDateFormat("'"+ opt.concept+"_'yyyyMMddhhmmss'.xml'").format(new Date()) ) << ins
   }
   
   void testXMLGenerator2()
   {
      def path = "resources"+ PS +"opts"+ PS +"sample template_en.opt"
      def opt = loadAndParse(path)
      def igen = new XmlInstanceGenerator()
      def ins = igen.generateXMLCompositionStringFromOPT(opt)
      //println ins
      
      new File( "documents" + PS + new java.text.SimpleDateFormat("'"+ opt.concept+"_'yyyyMMddhhmmss'.xml'").format(new Date()) ) << ins
   }
   
   void testJSONGenerator()
   {
      def path = "resources"+ PS +"opts"+ PS +"Referral.opt"
      def opt = loadAndParse(path)
      def igen = new JsonInstanceGenerator()
      def ins = igen.generateJSONCompositionStringFromOPT(opt)
      //println ins
      
      new File( "documents" + PS + new java.text.SimpleDateFormat("'"+ opt.concept+"_'yyyyMMddhhmmss'.json'").format(new Date()) ) << ins
   }


   void testUIGenerator()
   {
      def path = "resources"+ PS +"opts"+ PS +"Encuentro.opt"
      def opt = loadAndParse(path)
      def gen = new OptUiGenerator()
      def ui = gen.generate(opt)
      
      //println ui
      
      new File( "html" + PS + new java.text.SimpleDateFormat("'"+ opt.concept +"_'yyyyMMddhhmmss'_"+ opt.langCode +".html'").format(new Date()) ) << ui
   }
   
   void testUIGenerator2()
   {
      def path = "resources"+ PS +"opts"+ PS +"Referral.opt"
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
   
   /*
   void testTerminologyParser()
   {
      def tm = new TerminologyParser()
      def terms = tm.parseTerms(new File("resources"+ PS +"terminology"+ PS +"openehr_terminology_en.xml"))
      //println terms
      assert tm.getRubric('en', '433') == 'event'
      println tm.getRubric('en', '229')
   }
   */
   
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
   
   void testParserCodedTextConstraint()
   {
      def path = "resources"+ PS +"opts"+ PS +"Referral.opt"
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
            assert node.xmlNode.code_list.size() == 5
            
            node.xmlNode.code_list.each {
               println it.text() // at00XX
            }
         }
      }
      

      //assert opt.getTerm('openEHR-EHR-OBSERVATION.terminology_ref.v1', 'at0004') == 'Terminology ref'
      
      //opt.definition.attributes.each { println it.rmAttributeName }
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
   
   OperationalTemplate loadAndParse(String path)
   {
      def parser = new OperationalTemplateParser()
      
      def optFile = new File( path )
      def text = optFile.getText()
      
      assertNotNull(text)
      assert text != ''
      
      return parser.parse( text )
   }
}
