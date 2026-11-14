package com.cabolabs.openehr.opt.model.primitive

import com.cabolabs.openehr.opt.model.IntervalDuration
import com.cabolabs.openehr.opt.model.datatypes.Duration
import com.cabolabs.openehr.opt.model.validation.ValidationResult

class CDuration extends CPrimitive {

   String pattern

   IntervalDuration range

   def setPattern(String pattern)
   {
      if (!CDuration.validPattern(pattern))
      {
         throw new Exception("C_DURATION.pattern value '${pattern}' is not valid")
      }
      this.pattern = pattern
   }

   // ISO 8601 duration, e.g. P1Y2M3W4DT5H6M7.5S, at least one component, T only if there are time components
   private static final def DURATION_VALUE = ~/^-?P(?=\d|T\d)(?:(\d+(?:[.,]\d+)?)Y)?(?:(\d+(?:[.,]\d+)?)M)?(?:(\d+(?:[.,]\d+)?)W)?(?:(\d+(?:[.,]\d+)?)D)?(?:T(?=\d)(?:(\d+(?:[.,]\d+)?)H)?(?:(\d+(?:[.,]\d+)?)M)?(?:(\d+(?:[.,]\d+)?)S)?)?$/

   // units for each group of DURATION_VALUE, time units are prefixed with T to differentiate months and minutes
   private static final List DURATION_UNITS = ['Y', 'M', 'W', 'D', 'TH', 'TM', 'TS']

   ValidationResult isValid(String value)
   {
      def m = (value =~ DURATION_VALUE)
      if (value == null || !m.matches())
      {
         return new ValidationResult(isValid: false, message: "value '${value}' is not a valid ISO 8601 duration")
      }

      // the ADL pattern lists the allowed units, e.g. PYMD or PTHM
      if (pattern)
      {
         def allowed = CDuration.patternUnits(pattern)
         def used = []
         DURATION_UNITS.eachWithIndex { unit, i -> if (m.group(i + 1) != null) used << unit }
         def not_allowed = used - allowed

         if (not_allowed)
         {
            return new ValidationResult(isValid: false, message: "value '${value}' has units not allowed by the duration pattern '${pattern}'")
         }
      }

      // FIXME: add message with path
      if (range)
      {
         def in_range
         try
         {
            in_range = range.has(new Duration(value: value))
         }
         catch (Exception e)
         {
            return new ValidationResult(isValid: false, message: "value '${value}' can't be compared with the interval "+ range)
         }

         if (!in_range)
         {
            return new ValidationResult(isValid: false, message: "value '${value}' is not in the interval "+ range)
         }
      }

      return new ValidationResult(isValid: true)
   }

   // units allowed by an ADL duration pattern, e.g. PYMDTHM => [Y, M, D, TH, TM]
   static List patternUnits(String pattern)
   {
      def parts = pattern.substring(1).split('T', -1) as List
      def units = (parts[0] as List)
      if (parts.size() > 1) units += (parts[1] as List).collect { 'T'+ it }
      return units
   }

   // verifies if an ADL duration pattern is valid or not
   // a pattern could be PYMWDTHMS with all being optional but at least one should be there
   static boolean validPattern(String pattern)
   {
      def matcher = ~/^P(([YMWD]+)|([YMWD]*T[HMS]+))$/

      return pattern.matches(matcher)
   }
}
