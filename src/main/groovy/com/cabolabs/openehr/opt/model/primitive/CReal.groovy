package com.cabolabs.openehr.opt.model.primitive

import com.cabolabs.openehr.opt.model.IntervalBigDecimal
import com.cabolabs.openehr.opt.model.validation.ValidationResult

class CReal extends CPrimitive {

   // TODO: list constraint (this is not commonly used)
   // http://www.openehr.org/releases/1.0.2/architecture/am/aom.pdf page 38

   // interval real
   IntervalBigDecimal range

   ValidationResult isValid(BigDecimal value)
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
