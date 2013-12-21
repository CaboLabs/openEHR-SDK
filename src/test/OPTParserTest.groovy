package test

import groovy.util.GroovyTestCase
import com.cabolabs.openehr.opt.parser.*
import com.cabolabs.openehr.opt.model.*

class OPTParserTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")


   void testParser()
   {
      println "==============="
      println new File('').getCanonicalPath()
      println System.getProperty("user.dir")
      println "==============="
   
      //assert 1 == 1
      //assert 2 + 2 == 4 : "We're in trouble, arithmetic is broken"
      def parser = new OperationalTemplateParser()
      
      def path = "resources"+ PS +"opts"+ PS +"Heart Failure Clinic First Visit Summary.opt"
      def optFile = new File( path )
      def text = optFile.getText()
      
      assertNotNull(text)
      assert text != ''
      
      def opt = parser.parse( text )
      
      assertToString(opt.concept, 'Heart Failure Clinic First Visit Summary')
      
      assertNotNull(opt.definition)
      
      assertLength(4, opt.definition.attributes.toArray())
      
      opt.definition.attributes.each { println it.rmAttributeName }
   }
}