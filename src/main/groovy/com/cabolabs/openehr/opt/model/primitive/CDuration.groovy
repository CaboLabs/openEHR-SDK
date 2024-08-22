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

   ValidationResult isValid(String value)
   {
      // TODO: check pattern constraint

      // FIXME: add message with path
      if (range && !range.has(new Duration(value: value)))
      {
         return new ValidationResult(isValid: false, message: "value '${value}' is not in the interval "+ range)
      }

      return new ValidationResult(isValid: true)
   }

   // verifies if an ADL duration pattern is valid or not
   // a pattern could be PYMWDTHMS with all being optional but at least one should be there
   static boolean validPattern(String pattern)
   {
      def matcher = ~/^P(([YMWD]+)|([YMWD]*T[HMS]+))$/

      return pattern.matches(matcher)
   }
}
