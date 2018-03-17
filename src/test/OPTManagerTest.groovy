package test

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

   void testOptManagerLanguages()
   {
      String PS = File.separator
      def man = OptManager.getInstance('resources'+ PS +'opts'+ PS +'test_languages')

      assert man.getLoadedOpts().size() == 0

      man.loadAll()

      assert man.getLoadedOpts().size() == 2

      println "Loaded OPTs:"
      man.getLoadedOpts().each { id, opt ->
        println opt.templateId +' '+ opt.language
      }

      println "OPT nodes"
      man.getLoadedOpts().each { id, opt ->
        opt.nodes.each { optpath, node ->
          if (node.archetypeId.contains("pulse") || node.path.contains('1055'))
            println node.archetypeId +' '+ node.path // archetypeId is only present on root nodes
        }
      }

      def archetypeId = 'openEHR-EHR-OBSERVATION.pulse.v1'
      man.getNode(archetypeId, '/data[at0002]/events[at0003]/data[at0001]/items[at1055]/value/defining_code')?.codeList.each {

        println it // code
        println man.getText(archetypeId, it, 'es') // at00XX -> name
      }

      println "Referenced Archetypes from OPTManager"
      man.getAllReferencedArchetypes().keySet().each {
         println it
      }
   }
}
