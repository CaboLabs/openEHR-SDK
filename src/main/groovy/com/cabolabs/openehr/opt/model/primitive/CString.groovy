package com.cabolabs.openehr.opt.model.primitive

import com.cabolabs.openehr.opt.model.validation.ValidationResult

class CString extends CPrimitive {

   String pattern

   // List<String>
   List list = []

   // FIXME: the parent of the value can be Pathable or DataValue
   // FIXME: another thing is the attribute name might not be value as in line 23!!!!!!!
   ValidationResult isValid(String value)
   {
      if (pattern)
      {
         def matching_pattern = ~/${pattern}/
         if (!matching_pattern.matcher(value).matches())
         {
            def msg = "value '${value}' doesn't match pattern '${pattern}'"
            return new ValidationResult(isValid: false, message: msg)
         }
      }

      if (list != null && list.size() > 0)
      {
         if (!list.contains(value))
         {
            def msg = "value '${value}' is not contained in the list ${list}"
            return new ValidationResult(isValid: false, message: msg)
         }
      }

      return new ValidationResult(isValid: true)
   }
}
