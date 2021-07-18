package com.cabolabs.openehr.opt.model.primitive

import com.cabolabs.openehr.opt.model.IntervalInt
import com.cabolabs.openehr.opt.model.validation.ValidationResult

@groovy.util.logging.Log4j
class CBoolean extends CPrimitive {

   Boolean trueValid
   Boolean falseValid

   ValidationResult isValid(Boolean value)
   {
      return new ValidationResult(isValid: true)
   }
}
