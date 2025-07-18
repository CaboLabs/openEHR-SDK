package com.cabolabs.openehr.opt

import groovy.util.GroovyTestCase

import com.cabolabs.openehr.opt.parser.*
import com.cabolabs.openehr.opt.ui_generator.OptUiGenerator
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.opt.instance_generator.*
import com.cabolabs.openehr.opt.instance_validation.XmlValidation
import com.cabolabs.openehr.opt.serializer.JsonSerializer


class OptManagerTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator") // File.separator

   private man
   private used_namespaces = [
      'test_ism_paths',
      'test_ref_archs_namespace',
      OptManager.DEFAULT_NAMESPACE
   ]

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
   protected void setUp() throws Exception
   {
      super.setUp()
      println "setup"

      // this is the default repo, each test could change it
      def repo = new OptRepositoryFSImpl(getClass().getResource('/opts').toURI())
      this.man = OptManager.getInstance()
      this.man.init(repo, 2)
      //this.man.unloadAll()
   }

   protected void tearDown()
   {
      println "teadDown"
      used_namespaces.each { namespace ->
         this.man.unloadAll(namespace)
      }
      //man.reset()
   }

   void testRootArchetype()
   {
      println "====== testRootArchetype ======"

      String namespace = 'compo_root_nodes'

      // manager users the default repo
      assert man.getLoadedOpts(namespace).size() == 0
      man.loadAll(namespace, true)
      assert man.getLoadedOpts(namespace).size() == 1

      /*
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

      println man.getNodes('openEHR-EHR-ADMIN_ENTRY.demographics.v1', '/', namespace)

      println man.cache
      */

      def opt = man.getOpt('Demographics', namespace)
      // opt.nodes.keySet().sort{it}.each{ path ->
      //    println path
      // }

      def root = opt.findRoot("openEHR-EHR-COMPOSITION.demographics.v1")

      println root

      def toJson = new JsonSerializer()
      toJson.serialize(root)
      def json = toJson.get()

      println json

      //println opt.getNode('/content[archetype_id=openEHR-EHR-OBSERVATION.test_all_datatypes.v1]')

      man.reset()
   }


   void testReferencedArchetypes()
   {
      println "====== testReferencedArchetypes ======"

      String namespace = 'test_ref_archs_namespace'

      // manager users the default repo
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

   void testTemplateDataPathsReferral()
   {
      println "====== testTemplateDataPathsReferral ======"

      String namespace = 'test_template_data_paths'

      // manager users the default repo
      assert man.getLoadedOpts(namespace).size() == 0
      man.loadAll(namespace)
      assert man.getLoadedOpts(namespace).size() == 1

      def opt = man.getOpt('Referral')

      def dataPaths = [:]

      opt.nodes.each { templatePath, nodes ->
         //println templatePath

         nodes.each { node ->

            if (!dataPaths[templatePath])
            {
               dataPaths[templatePath] = 1
            }
            else
            {
               println nodes.templateDataPath
               dataPaths[templatePath]++
            }
         }
      }

      // dataPaths.each {
      //    println it.key
      //    println it.value
      //    println ""
      // }

      //println dataPaths.findAll{ it.value > 1 }
   }

   void testTemplateDataPaths()
   {
      println "====== testTemplateDataPaths ======"

      String namespace = 'test_ism_paths'

      // manager users the default repo
      assert man.getLoadedOpts(namespace).size() == 0
      man.loadAll(namespace)
      assert man.getLoadedOpts(namespace).size() == 1

      def archs = man.getAllReferencedArchetypes(namespace) // List<ObjectNode>
      assert archs.size() == 2 // compo and action
      assert archs['openEHR-EHR-ACTION.test_ism_paths.v1'].size() == 1 // one root for action because it is referenced by just one OPT

      //println archs['openEHR-EHR-ACTION.test_ism_paths.v1'][0] // ObjectNode
      archs['openEHR-EHR-ACTION.test_ism_paths.v1'][0].nodes.each { archPath, objectNode ->
         println archPath
         println objectNode.templateDataPath
      }

      def templateDataPaths = [
         '/content[archetype_id=openEHR-EHR-ACTION.test_ism_paths.v1](1)/ism_transition(1)',
         '/content[archetype_id=openEHR-EHR-ACTION.test_ism_paths.v1](1)/ism_transition(1)/current_state(1)',
         '/content[archetype_id=openEHR-EHR-ACTION.test_ism_paths.v1](1)/ism_transition(1)/current_state(1)/defining_code(1)'
      ]

      def opt = man.getOpt('test_ism_paths.es.v1', namespace)

      assert opt

      // opt.nodes.each { templatePath, nodes ->

      //    println nodes.templateDataPath
      // }

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

      // man uses default repo

      println man.status()

      assert man.getLoadedOpts(namespace).size() == 0
      man.loadAll(namespace)

      println man.status()

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
   }

   void testOptManagerLanguages()
   {
      println "====== testOptManagerLanguages ======"

      // this tests uses a different opt repo than the default one
      def repo = new OptRepositoryFSImpl(getClass().getResource('/opts/test_languages').toURI())
      this.man = OptManager.getInstance()
      this.man.init(repo)

      assert man.getLoadedOpts().size() == 0

      man.loadAll() // uses default namespace

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
   }

   void testCleanCache()
   {
      println "====== testsCleanCache ======"

      String namespace = 'test_ism_paths'

      // man uses default repo

      assert man.getLoadedOpts(namespace).size() == 0
      man.loadAll(namespace)
      assert man.getLoadedOpts(namespace).size() == 1

      sleep(3000) // teh cache ttl is 2  secs

      man.cleanCache() // should clean the opt from the cache

      assert man.getLoadedOpts(namespace).size() == 0
   }

   void testGetArchetypesInTemplate()
   {
      def optMan = OptManager.getInstance()
      def opt = optMan.getOpt('generic_agent', OptManager.DEFAULT_NAMESPACE)
      //List tree = []
      //getReferencedArchetypesRecursive(opt.definition, tree)

      println opt.getReferencedArchetypes()
   }

   void testAttributesNotInOpt()
   {
      def opt = man.getOpt('generic_relationship', OptManager.DEFAULT_NAMESPACE)
      def obj = opt.findRoot('openEHR-DEMOGRAPHIC-PARTY_RELATIONSHIP.generic_relationship.v1')

      // obj.nodes.sort{it.key}.each {
      //    println it
      // }

      assert !obj.nodes['/time_validity']
      assert !obj.nodes['/source']

      opt.complete()

      obj = opt.findRoot('openEHR-DEMOGRAPHIC-PARTY_RELATIONSHIP.generic_relationship.v1')

      // obj.nodes.sort{it.key}.each {
      //    println it
      // }

      assert obj.nodes['/time_validity']
      assert obj.nodes['/source']


      def toJson = new JsonSerializer()
      toJson.serialize(obj)
      def json = toJson.get()
      println json

   }

   void testAttributesNotInOptObservation()
   {
      def opt = man.getOpt('lab_results1', OptManager.DEFAULT_NAMESPACE)
      def obj = opt.findRoot('openEHR-EHR-OBSERVATION.lab_test-blood_glucose.v1')

      // obj.nodes.sort{it.key}.each {
      //    println it
      // }
/*
      assert !obj.nodes['/time_validity']
      assert !obj.nodes['/source']
*/
      opt.complete()

      obj = opt.findRoot('openEHR-EHR-OBSERVATION.lab_test-blood_glucose.v1')

println "obj path "+ obj.templatePath

      obj.nodes.sort{it.key}.each {
         println it
      }
/*
      assert obj.nodes['/time_validity']
      assert obj.nodes['/source']
*/

      def toJson = new JsonSerializer()
      toJson.serialize(obj)
      def json = toJson.get()
      println json

   }
}
