package com.cabolabs.openehr.opt

import groovy.util.GroovyTestCase

import com.cabolabs.openehr.opt.parser.*
import com.cabolabs.openehr.opt.ui_generator.OptUiGenerator
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.model.primitive.*
import com.cabolabs.openehr.opt.model.domain.*
import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.opt.instance_generator.*
import com.cabolabs.openehr.terminology.TerminologyParser
import com.cabolabs.openehr.opt.instance_validation.XmlValidation
import com.cabolabs.openehr.opt.instance_generator.*
import com.cabolabs.openehr.opt.serializer.*
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Element

import com.cabolabs.openehr.rm_1_0_2.data_types.text.*
import com.cabolabs.openehr.rm_1_0_2.support.identification.TerminologyId
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.*

import com.cabolabs.testing.TestUtils

class OptXmlSerializerTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")

   private void validateOptString(OperationalTemplate opt)
   {
      // serialize opt
      def toXml = new OptXmlSerializer(true)
      String optString = toXml.serialize(opt)

      //println optString

      // validate string against schema
      def inputStream = getClass().getResourceAsStream('/xsd/OperationalTemplate.xsd')
      def schemaValidator = new XmlValidation(inputStream)
      boolean valid = schemaValidator.validate(optString)

      if (!valid)
      {
         println schemaValidator.getErrors()

         println optString
      }

      assert valid
   }

   private void verifySerializeParseSerialize(OperationalTemplate opt)
   {
      // serialize opt
      def toXml = new OptXmlSerializer(true)
      String optString = toXml.serialize(opt)

      // parse opt using the serialized string
      def parser = new OperationalTemplateParser()
      def optFromString = parser.parse(optString)

      // serialize the new parsed opt
      String optString2 = toXml.serialize(optFromString)

      // compare first serialization against the parsed and serialized from that string
      assert optString == optString2
   }



   void testError()
   {
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/solicitacao_exame_error.opt" // this OPT has an error: a C_PRIMITIVE_OBJECT.item is missing

      try
      {
         TestUtils.loadTemplate(path)
      }
      catch (Exception e)
      {
         assert e.getMessage().startsWith("Invalid template: missing required primitive.item")
      }
   }

   void testRole()
   {
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/generic_role_complete.opt"
      def opt = TestUtils.loadTemplate(path)

      validateOptString(opt)
      verifySerializeParseSerialize(opt)
   }

   void testEHR_STATUS_anyOPT()
   {
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/ehr_status_any_en_v1.opt"
      def opt = TestUtils.loadTemplate(path)

      validateOptString(opt)
      verifySerializeParseSerialize(opt)
   }

   void testEHR_STATUS_treeOPT()
   {
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/ehr_status_tree_en_v1.opt"
      def opt = TestUtils.loadTemplate(path)

      validateOptString(opt)
      verifySerializeParseSerialize(opt)
   }

   void testEHR_STATUS_codedOPT()
   {
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/ehr_status_coded_en_v1.opt"
      def opt = TestUtils.loadTemplate(path)

      validateOptString(opt)
      verifySerializeParseSerialize(opt)
   }

   void testAllTypes()
   {
      def path = "opts/"+ 'test_all_types2' + "/test_all_types_en_v1.opt"
      def opt = TestUtils.loadTemplate(path)

      validateOptString(opt)
      verifySerializeParseSerialize(opt)
   }

   void testAllTypesComplete()
   {
      def path = "opts/"+ 'test_all_types2' + "/test_all_types_en_v1.opt"
      def opt = TestUtils.loadTemplate(path)
      opt.complete()

      validateOptString(opt)
      verifySerializeParseSerialize(opt)
   }

   void testReview()
   {
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/review.opt"
      def opt = TestUtils.loadTemplate(path)

      validateOptString(opt)
      verifySerializeParseSerialize(opt)
   }

   void testReviewComplete()
   {
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/review.opt"
      def opt = TestUtils.loadTemplate(path)
      opt.complete()

      validateOptString(opt)
      verifySerializeParseSerialize(opt)
   }

   void testActionIsmPaths()
   {
      def path = "opts/test_ism_paths/test_ism_paths.opt" // For JDK 11 the resource shouldn't start with /
      def opt = TestUtils.loadTemplate(path)

      validateOptString(opt)
      verifySerializeParseSerialize(opt)
   }

   void testLabResults1()
   {
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/lab_results1.opt"
      def opt = TestUtils.loadTemplate(path)

      validateOptString(opt)
      verifySerializeParseSerialize(opt)
   }

   void testSignos()
   {
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/signos.opt"
      def opt = TestUtils.loadTemplate(path)

      validateOptString(opt)
      verifySerializeParseSerialize(opt)
   }

   void testReferral()
   {

      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/referral.opt"
      def opt = TestUtils.loadTemplate(path)

      validateOptString(opt)
      verifySerializeParseSerialize(opt)
   }


   // <children xsi:type="CONSTRAINT_REF">
   void testAmdAssessment()
   {
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/amd_assessment.opt"
      def opt = TestUtils.loadTemplate(path)

      validateOptString(opt)
      verifySerializeParseSerialize(opt)
   }

