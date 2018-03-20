package com.cabolabs.openehr.opt.model.primitive

import com.cabolabs.openehr.opt.model.validation.ValidationResult

@groovy.util.logging.Log4j
class CString extends CPrimitive {

   String pattern

   // List<String>
   List list = []

   ValidationResult isValid(String value)
   {
      // TODO:
      //if (!range.has(new Duration(value: value))) return new ValidationResult(isValid: false, message:'CDuration.validation.error.valueNotInRange')

      return new ValidationResult(isValid: true)
   }
}
