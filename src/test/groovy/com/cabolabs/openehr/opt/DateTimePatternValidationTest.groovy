package com.cabolabs.openehr.opt

import com.cabolabs.openehr.opt.model.primitive.*
import groovy.util.GroovyTestCase

/**
 * Validation of date/time values against ADL date/time patterns, including partial dates (??)
 * and forbidden components (XX), and values with no pattern (any valid ISO 8601 value).
 * https://github.com/CaboLabs/openEHR-SDK/issues/34
 * https://github.com/CaboLabs/openEHR-SDK/issues/197
 */
class DateTimePatternValidationTest extends GroovyTestCase {

   private void valid(CPrimitive c, List values)
   {
      values.each { v ->
         def r = c.isValid(v)
         assert r.isValid, "'${v}' should be valid for pattern '${c.pattern}': ${r.message}"
      }
   }

   private void invalid(CPrimitive c, List values)
   {
      values.each { v ->
         def r = c.isValid(v)
         assert !r.isValid, "'${v}' should be invalid for pattern '${c.pattern}'"
         assert r.message
      }
   }

   // ---- dates ----

   void testDateNoPattern()
   {
      [null, ''].each { p ->
         def c = new CDate(pattern: p)
         valid(c, ['2024-10-17', '20241017', '2024-10', '202410', '2024', '2024-02-29'])
         invalid(c, ['garbage', '', '2024-13-01', '2024-02-30', '2023-02-29', '2024-10-17garbage', '2024-10-1', '24-10-17', '2024-1017', '2024-10-17T10:00:00'])
      }
   }

   void testDateFullPattern()
   {
      def c = new CDate(pattern: 'yyyy-mm-dd')
      valid(c, ['2024-10-17', '20241017'])
      invalid(c, ['2024-10', '2024', 'garbage'])
   }

   void testDateOptionalDayPattern()
   {
      def c = new CDate(pattern: 'yyyy-mm-??')
      valid(c, ['2024-10-17', '2024-10'])
      invalid(c, ['2024'])
   }

   void testDateOptionalMonthPattern()
   {
      def c = new CDate(pattern: 'yyyy-??-??')
      valid(c, ['2024-10-17', '2024-10', '2024'])
   }

   // issue #197
   void testDateForbiddenDayPattern()
   {
      def c = new CDate(pattern: 'yyyy-??-XX')
      valid(c, ['2024-10', '2024', '202410'])
      invalid(c, ['2024-10-17', '20241017'])

      c = new CDate(pattern: 'yyyy-mm-XX')
      valid(c, ['2024-10'])
      invalid(c, ['2024-10-17', '2024'])

      c = new CDate(pattern: 'yyyy-XX-XX')
      valid(c, ['2024'])
      invalid(c, ['2024-10', '2024-10-17'])
   }

   void testDateUnsupportedPattern()
   {
      // required after optional and allowed after forbidden don't make sense for ISO 8601 partial dates
      ['yyyy-??-dd', 'yyyy-XX-??', 'dd-mm-yyyy', 'whatever'].each { p ->
         def r = new CDate(pattern: p).isValid('2024-10-17')
         assert !r.isValid
         assert r.message.contains('not a valid ADL date pattern')
      }
   }

   // ---- times ----

   void testTimeNoPattern()
   {
      def c = new CTime()
      valid(c, ['10:30:00', '103000', '10:30', '1030', '10', '10:30:00.123', '10:30:00,5', '10:30:00Z', '10:30:00-03:00', '10:30:00+0530', '23:59:59'])
      invalid(c, ['garbage', '24:00:00', '10:60:00', '10:30:60', '10:30:00garbage', '10:3000', '10:30:00+25:00', '10.5'])
   }

