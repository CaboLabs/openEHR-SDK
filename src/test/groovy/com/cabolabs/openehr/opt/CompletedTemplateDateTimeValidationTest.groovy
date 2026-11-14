package com.cabolabs.openehr.opt

import com.cabolabs.openehr.opt.instance_generator.RmInstanceGenerator
import com.cabolabs.openehr.opt.manager.*
import com.cabolabs.openehr.opt.model.*
import com.cabolabs.openehr.opt.model.primitive.*
import com.cabolabs.openehr.opt.parser.OperationalTemplateParser
import com.cabolabs.openehr.opt.serializer.OptXmlSerializer
import com.cabolabs.openehr.rm_1_0_2.composition.Composition
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Element
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.*
import com.cabolabs.openehr.validation.*
import groovy.util.GroovyTestCase

/**
 * complete() injects date/time constraints (C_DATE_TIME, C_DATE, C_TIME, C_DURATION with no pattern)
 * for the value of date/time data types the OPT doesn't constrain, so the validator always checks
 * the value is a valid ISO 8601 value, and when the OPT has a pattern, the value is checked against it.
 * https://github.com/CaboLabs/openEHR-SDK/issues/34
 */
class CompletedTemplateDateTimeValidationTest extends GroovyTestCase {

   private static String OPT = 'test_all_datatypes_en_v1'

   private OperationalTemplate loadOpt()
   {
      def xml = getClass().getResource("/opts/com.cabolabs.openehr_opt.namespaces.default/${OPT}.opt").text
      return new OperationalTemplateParser().parse(xml)
   }

   private Composition generate()
   {
      return new RmInstanceGenerator().generateVersionFromOPT(loadOpt(), true).data
   }

   private RmValidationReport validate(Composition c)
   {
      OptManager opt_manager = OptManager.getInstance()
      opt_manager.init(new OptRepositoryFSImpl(getClass().getResource('/opts').toURI()))
      return new RmValidator2(opt_manager).dovalidate(c, OptManager.DEFAULT_NAMESPACE)
   }

   // first ELEMENT with a value of the given type
   private Element findElement(Composition c, Class type)
   {
      def found
      def visit
      visit = { obj ->
         if (found || obj == null) return
         if (obj instanceof Collection) { obj.each { visit(it) }; return }
         if (obj instanceof Element && type.isInstance(obj.value)) { found = obj; return }
         if (!obj.getClass().name.startsWith('com.cabolabs.openehr.rm_1_0_2')) return
         obj.metaClass.properties.each { p ->
            if (p.name in ['class', 'parent', 'metaClass'] || !p.getter) return
            try { visit(p.getProperty(obj)) } catch (Exception e) {}
         }
      }
      visit(c.content)
      return found
   }

   void testCompleteInjectsDateTimeConstraints()
   {
      def opt = loadOpt()
      opt.complete()

      def nodes = opt.nodes['/context/start_time/value']
      assert nodes?.size() == 1

      def node = nodes[0]
      assert node instanceof PrimitiveObjectNode
      assert node.type == 'C_PRIMITIVE_OBJECT'
      assert node.rmTypeName == 'DATE_TIME'
      assert node.item instanceof CDateTime
      assert !node.item.pattern // any valid datetime

      assert node.item.isValid('2024-10-17T10:30:00Z')
      assert !node.item.isValid('garbage')
   }

   void testCompletedOptSerializesAndParses()
   {
      def opt = loadOpt()
      opt.complete()

      def xml = new OptXmlSerializer().serialize(opt)
      assert xml.contains('C_DATE_TIME')

      // the injected constraints have no pattern, that should be parseable
      def reparsed = new OperationalTemplateParser().parse(xml)
      assert reparsed
   }

   void testGeneratedInstanceIsValid()
   {
      def report = validate(generate())
      assert !report.hasErrors(), report.errors.collect { (it.dataPath ?: it.path) +' '+ it.error }.join('\n')
   }

   // issue #34: the DV_DATE of the ELEMENT has no constraints in the OPT (empty C_COMPLEX_OBJECT),
   // the C_DATE with no pattern comes from complete(), any valid ISO 8601 date is allowed
   void testUnconstrainedDate()
   {
      def opt = loadOpt()
      opt.complete()
      def date_value_nodes = opt.nodes.values().flatten().findAll {
         it instanceof PrimitiveObjectNode && it.item instanceof CDate && it.parent.parent.rmTypeName == 'DV_DATE'
      }
      assert date_value_nodes
      assert date_value_nodes.every { it.rmTypeName == 'DATE' && !it.item.pattern }

      def c = generate()
      def element = findElement(c, DvDate)
      assert element

      ['2024-10-17', '2024-10', '2024'].each { v ->
         element.value.value = v
         def report = validate(c)
         assert !report.hasErrors(), v +': '+ report.errors
      }

      ['garbage', '2024-02-30', '2024-10-17T10:00:00'].each { v ->
         element.value.value = v
         def report = validate(c)
         assert report.errors.size() == 1, v +': '+ report.errors
         assert report.errors[0].error.contains('not a valid ISO 8601 date')
      }
   }

   // C_DURATION with range PT0H..PT5H
   void testDurationAgainstOptRange()
   {
      def c = generate()
      def element = findElement(c, DvDuration)
      assert element

      element.value.value = 'PT2H'
      assert !validate(c).hasErrors()

      element.value.value = 'PT6H'
      assert validate(c).errors.find { it.error.contains('is not in the interval') }

      element.value.value = 'garbage'
      assert validate(c).errors.find { it.error.contains('not a valid ISO 8601 duration') }
   }

   // the pattern from the OPT is checked, it was skipped before
   void testDateTimeAgainstOptPattern()
   {
      def c = generate()
      def element = findElement(c, DvDateTime)
      assert element

      element.value.value = '2024-10-17T10:30:00'
      assert !validate(c).hasErrors()

      element.value.value = '2024-10-17' // the pattern yyyy-mm-ddTHH:MM:SS requires the time
      def report = validate(c)
      assert report.hasErrors()
      assert report.errors.find { it.error?.contains("pattern 'yyyy-mm-ddTHH:MM:SS'") }

      element.value.value = 'garbage'
      assert validate(c).hasErrors()
   }
}
