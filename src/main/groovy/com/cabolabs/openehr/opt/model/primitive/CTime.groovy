package com.cabolabs.openehr.opt.model.primitive


import com.cabolabs.openehr.opt.model.validation.ValidationResult
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable


class CTime extends CPrimitive {

   // TODO: range constraint
   // http://www.openehr.org/releases/1.0.2/architecture/am/aom.pdf

   // OPT time Pattern
   String pattern


   // ADL time pattern, e.g. hh:mm:?? (see DateTimePatternValidator), null or empty allows any valid time
   ValidationResult isValid(String value)
   {
      return DateTimePatternValidator.validateTime(value, pattern)
   }
}
