package com.cabolabs.openehr.opt.model.primitive

import com.cabolabs.openehr.opt.model.IntervalInt
import com.cabolabs.openehr.opt.model.validation.ValidationResult

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable

class CDate extends CPrimitive {

   // TODO: list constraint (this is not commonly used)
   // http://www.openehr.org/releases/1.0.2/architecture/am/aom.pdf page 38

   // OPT DateTime Pattern
   String pattern


   // ADL date pattern, e.g. yyyy-mm-?? (see DateTimePatternValidator), null or empty allows any valid date
   ValidationResult isValid(String value)
   {
      return DateTimePatternValidator.validateDate(value, pattern)
   }
}
