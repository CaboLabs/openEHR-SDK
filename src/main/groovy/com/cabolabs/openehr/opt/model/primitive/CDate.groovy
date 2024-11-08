package com.cabolabs.openehr.opt.model.primitive

import com.cabolabs.openehr.opt.model.IntervalInt
import com.cabolabs.openehr.opt.model.validation.ValidationResult

import java.text.SimpleDateFormat
import java.text.ParseException
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable

class CDate extends CPrimitive {

   // TODO: list constraint (this is not commonly used)
   // http://www.openehr.org/releases/1.0.2/architecture/am/aom.pdf page 38

   // OPT DateTime Pattern
   String pattern

   // OPT DateTime Pattern => Java SimpleFormat Patterns
   static Map validators = [
      'yyyy-mm-dd':  ["yyyy-MM-dd", "yyyyMMdd"],
      'yyyy-mm-??':  ["yyyy-MM-dd", "yyyyMMdd", "yyyy-MM", "yyyyMM"],
      'yyyy-??-??':  ["yyyy-MM-dd", "yyyyMMdd", "yyyy-MM", "yyyyMM", "yyyy"],
      'any_allowed': ["yyyy-MM-dd", "yyyyMMdd", "yyyy-MM", "yyyyMM", "yyyy"]
   ]


   ValidationResult isValid(String formattedDate)
   {
      def formats = validators[pattern]

      for (String format : formats)
      {
         try
         {
            SimpleDateFormat sdf = new SimpleDateFormat(format)
            sdf.setLenient(false) // avoids heuristic parsing, enabling just exact parsing
            sdf.parse(formattedDate) // returns the date but we'll not use it

            return new ValidationResult(isValid: true)
         }
         catch (ParseException e) {}

      }

      return new ValidationResult(isValid: false, message:'CDate.validation.error.notAValidDate')
   }
}
