package com.cabolabs.openehr.opt

import com.cabolabs.openehr.opt.parser.OperationalTemplateParser
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.formats.OperationalTemplate2JsMindTree
import com.cabolabs.openehr.opt.manager.OptManager
import groovy.util.GroovyTestCase

class OperationalTemplate2JsMindTreeTest extends GroovyTestCase {

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

      return parser.parse( text )
   }
   
   
   void testGetJsMindTree()
   {
      def path = PS +"opts"+ PS + OptManager.DEFAULT_NAMESPACE + PS +"lab_results1.opt"
      def opt = loadAndParse(path)

      def jsmindSerializer = new OperationalTemplate2JsMindTree()
      Map tree = jsmindSerializer.getJsMindTree(opt)

      println tree

      assert true
   }

   void testGetJsMindTreeString()
   {
      def path = PS +"opts"+ PS + OptManager.DEFAULT_NAMESPACE + PS +"lab_results1.opt"
      def opt = loadAndParse(path)

      def jsmindSerializer = new OperationalTemplate2JsMindTree()
      String tree = jsmindSerializer.getJsMindTreeString(opt)

      println tree

      assert true
   }
}