/*
   void testXMLGenerator2()
   {
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/sample_template_en.opt"
      def opt = TestUtils.loadTemplate(path)
      def igen = new XmlInstanceGenerator()
      def ins = igen.generateXMLCompositionStringFromOPT(opt)
      //println ins

      new File( "documents" + PS + new java.text.SimpleDateFormat("'"+ opt.concept+"_'yyyyMMddhhmmss'.xml'").format(new Date()) ) << ins
   }

   void testJSONGenerator()
   {
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/referral.opt"
      def opt = TestUtils.loadTemplate(path)
      def igen = new JsonInstanceGenerator()
      def ins = igen.generateJSONCompositionStringFromOPT(opt)
      //println ins

      def dpath = "documents" + PS + new java.text.SimpleDateFormat("'"+ opt.concept+"_'yyyyMMddhhmmss'.json'").format(new Date())
      new File(dpath) << ins
   }

   void testUIGenerator()
   {
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/encuentro.opt"
      def opt = TestUtils.loadTemplate(path)
      def gen = new OptUiGenerator()
      def ui = gen.generate(opt)

      //println ui

      new File( "html" + PS + new java.text.SimpleDateFormat("'"+ opt.concept +"_'yyyyMMddhhmmss'_"+ opt.langCode +".html'").format(new Date()) ) << ui
   }

   void testUIGenerator2()
   {
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/referral.opt"
      def opt = TestUtils.loadTemplate(path)
      def gen = new OptUiGenerator()
      def ui = gen.generate(opt)

      //println ui

      new File( "html" + PS + new java.text.SimpleDateFormat("'"+ opt.concept +"_'yyyyMMddhhmmss'_"+ opt.langCode +".html'").format(new Date()) ) << ui
   }

   void testUIGeneratorObservationEN()
   {
      def path = "sets"+ "/composition_observation_1"+ "/composition_observation_en.opt"
      def opt = TestUtils.loadTemplate(path)
      // def gen = new OptUiGenerator()
      // def ui = gen.generate(opt)
      // new File( "html" + PS + new java.text.SimpleDateFormat("'"+ opt.concept +"_'yyyyMMddhhmmss'_"+ opt.langCode +".html'").format(new Date()) ) << ui
   }

   void testUIGeneratorObservationES()
   {
      def path = "sets"+ "/composition_observation_1"+ "/composition_observation_es.opt"
      def opt = TestUtils.loadTemplate(path)
      def gen = new OptUiGenerator()
      def ui = gen.generate(opt)
      new File( "html" + PS + new java.text.SimpleDateFormat("'"+ opt.concept +"_'yyyyMMddhhmmss'_"+ opt.langCode +".html'").format(new Date()) ) << ui
   }

   void testValidacionXSD1()
   {
      def validator = new XmlValidation(getClass().getResourceAsStream(File.separator + 'xsd'+ File.separator + 'Version.xsd'))

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

   void testParser()
   {
      log.info(  new File('').getCanonicalPath() )
      log.info(  System.getProperty("user.dir") )


      //assert 1 == 1
      //assert 2 + 2 == 4 : "We're in trouble, arithmetic is broken"

      def path = "opts/"+ "com.cabolabs.openehr_opt.namespaces.default"+ "/referral.opt"
      def opt = TestUtils.loadTemplate(path)

      assertToString(opt.concept, 'Referral')

      assertNotNull(opt.definition)

      assertLength(3, opt.definition.attributes.toArray())

      opt.definition.attributes.each {
         log.info( it.rmAttributeName )
      }
   }


   // testing finding the collection attribute nodes in an OPT, to be able to read the cardinality constraints
   void testTraverseCollectionAttributes()
   {
      def collection_attrs = [
         'COMPOSITION': [
            'content'
         ],
         'SECTION': [
            'items'
         ],
         'INSTRUCTION': [
            'activities'
         ],
         'HISTORY': [
            'events'
         ],
         'ITEM_TREE': [
            'items'
         ],
         'ITEM_LIST': [
            'items'
         ],
         'ITEM_TABLE': [
            'rows'
         ],
         'CLUSTER': [
            'items'
         ],
         'FOLDER': [
            'folders'
         ]
      ]

      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/test_all_datatypes_en_v1.opt"
      def opt = TestUtils.loadTemplate(path)

      def collection_attr_nodes = [], // collection attribute nodes in the current OPT
          found_object_nodes,
          collect_found_object_nodes = []

      collection_attrs.each { clazz, attrs ->

         attrs.each { attr ->

            //println "searching ${clazz}.${attr}"

            opt.nodes.each { opath, object_nodes ->

               //println "matching ${object_nodes.rmTypeName}"

               found_object_nodes = object_nodes.findAll { it.rmTypeName == clazz }

               if (found_object_nodes)
               {
                  collect_found_object_nodes.addAll(found_object_nodes)
               }
            }

            collect_found_object_nodes.each { object_node ->
               collection_attr_nodes.addAll(
                  object_node.attributes.find{ an -> an.rmAttributeName == attr }
               )
            }

            collect_found_object_nodes = []
         }
      }

      // all the collection attribute will have cardinality constraint
      collection_attr_nodes.each { attr_node ->

         assert attr_node.cardinality != null
      }

      println collection_attr_nodes.rmAttributeName
      println collection_attr_nodes.path
      println collection_attr_nodes.cardinality.interval
      println collection_attr_nodes.cardinality.interval*.anyAllowed()
   }

   void testParserTerminologyRefOpt()
   {
      def path = "opts"+ "/Terminology ref.opt"
      def opt = TestUtils.loadTemplate(path)

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
      def path = "opts"+ "/Terminology ref.opt"
      def opt = TestUtils.loadTemplate(path)
      opt.getNodes().each { tpath, node ->
         println node.nodes
      }
   }

   void testParserQuantityUnits()
   {
      def path = "opts"+ "/Encuentro.opt"
      def opt = TestUtils.loadTemplate(path)
      def termConstraintsMap = opt.nodes.findAll { it.value.rmTypeName == 'DV_QUANTITY' }

      termConstraintsMap.each { tpath, node ->

         println tpath

         // /content[archetype_id=openEHR-EHR-SECTION.vital_signs.v1]/items[archetype_id=openEHR-EHR-OBSERVATION.blood_pressure.v1]/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value
         // /content[archetype_id=openEHR-EHR-SECTION.vital_signs.v1]/items[archetype_id=openEHR-EHR-OBSERVATION.blood_pressure.v1]/data[at0001]/events[at0006]/data[at0003]/items[at0005]/value

         if (tpath == '/content[archetype_id=openEHR-EHR-SECTION.vital_signs.v1]/items[archetype_id=openEHR-EHR-OBSERVATION.blood_pressure.v1]/data[at0001]/events[at0006]/data[at0003]/items[at0004]/value')
         {
            assert node.list.size() == 1

            node.list.each {
               println it.units // mm[Hg]
            }
         }
      }

   }

   */
}
