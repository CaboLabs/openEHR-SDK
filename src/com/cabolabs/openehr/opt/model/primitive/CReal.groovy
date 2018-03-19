package com.cabolabs.openehr.opt.model.primitive

import com.cabolabs.openehr.opt.model.IntervalFloat
import com.cabolabs.openehr.opt.model.validation.ValidationResult

@groovy.util.logging.Log4j
class CReal extends CPrimitive {

   // TODO: list constraint (this is not commonly used)
   // http://www.openehr.org/releases/1.0.2/architecture/am/aom.pdf page 38

   IntervalFloat range

   ValidationResult isValid(Float value)
   {
      if (!range.has(value)) return new ValidationResult(isValid: false, message:'CPrimitive.validation.error.valueNotInRange')

      return new ValidationResult(isValid: true)
   }
}
