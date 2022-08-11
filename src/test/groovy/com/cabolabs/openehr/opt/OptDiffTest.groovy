package com.cabolabs.openehr.opt

import groovy.util.GroovyTestCase
import com.cabolabs.openehr.opt.parser.*
import com.cabolabs.openehr.validation.*
import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.diff.*
import groovy.json.*

class OptDiffTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")

   OperationalTemplate loadAndParse(String path)
   {
      def parser = new OperationalTemplateParser()

      //println getClass().getResource(path).toURI()

      def optFile = new File(getClass().getResource(path).toURI())
      //def optFile = new File( path )
      def text = optFile.getText()

      assertNotNull(text)
      assert text != ''

      return parser.parse(text)
   }

   void testOperationalTemplateDiff()
   {
      def path1 = PS +"opts"+ PS + 'diff' + PS +"Registro_de_monitor_de_signos.opt"
      def opt1 = loadAndParse(path1)

      def path2 = PS +"opts"+ PS + 'diff' + PS +"Registro_de_monitor_de_signos_v2.opt"
      def opt2 = loadAndParse(path2)

      /*
      println opt1
      println opt1.nodes.keySet()

      opt1.nodes.values().flatten().collect{ it.templateDataPath }.sort().each {
         println it
      }
      */

      def diffal = new OperationalTemplateDiffAlgorithm()
      def diff = diffal.diff(opt1, opt2)

      def node = findNode(
         diff,
        '/content[archetype_id=openEHR-EHR-OBSERVATION.blood_pressure.v2](1)/data[at0001](1)/events[at0006](1)/state[at0007](1)/items[at0008](1)'
      )

      assert node
      assert node.compareResult == 'added'

      println ""


      node = findNode(
         diff,
        '/content[archetype_id=openEHR-EHR-OBSERVATION.blood_pressure.v2](1)/data[at0001](1)/events[at0006](1)/data[at0003](1)/items[at0005](1)/value(1)'
      )

      assert node
      assert node.compareResult == 'same'

      println ""


      // compare opposite direction
      diff = diffal.diff(opt2, opt1)

      node = findNode(
         diff,
        '/content[archetype_id=openEHR-EHR-OBSERVATION.blood_pressure.v2](1)/data[at0001](1)/events[at0006](1)/state[at0007](1)/items[at0008](1)'
      )

      assert node
      assert node.compareResult == 'removed'


      node = findNode(
         diff,
        '/content[archetype_id=openEHR-EHR-OBSERVATION.blood_pressure.v2](1)/data[at0001](1)/events[at0006](1)/data[at0003](1)/items[at0005](1)/value(1)'
      )

      assert node
      assert node.compareResult == 'same'
   }

   void testOperationalTemplateDiff2()
   {
      def path1 = PS +"opts"+ PS + 'diff' + PS +"diff_test.opt"
      def opt1 = loadAndParse(path1)

      def path2 = PS +"opts"+ PS + 'diff' + PS +"diff_test_v2.opt"
      def opt2 = loadAndParse(path2)

      /*
      println opt1
      println opt1.nodes.keySet()

      opt1.nodes.values().flatten().collect{ it.templateDataPath }.sort().each {
         println it
      }
      */

      def diffal = new OperationalTemplateDiffAlgorithm()
      def diff = diffal.diff(opt1, opt2)

      // def node = findNode(
      //    diff,
      //   '/content[archetype_id=openEHR-EHR-OBSERVATION.blood_pressure.v2](1)/data[at0001](1)/events[at0006](1)/state[at0007](1)/items[at0008](1)'
      // )

      // assert node
      // assert node.compareResult == 'added'

      def jsmindSerializer = new OperationalTemplateDiff2JsMindTree()
      Map tree = jsmindSerializer.getJsMindTree(diff)

      //println tree

      println JsonOutput.toJson(tree)
   }

   def findNode(OperationalTemplateDiff diff, String path)
   {
      findNodeRecursive(diff.root, path)
   }

   def findNodeRecursive(NodeDiff root, String path)
   {
      if (root.templateDataPath == path) return root

      def found, nodes
      for (String attr : root.attributeDiffs.keySet())
      {
         nodes = root.attributeDiffs[attr]

         for (def node : nodes)
         {
            found = findNodeRecursive(node, path)
            if (found) return found
         }
      }

      return found
   }

}