package com.cabolabs.openehr.opt.model.primitive

import com.cabolabs.openehr.opt.model.IntervalInt
import com.cabolabs.openehr.opt.model.validation.ValidationResult

@groovy.util.logging.Log4j2
class CInteger extends CPrimitive {

   // list constraint (this is not commonly used)
   // http://www.openehr.org/releases/1.0.2/architecture/am/aom.pdf page 38

   List list = []

   IntervalInt range

   ValidationResult isValid(Integer value)
   {
      if (range)
      {
         if (!range.has(value))
         {
            def msg = "value '${value}' is not contained in the range '${range.lower}..${range.upper}'"
            return new ValidationResult(isValid: false, message: msg)
         }
      }

      if (list)
      {
         if (!list.contains(value))
         {
            def msg = "value '${value}' is not contained in the list '${list}'"
            return new ValidationResult(isValid: false, message: msg)
         }
      }

      return new ValidationResult(isValid: true)
   }
}
