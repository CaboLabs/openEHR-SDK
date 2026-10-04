package com.cabolabs.openehr.opt

import groovy.util.GroovyTestCase

import com.cabolabs.openehr.opt.diff.SemanticOperationalTemplateDiffAlgorithm
import com.cabolabs.openehr.opt.instance_validation.XmlValidation
import com.cabolabs.openehr.opt.model.OperationalTemplate
import com.cabolabs.openehr.opt.model.domain.CDvQuantity
import com.cabolabs.openehr.opt.parser.OperationalTemplateParser
import com.cabolabs.openehr.opt.serializer.JsonSerializer
import com.cabolabs.openehr.opt.serializer.OptXmlSerializer

/**
 * units_system and units_display_name: optional fields of the items of a C_DV_QUANTITY list, written by the template designer
 * since the data types spec added them to the units of a quantity. The fixtures are real template designer OPTs (units_v0 has
 * them on g and mg, v1 only on mg).
 */
class OptQuantityUnitsSystemTest extends GroovyTestCase {

   private static String PS = System.getProperty("file.separator")

   private OperationalTemplate load(String name)
   {
      def text = new File(getClass().getResource(PS +'opts'+ PS +'diff'+ PS + name).toURI()).getText()
      def opt = new OperationalTemplateParser().parse(text)
      assertNotNull(opt)
      return opt
   }

   private List<CDvQuantity> quantities(OperationalTemplate opt)
   {
      opt.nodes.values().flatten().findAll { it instanceof CDvQuantity }.unique() as List<CDvQuantity>
   }

   void testParse()
   {
      def items = quantities(load('units_system_v0.opt'))*.list.flatten()

      assert items*.units.sort() == ['g', 'mg']
      items.each {
         assert it.unitsSystem == 'http://unitsofmeasure.org'
         assert it.unitsDisplayName == it.units
      }
   }

   void testAbsentFieldsAreNull()
   {
      // an OPT written before the fields existed
      def opt = load('Registro_de_monitor_de_signos.opt')
      def items = quantities(opt)*.list.flatten()

      assert items // there are quantities in it
      items.each {
         assert it.unitsSystem == null
         assert it.unitsDisplayName == null
      }
   }

   void testSerializeKeepsThemInOrderAndValidates()
   {
      def opt = load('units_system_v0.opt')
      String xml = new OptXmlSerializer(true).serialize(opt)

      // after units, as the schema says
      def m = (xml =~ /(?s)<units>mg<\/units>\s*<units_system>http:\/\/unitsofmeasure.org<\/units_system>\s*<units_display_name>mg<\/units_display_name>/)
      assert m.find()

      // both schemas accept it
      ['/xsd/OperationalTemplate.xsd', '/xsd/OperationalTemplateExtra.xsd'].each { xsd ->
         def validator = new XmlValidation(getClass().getResourceAsStream(xsd))
         assert validator.validate(xml) : "${xsd}: ${validator.getErrors()}"
      }

      // parse again: same fields, and serializing it again gives the same text
      def again = new OperationalTemplateParser().parse(xml)
      def items = quantities(again)*.list.flatten()
      assert items*.unitsSystem.every { it == 'http://unitsofmeasure.org' }
      assert items*.unitsDisplayName.sort() == ['g', 'mg']
      assert new OptXmlSerializer(true).serialize(again) == xml
   }

   void testSerializeWithoutThemWritesNothing()
   {
      def opt = load('Registro_de_monitor_de_signos.opt')
      String xml = new OptXmlSerializer(true).serialize(opt)

      assert quantities(opt) // it has quantities
      assert !xml.contains('units_system')
      assert !xml.contains('units_display_name')
   }

   void testJson()
   {
      def json = new JsonSerializer().serialize(load('units_system_v0.opt')) // a map

      def items = []
      def walk
      walk = { n ->
         if (n instanceof Map)
         {
            if (n.containsKey('units')) items << n
            n.values().each { walk(it) }
         }
         else if (n instanceof List) n.each { walk(it) }
      }
      walk(json)

      assert items.find { it.units == 'mg' }.units_system == 'http://unitsofmeasure.org'
      assert items.find { it.units == 'mg' }.units_display_name == 'mg'
   }

   void testDiffReportsAChangeOfTheUnitsSystemAsInformation()
   {
      def v1 = load('units_system_v0.opt')
      def v2 = load('units_system_v0.opt')
      quantities(v2)*.list.flatten().find { it.units == 'mg' }.unitsSystem = 'urn:other:system'

      def diff = new SemanticOperationalTemplateDiffAlgorithm().diff(v1, v2)

      def changes = []
      def walk
      walk = { n ->
         n.listChanges.each { lc -> lc.modified.each { changes.addAll(it.changes) } }
         n.attributes.values().each { a -> a.children.each { walk(it) } }
      }
      walk(diff.root)

      def change = changes.find { it.field == 'unitsSystem' }
      assert change
      assert change.oldValue == 'http://unitsofmeasure.org'
      assert change.newValue == 'urn:other:system'

      // information, not a breaking change
      assert !diff.breakingChanges
   }

   void testDiffWithTheSameUnitsSystemHasNoChanges()
   {
      def diff = new SemanticOperationalTemplateDiffAlgorithm().diff(load('units_system_v0.opt'), load('units_system_v0.opt'))

      assert diff.root.status == 'same'
      assert !diff.breakingChanges
   }
}
