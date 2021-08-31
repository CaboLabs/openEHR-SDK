package com.cabolabs.openehr.opt.model.primitive

import com.cabolabs.openehr.opt.model.IntervalDuration
import com.cabolabs.openehr.opt.model.datatypes.Duration
import com.cabolabs.openehr.opt.model.validation.ValidationResult
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable

@groovy.util.logging.Log4j
class CDuration extends CPrimitive {

   // TODO: list constraint (this is not commonly used)
   // http://www.openehr.org/releases/1.0.2/architecture/am/aom.pdf page 38
   String pattern

   IntervalDuration range

   ValidationResult isValid(Pathable parent, String value)
   {
      // TODO: check pattern constraint

      // FIXME: add message with path
      if (range && !range.has(new Duration(value: value))) return new ValidationResult(isValid: false, message:'CDuration.validation.error.valueNotInRange')

      return new ValidationResult(isValid: true)
   }
}
