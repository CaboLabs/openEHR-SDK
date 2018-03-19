package com.cabolabs.openehr.opt.model.primitive

import com.cabolabs.openehr.opt.model.IntervalDuration
import com.cabolabs.openehr.opt.model.datatypes.Duration
import com.cabolabs.openehr.opt.model.validation.ValidationResult

@groovy.util.logging.Log4j
class CDuration extends CPrimitive {

   // TODO: list constraint (this is not commonly used)
   // http://www.openehr.org/releases/1.0.2/architecture/am/aom.pdf page 38

   IntervalDuration range

   ValidationResult isValid(String value)
   {
      if (!range.has(new Duration(value: value))) return new ValidationResult(isValid: false, message:'CDuration.validation.error.valueNotInRange')

      return new ValidationResult(isValid: true)
   }
}
