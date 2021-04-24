package com.cabolabs.openehr.opt

import groovy.util.GroovyTestCase

import com.cabolabs.openehr.opt.parser.*
import com.cabolabs.openehr.opt.ui_generator.OptUiGenerator
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.opt.instance_generator.*
import com.cabolabs.openehr.terminology.TerminologyParser
import com.cabolabs.openehr.opt.instance_validation.XmlInstanceValidation

class OPTManagerTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")

/*
   void testXMLGenerator()
   {
      def path = "resources"+ PS +"opts"+ PS +"Referral.opt"
      def opt = loadAndParse(path)
      def igen = new XmlInstanceGenerator()
      def ins = igen.generateXMLCompositionStringFromOPT(opt)
      //println ins

      new File( "documents" + PS + new java.text.SimpleDateFormat("'"+ opt.concept+"_'yyyyMMddhhmmss'.xml'").format(new Date()) ) << ins
   }
*/
   void testReferencedArchetypes()
   {
      println "====== testReferencedArchetypes ======"

      String namespace = 'test_ref_archs_namespace'
      String PS = File.separator

      def repo = new OptRepositoryFSImpl(getClass().getResource(PS +'opts').toURI())
      def man = OptManager.getInstance(repo)

      assert man.getLoadedOpts(namespace).size() == 0
      man.loadAll(namespace)
      assert man.getLoadedOpts(namespace).size() == 1

      println "Referenced Archetypes from OPTManager"
      def refArchs = man.getAllReferencedArchetypes(namespace)
      refArchs.keySet().sort{it}.each { archId ->
         println archId
         refArchs[archId].each { obj ->
            println obj.rmTypeName +' '+obj.archetypeId + obj.path
            obj.nodes.keySet().sort{it}.each { path ->
               if (obj.nodes[path] instanceof ObjectNode)
                  println "\t"+ obj.nodes[path].rmTypeName +"\t"+path
            }
         }
      }
      
      println man.getNodes('openEHR-EHR-OBSERVATION.test_all_datatypes.v1', '/', namespace)

      println man.cache

      def opt = man.getOpt('test_all_datatypes.es.v1', namespace)
      opt.nodes.keySet().sort{it}.each{ path ->
         println path
      }
      //println opt.getNode('/content[archetype_id=openEHR-EHR-OBSERVATION.test_all_datatypes.v1]')

      man.reset()
   }

   void testTemplateDataPaths()
   {
      println "====== testTemplateDataPaths ======"

      String namespace = 'test_ism_paths'
      String PS = File.separator
      def repo = new OptRepositoryFSImpl(getClass().getResource(PS +'opts').toURI())
      def man = OptManager.getInstance(repo)
      man.loadAll(namespace)

      def archs = man.getAllReferencedArchetypes(namespace) // List<ObjectNode>
      assert archs.size() == 2 // compo and action
      assert archs['openEHR-EHR-ACTION.test_ism_paths.v1'].size() == 1 // one root for action because it is referenced by just one OPT

      //println archs['openEHR-EHR-ACTION.test_ism_paths.v1'][0] // ObjectNode
      archs['openEHR-EHR-ACTION.test_ism_paths.v1'][0].nodes.each { archPath, objectNode ->
         println archPath
         println objectNode.templateDataPath
      }

      def templateDataPaths = [
         '/content[archetype_id=openEHR-EHR-ACTION.test_ism_paths.v1]/ism_transition',
         '/content[archetype_id=openEHR-EHR-ACTION.test_ism_paths.v1]/ism_transition/current_state',
         '/content[archetype_id=openEHR-EHR-ACTION.test_ism_paths.v1]/ism_transition/current_state/defining_code'
      ]

      def opt = man.getOpt('test_ism_paths.es.v1', namespace)

      assert opt

      def nodes
      templateDataPaths.each { tdp ->
      
         nodes = opt.getNodesByTemplateDataPath(tdp)

         assert nodes

         if (nodes)
         {
            println "FOUND FOR "+ tdp
            nodes.each { obj ->
               println obj.rmTypeName +' '+ obj.type
            }
         }
         else
         {
            println "NOT FOUND FOR "+ tdp
         }
      }

      man.unloadAll(namespace)

      man.reset()
   }

   void testDataPaths()
   {
      println "====== testDataPaths ======"

      String namespace = 'test_ism_paths'
      String PS = File.separator
      def repo = new OptRepositoryFSImpl(getClass().getResource(PS +'opts').toURI())
      def man = OptManager.getInstance(repo)

      assert man.getLoadedOpts(namespace).size() == 0
      man.loadAll(namespace)
      assert man.getLoadedOpts(namespace).size() == 1

      def archs = man.getAllReferencedArchetypes(namespace) // List<ObjectNode>
      assert archs.size() == 2 // compo and action
      assert archs['openEHR-EHR-ACTION.test_ism_paths.v1'].size() == 1 // one root for action because it is referenced by just one OPT

      //println archs['openEHR-EHR-ACTION.test_ism_paths.v1'][0] // ObjectNode
      archs['openEHR-EHR-ACTION.test_ism_paths.v1'][0].nodes.each { archPath, objectNodes ->
         println archPath
      }

      // result is a map!
      def constraints = archs['openEHR-EHR-ACTION.test_ism_paths.v1'][0].nodes.values().flatten().findAll{ it.dataPath == '/ism_transition/careflow_step/defining_code' }
      assert constraints.size() == 4
      constraints.each {
         println it.codeList
      }

      // this code should do the same as above, so result should be the same
      // but the result is the list of ObjectNode not a map
      def nodes = man.getNodesByDataPath('openEHR-EHR-ACTION.test_ism_paths.v1', '/ism_transition/careflow_step/defining_code', namespace)
      assert nodes.size() == 4
      nodes.each {
         println it.codeList
      }

      println nodes.collect{ it.codeList[0] }

      man.unloadAll(namespace)

      man.reset()
   }


   void testOptManagerLanguages()
   {
      println "====== testOptManagerLanguages ======"

      String PS = File.separator
      def repo = new OptRepositoryFSImpl(getClass().getResource(PS +'opts' + PS +'test_languages').toURI())
      def man = OptManager.getInstance(repo)

      assert man.getLoadedOpts().size() == 0

      man.loadAll()

      assert man.getLoadedOpts().size() == 2

      println "Loaded OPTs:"
      man.getLoadedOpts().each { id, opt ->
        println opt.templateId +' '+ opt.language
      }

      println "OPT nodes"
      man.getLoadedOpts().each { id, opt ->
         opt.nodes.each { optpath, nodes ->
            nodes.each { node ->
               if (node.archetypeId.contains("pulse") || node.path.contains('1055'))
                  println node.archetypeId +' '+ node.path // archetypeId is only present on root nodes
            }
         }
      }

      def archetypeId = 'openEHR-EHR-OBSERVATION.pulse.v1'
      man.getNodes(archetypeId, '/data[at0002]/events[at0003]/data[at0001]/items[at1055]/value/defining_code')*.codeList.flatten().each {

        println it // code
        println man.getText(archetypeId, it, 'es') // at00XX -> name
      }

      println "Referenced Archetypes from OPTManager"
      man.getAllReferencedArchetypes().keySet().each {
         println it
      }

      man.reset()
   }
   
}
