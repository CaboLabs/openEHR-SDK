package com.cabolabs.openehr.opt.model.primitive

import com.cabolabs.openehr.opt.model.IntervalDouble
import com.cabolabs.openehr.opt.model.validation.ValidationResult

@groovy.util.logging.Log4j2
class CReal extends CPrimitive {

   // TODO: list constraint (this is not commonly used)
   // http://www.openehr.org/releases/1.0.2/architecture/am/aom.pdf page 38

   IntervalDouble range

   ValidationResult isValid(Float value)
   {
      if (range)
      {
         if (!range.has(value))
         {
            def msg = "value '${value}' is not contained in the range '${range.lower}..${range.upper}'"
            return new ValidationResult(isValid: false, message: msg)
         }
      }

      return new ValidationResult(isValid: true)
   }
}
