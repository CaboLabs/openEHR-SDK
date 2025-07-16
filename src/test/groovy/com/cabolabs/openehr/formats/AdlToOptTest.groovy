package com.cabolabs.openehr.formats

import groovy.util.GroovyTestCase

import org.openehr.am.archetype.Archetype
import org.openehr.rm.support.identification.ArchetypeID
import se.acode.openehr.parser.*

import com.cabolabs.openehr.opt.opt_generator.AdlToOpt
import com.cabolabs.openehr.opt.model.OperationalTemplate

import com.cabolabs.openehr.opt.serializer.OptXmlSerializer


class AdlToOptTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")

   /*
   void testAdlToOpt()
   {
      def adl = new File("src${PS}main${PS}resources${PS}adl${PS}openEHR-DEMOGRAPHIC-ROLE.generic_role.v1.adl")

      def archetype = this.loadArchetype(adl)


      assert archetype != null


      def adlToOpt = new AdlToOpt()

      //def archetype = adlToOpt.parseAdl(adl)

      def opt = adlToOpt.generateOpt(archetype)

      assert opt != null
      assert opt.uid != null
      // assert opt.templateId == 'adl_to_opt_test'
      // assert opt.concept == 'adl_to_opt_test'
      assert opt.language == 'ISO_639-1::en'
      assert opt.definition != null

      def toXml = new OptXmlSerializer(true)
      String optString = toXml.serialize(opt)
      //println optString
   }

   void testAdlToOptPerson()
   {
      def adl = new File("src${PS}main${PS}resources${PS}adl${PS}demographic${PS}openEHR-DEMOGRAPHIC-PERSON.persona_cua.v1.adl")

      def archetype = this.loadArchetype(adl)


      assert archetype != null


      def adlToOpt = new AdlToOpt()

      //def archetype = adlToOpt.parseAdl(adl)

      def opt = adlToOpt.generateOpt(archetype)

      assert opt != null
      assert opt.uid != null
      // assert opt.templateId == 'adl_to_opt_test'
      // assert opt.concept == 'adl_to_opt_test'
      assert opt.language == 'ISO_639-1::es-cl'
      assert opt.definition != null

      def toXml = new OptXmlSerializer(true)
      String optString = toXml.serialize(opt)
      println optString
   }

   void testAdlToOptProfRole()
   {
      def adl = new File("src${PS}main${PS}resources${PS}adl${PS}demographic${PS}openEHR-DEMOGRAPHIC-ROLE.profesional_cua.v1.adl")

      def archetype = this.loadArchetype(adl)


      assert archetype != null


      def adlToOpt = new AdlToOpt()

      //def archetype = adlToOpt.parseAdl(adl)

      def opt = adlToOpt.generateOpt(archetype)

      assert opt != null
      assert opt.uid != null
      // assert opt.templateId == 'adl_to_opt_test'
      // assert opt.concept == 'adl_to_opt_test'
      assert opt.language == 'ISO_639-1::es-cl'
      assert opt.definition != null

      def toXml = new OptXmlSerializer(true)
      String optString = toXml.serialize(opt)
      println optString
   }
   */

   void testAdlToOptStatus()
   {
      def adl = new File("src${PS}main${PS}resources${PS}adl${PS}openEHR-EHR-EHR_STATUS.status_all_types.v0.adl")

      def archetype = this.loadArchetype(adl)


      assert archetype != null


      def adlToOpt = new AdlToOpt()

      //def archetype = adlToOpt.parseAdl(adl)

      def opt = adlToOpt.generateOpt(archetype)

      assert opt != null
      assert opt.uid != null
      // assert opt.templateId == 'adl_to_opt_test'
      // assert opt.concept == 'adl_to_opt_test'
      assert opt.language == 'ISO_639-1::en'
      assert opt.definition != null

      def toXml = new OptXmlSerializer(true)
      String optString = toXml.serialize(opt)
      println optString
   }

   private Archetype loadArchetype(File adl)
   {
      ADLParser parser = null
      try
      {
         parser = new ADLParser(adl)
      }
      catch (IOException e)
      {
         //log.debug("PROBLEMA AL CREAR EL PARSER: " + e.message)
         println e.message
         return
      }

      Archetype archetype = null
      try
      {
         if (!parser) println "AHR: " + adl.name
         archetype = parser.archetype()
      }
      catch (Exception e)
      {
         println e.message
         return
      }

      return archetype
   }
}