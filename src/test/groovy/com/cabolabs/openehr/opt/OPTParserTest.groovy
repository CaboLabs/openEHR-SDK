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

class OPTParserTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")

   /*
   OperationalTemplate loadAndParse(String path)
   {
      def parser = new OperationalTemplateParser()

      //println getClass().getResource(path).toURI()

      def optFile = new File(getClass().getResource(path).toURI())
      //def optFile = new File( path )
      def text = optFile.getText()

      assertNotNull(text)
      assert text != ''

      return parser.parse( text )
   }
   */


/*
   void testAttributeParentNode()
   {
      println "====== testAttributeParentNode ======"
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/Referral.opt"
      def opt = TestUtils.loadTemplate(path)
      opt.definition.attributes.each { attr ->
         println attr.rmAttributeName
         assert attr.parent == opt.definition
      }
   }
*/

   void testEHR_STATUS_anyOPT()
   {
      println "========= testEHR_STATUS_anyOPT ==========="
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/ehr_status_any_en_v1.opt"
      def opt = TestUtils.loadTemplate(path)

      // incomplete
      def toJson = new JsonSerializer()
      toJson.serialize(opt)
      def incomplete = toJson.get(true)
      //new File('incomplete.json') << toJson.get(true)

      // complete
      toJson = new JsonSerializer()
      opt.complete()
      toJson.serialize(opt)
      def complete = toJson.get(true)
      //new File('complete.json') << toJson.get(true)

      def subject = opt.getNodes('/subject')[0]
      assert subject
      assert subject instanceof ObjectNode
      assert subject.rmTypeName == 'PARTY_SELF'
      assert subject.type == 'C_COMPLEX_OBJECT'

      def other_details = opt.getNodes('/other_details')
      assert !other_details

      assert incomplete.size() < complete.size()
   }

   void testEHR_STATUS_treeOPT()
   {
      println "========= testEHR_STATUS_treeOPT ==========="
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/ehr_status_tree_en_v1.opt"
      def opt = TestUtils.loadTemplate(path)

      // incomplete
      def toJson = new JsonSerializer()
      toJson.serialize(opt)
      def incomplete = toJson.get(true)
      //new File('incomplete.json') << toJson.get(true)

      // complete
      toJson = new JsonSerializer()
      opt.complete()
      toJson.serialize(opt)
      def complete = toJson.get(true)
      //new File('complete.json') << toJson.get(true)

      def subject = opt.getNodes('/subject')[0]
      assert subject
      assert subject instanceof ObjectNode
      assert subject.rmTypeName == 'PARTY_SELF'
      assert subject.type == 'C_COMPLEX_OBJECT'

      def other_details = opt.getNodes('/other_details[at0001]')[0]
      assert other_details
      assert other_details instanceof ObjectNode
      assert other_details.rmTypeName == 'ITEM_TREE'
      assert other_details.type == 'C_COMPLEX_OBJECT'

      assert incomplete.size() < complete.size()
   }

   void testEHR_STATUS_codedOPT()
   {
      println "========= testEHR_STATUS_codedOPT ==========="
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/ehr_status_coded_en_v1.opt"
      def opt = TestUtils.loadTemplate(path)

      // incomplete
      def toJson = new JsonSerializer()
      toJson.serialize(opt)
      def incomplete = toJson.get(true)
      //new File('incomplete.json') << toJson.get(true)

      // complete
      toJson = new JsonSerializer()
      opt.complete()
      toJson.serialize(opt)
      def complete = toJson.get(true)
      //new File('complete.json') << toJson.get(true)

      def subject = opt.getNodes('/subject')[0]
      assert subject
      assert subject instanceof ObjectNode
      assert subject.rmTypeName == 'PARTY_SELF'
      assert subject.type == 'C_COMPLEX_OBJECT'

      def other_details = opt.getNodes('/other_details[at0001]')[0]
      assert other_details
      assert other_details instanceof ObjectNode
      assert other_details.rmTypeName == 'ITEM_TREE'
      assert other_details.type == 'C_COMPLEX_OBJECT'

      def coded = opt.getNodes('/other_details[at0001]/items[at0002]/value')[0]
      assert coded
      assert coded instanceof ObjectNode
      assert coded.rmTypeName == 'DV_CODED_TEXT'
      assert coded.type == 'C_COMPLEX_OBJECT'

      assert incomplete.size() < complete.size()
   }


   void testCompleteOPT()
   {
      println "========= testCompleteOPT ==========="
      def path = "opts/"+ 'test_all_types2' + "/test_all_types_en_v1.opt"
      def opt = TestUtils.loadTemplate(path)

      // incomplete
      def toJson = new JsonSerializer()
      toJson.serialize(opt)
      def incomplete = toJson.get(true)
      //new File('incomplete.json') << toJson.get(true)

      // complete
      toJson = new JsonSerializer()
      opt.complete()
      toJson.serialize(opt)
      def complete = toJson.get(true)
      //new File('complete.json') << toJson.get(true)

      assert incomplete.size() < complete.size()
   }

   void testTextDescription()
   {
      println "========= testTextDescription ==========="
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/review.opt"
      def opt = TestUtils.loadTemplate(path)
      opt.complete()
      def nodes = opt.nodes.findAll{ it.key.endsWith('null_flavour') }

      nodes.each { tpath, obn ->
         println tpath
         println obn.text
      }
   }

   void testGetText()
   {
      println "========= testGetText ==========="
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/review.opt"
      def opt = TestUtils.loadTemplate(path)
      opt.complete()
      def nodes = opt.nodes

      println opt.definition.text

      nodes.each { tpath, obn ->
         println tpath +') '+ obn.text
      }
   }


   void testParentPathsReviewOPT()
   {
      println "========= testParentPathsReviewOPT ==========="
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/review.opt"
      def opt = TestUtils.loadTemplate(path)
      opt.complete()

      opt.nodes.sort{ it.key }.each { tpath, obns ->
         //println tpath
         //println obns.archetypeId
         println obns.path // multiple nodes can occur at the same template path
      }

      def obn = opt.findRoot('openEHR-EHR-OBSERVATION.glasgow_coma_scale.v1')

      println obn

      // Get the object node that for thet EVENT.time inside the observation
      // NOTE: this is a list
      def event_time_node = obn.getNodes('/data[at0001]/events[at0002]/time')

      // We know the object node is for a datatype, so the parent object should be a locatable or pathable,
      // in this case is for the EVENT
      def event_node = event_time_node[0].parent.parent

      println event_time_node[0]
      println event_node.rmTypeName


      // Then we want to verify if the event node contains a descendant path,
      // this is to know the data
      println event_node.nodes.find{ it.key == '/data[at0001]/events[at0002]/data[at0003]/items[at0037]/value' }

      // event_node.nodes.each {
      //    println it.key
      // }

      //println obn.getNodes('/data[at0001]/events[at0002]')

      // obn.getNodes('/data[at0001]/events[at0002]').each{ _node ->
      //    _node.nodes.each { pth, _nodes ->
      //       println "--"+ pth
      //    }
      // }

      println obn.getNodes('/data[at0001]/events[at0002]/data[at0003]/items[at0037]/value')
   }


   void testCBooleanParse()
   {
      println "====== testCBooleanParse ======"

      def path = "opts/"+ 'test_all_types2' + "/test_all_types_en_v1.opt"
      def opt = TestUtils.loadTemplate(path)

      opt.nodes.values()
         .findAll { it instanceof PrimitiveObjectNode && it.item instanceof CBoolean }
         .collect { it.item }
         .each { cb ->

            assert cb.trueValid
            assert cb.falseValid
         }
   }


   void testActionPaths()
   {
      println "====== testActionPaths ======"
      //def path = "opts/"+'test_ism_paths'+ "/test_ism_paths.opt"
      def path = "opts/test_ism_paths/test_ism_paths.opt" // For JDK 11 the resource shouldn't start with /
      def opt = TestUtils.loadTemplate(path)

      opt.nodes.values().sort{ it.path }.each { n ->

         println n.getClass().getSimpleName() +' p: '+ n.path +', dp: '+ n.dataPath


         // o.attributes.each { a ->
         //    println "  - " + a.rmAttributeName
         //    a.children.each { o2 ->
         //       println "    - " + o2.getClass().getSimpleName() +' p: '+ o2.path +', dp: '+ o2.dataPath
         //    }
         // }
      }


      def c = opt.getNodes('/content[archetype_id=openEHR-EHR-ACTION.test_ism_paths.v1]/ism_transition/current_state')[0]
      assert c instanceof ObjectNode
      assert c.rmTypeName == 'DV_CODED_TEXT'
      assert c.type == 'C_COMPLEX_OBJECT'


      opt.nodes.each { k, v ->
         println k
         println v.path
         println v.dataPath
         println "---"
      }
   }

   void testParseNodesCDvQuantity()
   {
      println "====== testParseNodesCDvQuantity ======"
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/lab_results1.opt"
      def opt = TestUtils.loadTemplate(path)

      def cdi = opt.getNodes('/content[archetype_id=openEHR-EHR-OBSERVATION.lab_test-full_blood_count.v1]/data[at0001]/events[at0002]/data[at0003]/items[at0078.4]/value')[0]

      assert cdi instanceof CDvQuantity

      assert  cdi.isValid(new DvQuantity(
         units: 'gm/l',
         magnitude: 50.5
      ))

      assert !cdi.isValid(new DvQuantity(
         units: 'qweerty',
         magnitude: 50.5
      ))

      assert !cdi.isValid(new DvQuantity(
         units: 'gm/l',
         magnitude: -50.5
      ))

      assert !cdi.isValid(new DvQuantity(
         units: 'qweerty',
         magnitude: -50.5
      ))

      assert cdi.isValid(new DvQuantity(
         units: 'qweerty',
         magnitude: 50.5
      )).message == "units 'qweerty' don't match [gm/l]"

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
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/signos.opt"
      def opt = TestUtils.loadTemplate(path)

      //def c = opt.getNode('/content[archetype_id=openEHR-EHR-OBSERVATION.glasgow_coma_scale.v1]/data[at0001]/events[at0002]/data[at0003]/items[at0026]/value/magnitude')
      //assert c.item.isValid(5)
      //assert !c.item.isValid(0)
      //assert !c.item.isValid(666)

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
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/review.opt"
      def opt = TestUtils.loadTemplate(path)

      def c = opt.getNodes('/content[archetype_id=openEHR-EHR-OBSERVATION.glasgow_coma_scale.v1]/data[at0001]/events[at0002]/data[at0003]/items[at0026]/value/magnitude')[0]

      assert  c instanceof ObjectNode
      assert  c.item instanceof CInteger

      assert  c.item.isValid(5)
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
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/test_all_datatypes_en_v1.opt"
      def opt = TestUtils.loadTemplate(path)


      def c = opt.getNodes('/content[archetype_id=openEHR-EHR-OBSERVATION.test_all_datatypes.v1]/data[at0001]/events[at0002]/data[at0003]/items[at0012]/value/value')[0]

      assert  c instanceof PrimitiveObjectNode
      assert  c.item instanceof CDateTime

      assert  c.item.pattern == 'yyyy-mm-ddTHH:MM:SS'
      assert  c.item.isValid('1981-10-24T09:59:56')
      assert  c.item.isValid('1981-10-24T09:59:56Z')
      assert  c.item.isValid('1981-10-24T09:59:56-03:00')
      assert  c.item.isValid('1981-10-24T09:59:56.666')
      assert !c.item.isValid('1981-10-24T09:59')


      // opt.nodes.each {

      //    if (it.value instanceof PrimitiveObjectNode)
      //    {
      //       //println it.key +": "+ it.value

      //       if (it.value.item instanceof CDateTime)
      //       {
      //          println it.value.item.pattern
      //       }
      //    }
      // }
   }

   void testParseNodesCDuration()
   {
      println "====== testParseNodesCDuration ======"
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/test_all_datatypes_en_v1.opt"
      def opt = TestUtils.loadTemplate(path)


      def c = opt.getNodes('/content[archetype_id=openEHR-EHR-OBSERVATION.test_all_datatypes.v1]/data[at0001]/events[at0002]/data[at0003]/items[at0018]/value/value')[0]

      assert  c instanceof PrimitiveObjectNode

      assert  c.item instanceof CDuration
      assert  c.item.range.lower.value == 'PT0H'
      assert  c.item.range.upper.value == 'PT5H'

      assert  c.item.isValid('PT0H')
      assert  c.item.isValid('PT1H')
      assert  c.item.isValid('PT5H')
      assert !c.item.isValid('PT10H')

      assert  c.item.isValid('PT10H').message == "value 'PT10H' is not in the interval PT0H..PT5H"
      //assert !c.item.isValid('P2Y') this fails since the Java Duration only allows from Days to Seconds

      // opt.nodes.each {

      //    if (it.value instanceof PrimitiveObjectNode)
      //    {
      //       if (it.value.item instanceof CDuration)
      //       {
      //          println it.key +": "+ it.value
      //          println it.value.item.range
      //       }
      //    }
      // }
   }


   /*
   void testParseToJSON()
   {
      println "====== testParseToJSON ======"
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/Review.opt"
      def opt = TestUtils.loadTemplate(path)

      // cant handle loops to parent
      //println groovy.json.JsonOutput.toJson(opt)
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
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/test_all_datatypes_en_v1.opt"
      def opt = TestUtils.loadTemplate(path)

      def toJson = new JsonSerializer()
      toJson.serialize(opt)
      //println toJson.get(true) // TODO: make some type of assert...
   }

   void testParseNodesCDvOrdinal()
   {
      println "====== testParseNodesCDvOrdinal ======"
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/review.opt"
      def opt = TestUtils.loadTemplate(path)


      def cdo = opt.getNodes('/content[archetype_id=openEHR-EHR-OBSERVATION.glasgow_coma_scale.v1]/data[at0001]/events[at0002]/data[at0003]/items[at0009]/value')[0]

      assert  cdo instanceof CDvOrdinal



      assert cdo.isValid(new DvOrdinal(
         value: 1,
         symbol: new DvCodedText(
            value: 'bla bla bla',
            defining_code: new CodePhrase(
               codeString: 'at0010',
               terminologyId: new TerminologyId(
                  value: 'local'
               )
            )
         )
      ))
      assert !cdo.isValid(new DvOrdinal(
         value: 2,
         symbol: new DvCodedText(
            value: 'bla bla bla',
            defining_code: new CodePhrase(
               codeString: 'at0010',
               terminologyId: new TerminologyId(
                  value: 'local'
               )
            )
         )
      ))// value and code exists, but the code is not for this value

      assert !cdo.isValid(new DvOrdinal(
         value: 1,
         symbol: new DvCodedText(
            value: 'bla bla bla',
            defining_code: new CodePhrase(
               codeString: 'at0010',
               terminologyId: new TerminologyId(
                  value: 'SNOMED'
               )
            )
         )
      )) // value and code exists, but terminology is not for those value and code

      assert !cdo.isValid(new DvOrdinal(
         value: 666,
         symbol: new DvCodedText(
            value: 'bla bla bla',
            defining_code: new CodePhrase(
               codeString: 'at0010',
               terminologyId: new TerminologyId(
                  value: 'local'
               )
            )
         )
      )) // value doesnt exists

      assert !cdo.isValid(new DvOrdinal(
         value: 1,
         symbol: new DvCodedText(
            value: 'bla bla bla',
            defining_code: new CodePhrase(
               codeString: 'a6666',
               terminologyId: new TerminologyId(
                  value: 'local'
               )
            )
         )
      )) // code doesnt exists

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

   void testParsernstraint()
   {
      println "====== testParserCodedTextConstraint ======"

      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/referral.opt"
      def opt = TestUtils.loadTemplate(path)

      assertToString(opt.concept, 'Referral')

      assertNotNull(opt.definition)


      // opt.nodes is a map path->ObjectNode
      def termConstraintsMap = opt.nodes.findAll { it instanceof ObjectNode && it.value.rmTypeName == 'CODE_PHRASE' }

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


   // <children xsi:type="CONSTRAINT_REF">
   void testParserConstraintRefConstraint()
   {
      println "====== testParserConstraintRefConstraint ======"

      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/amd_assessment.opt"
      def opt = TestUtils.loadTemplate(path)

      assertToString(opt.concept, 'AMD assessment')
      assertNotNull(opt.definition)

      // opt.nodes is a map path->ObjectNode

      // Get the only CONSTRAINT_REF in the OPT
      // Check the reference is set to the value that is on the OPT
      // Check the constraint type is CONSTRAINT_REF

      //opt.nodes.sort{it.key}.grep(~/.*0045.*/).each{println it.key}

      // List<CCodePhrase>
      def cs = opt.nodes.findAll{ it.key == '/content[archetype_id=openEHR-EHR-SECTION.image_test_analysis.v0]/items[archetype_id=openEHR-EHR-OBSERVATION.ophthalmic_tomography_examination.v0]/data[at0001]/events[at0002]/data[at0008]/items[at0039]/items[at0045]/value/defining_code'}.values().flatten()

      assert cs.size() == 2 // there are two alternative nodes with the same path
      assert cs[0] instanceof CCodePhrase
      assert cs[1] instanceof CCodePhrase

      // println cs[0].type
      // println cs[0].reference
      // println cs[0].terminologyRef

      def constraint_ref = cs[0]
      assert constraint_ref.reference == 'ac0001'
      assert constraint_ref.type == 'CONSTRAINT_REF'

      // cs[1] is a C_CODE_REFERENCE
   }



   void testParseNodesCCodePhrase()
   {
      println "====== testParseNodesCCodePhrase ======"
      def opt_path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/referral.opt"
      def opt = TestUtils.loadTemplate(opt_path)

      def category = opt.getNodes('/category/defining_code')[0]

      assert category instanceof CCodePhrase
      assert category.codeList == ['433']
      assert category.terminologyId == 'openehr'

      opt.nodes.each { path, node_list ->

         node_list.each { constraint ->

            if (constraint instanceof CCodePhrase)
            {
               println path +": "+ constraint
               println constraint.codeList
               assert ['local', 'openehr'].contains(constraint.terminologyId)
            }
         }
      }
   }




   void testXMLGenerator()
   {
      def path = "opts/"+ OptManager.DEFAULT_NAMESPACE + "/referral.opt"
      def opt = TestUtils.loadTemplate(path)
      def igen = new XmlInstanceGenerator()
      def ins = igen.generateXMLCompositionStringFromOPT(opt)
      //println ins

      new File( "documents" + PS + new java.text.SimpleDateFormat("'"+ opt.concept+"_'yyyyMMddhhmmss'.xml'").format(new Date()) ) << ins
   }


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
      def gen = new OptUiGenerator()
      def ui = gen.generate(opt)
      new File( "html" + PS + new java.text.SimpleDateFormat("'"+ opt.concept +"_'yyyyMMddhhmmss'_"+ opt.langCode +".html'").format(new Date()) ) << ui
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



   void testTerminologyParser()
   {
      def tm = TerminologyParser.getInstance()

      def terms = tm.parseTerms(new File(getClass().getResource("/terminology"+ "/openehr_terminology_en.xml").toURI()))
      //println terms
      assert tm.getRubric('en', '433') == 'event'
      println tm.getRubric('en', '229')
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

   /*

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

   void testOptManager()
   {
      String PS = File.separator
      def repo = new OptRepositoryFSImpl('resources'+ PS +'opts')
      def man = OptManager.getInstance()
      man.init(repo)

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