   void testTimePatterns()
   {
      def c = new CTime(pattern: 'HH:MM:SS')
      valid(c, ['10:30:00', '103000', '10:30:00.123Z'])
      invalid(c, ['10:30', '10'])

      c = new CTime(pattern: 'hh:mm:??')
      valid(c, ['10:30:00', '10:30'])
      invalid(c, ['10'])

      c = new CTime(pattern: 'hh:??:??')
      valid(c, ['10:30:00', '10:30', '10'])

      c = new CTime(pattern: 'hh:mm:XX')
      valid(c, ['10:30', '10:30Z'])
      invalid(c, ['10:30:00', '10'])

      c = new CTime(pattern: 'hh:??:XX')
      valid(c, ['10:30', '10'])
      invalid(c, ['10:30:00'])
   }

   // ---- date times ----

   void testDateTimeNoPattern()
   {
      def c = new CDateTime()
      valid(c, [
         '2024-10-17T10:30:00', '2024-10-17T10:30:00.123', '2024-10-17T10:30:00,123Z', '2024-10-17T10:30:00-03:00',
         '20241017T103000', '20241017T103000.5+0100', '2024-10-17T10:30', '2024-10-17T10', '2024-10-17', '2024-10', '2024'
      ])
      invalid(c, [
         'garbage', '2024-10-17T', '2024-10-17T25:00:00', '2024-02-30T10:00:00', '2024-10-17T10:30:00garbage',
         '2024-10-17 10:30:00', '2024-10T10:00:00', '2024-10-17T103000', '20241017T10:30:00'
      ])
   }

   void testDateTimeFullPattern()
   {
      def c = new CDateTime(pattern: 'yyyy-mm-ddTHH:MM:SS')
      valid(c, ['2024-10-17T10:30:00', '2024-10-17T10:30:00.123Z', '20241017T103000'])
      invalid(c, ['2024-10-17T10:30', '2024-10-17', '2024', 'garbage'])
   }

   void testDateTimePartialPatterns()
   {
      def c = new CDateTime(pattern: 'yyyy-mm-ddTHH:??:??')
      valid(c, ['2024-10-17T10:30:00', '2024-10-17T10:30', '2024-10-17T10'])
      invalid(c, ['2024-10-17'])

      c = new CDateTime(pattern: 'yyyy-mm-ddTHH:MM:??')
      valid(c, ['2024-10-17T10:30:00', '2024-10-17T10:30'])
      invalid(c, ['2024-10-17T10'])

      c = new CDateTime(pattern: 'yyyy-mm-ddT??:??:??')
      valid(c, ['2024-10-17T10:30:00', '2024-10-17'])
      invalid(c, ['2024-10'])

      c = new CDateTime(pattern: 'yyyy-??-??T??:??:??')
      valid(c, ['2024-10-17T10:30:00', '2024-10-17', '2024-10', '2024'])

      c = new CDateTime(pattern: 'yyyy-mm-ddTHH:MM:XX')
      valid(c, ['2024-10-17T10:30'])
      invalid(c, ['2024-10-17T10:30:00'])

      c = new CDateTime(pattern: 'yyyy-mm-ddTXX:XX:XX')
      valid(c, ['2024-10-17'])
      invalid(c, ['2024-10-17T10'])
   }

   // ---- durations ----

   void testDurationNoPattern()
   {
      def c = new CDuration()
      valid(c, ['P1Y', 'P1Y2M3W4D', 'PT1H', 'PT1H30M', 'PT0.5S', 'P1DT12H', 'P1Y2M3DT4H5M6S', '-P1D'])
      invalid(c, ['garbage', 'P', 'PT', 'P1DT', '1D', 'P1H', 'PT1D', 'P1D garbage', 'P1M1Y'])
   }

   void testDurationPattern()
   {
      def c = new CDuration(pattern: 'PYMD')
      valid(c, ['P1Y', 'P1M', 'P1Y2M3D'])
      invalid(c, ['P1W', 'PT1H', 'P1DT1M'])

      c = new CDuration(pattern: 'PTHM')
      valid(c, ['PT1H', 'PT1H30M', 'PT30M'])
      invalid(c, ['P1M', 'PT30S', 'P1DT1H'])
   }
}
