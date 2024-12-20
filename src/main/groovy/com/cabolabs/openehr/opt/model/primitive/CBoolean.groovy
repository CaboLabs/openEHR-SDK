package com.cabolabs.openehr.opt.model.primitive

import com.cabolabs.openehr.opt.model.IntervalInt
import com.cabolabs.openehr.opt.model.validation.ValidationResult

class CBoolean extends CPrimitive {

   Boolean trueValid
   Boolean falseValid

   ValidationResult isValid(Boolean value)
   {
      if (value == null)
      {
         def msg = "value is null/empty"
         return new ValidationResult(isValid: false, message: msg)
      }

      if (trueValid != null && trueValid == Boolean.FALSE && value == Boolean.TRUE)
      {
         def msg = "value 'true' is not valid"
         return new ValidationResult(isValid: false, message: msg)
      }

      if (falseValid != null && falseValid == Boolean.FALSE && value == Boolean.FALSE)
      {
         def msg = "value 'false' is not valid"
         return new ValidationResult(isValid: false, message: msg)
      }

      return new ValidationResult(isValid: true)
   }
}
