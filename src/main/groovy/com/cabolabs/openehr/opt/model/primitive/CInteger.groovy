package com.cabolabs.openehr.opt.model.primitive

import com.cabolabs.openehr.opt.model.IntervalInt
import com.cabolabs.openehr.opt.model.validation.ValidationResult

@groovy.util.logging.Log4j
class CInteger extends CPrimitive {

   // list constraint (this is not commonly used)
   // http://www.openehr.org/releases/1.0.2/architecture/am/aom.pdf page 38

   List list = []

   IntervalInt range

   ValidationResult isValid(Integer value)
   {
      if (!range.has(value)) return new ValidationResult(isValid: false, message:'CInteger.validation.error.valueNotInRange')

      return new ValidationResult(isValid: true)
   }
}
