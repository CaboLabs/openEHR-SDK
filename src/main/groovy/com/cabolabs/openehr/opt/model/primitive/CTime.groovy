package com.cabolabs.openehr.opt.model.primitive


import com.cabolabs.openehr.opt.model.validation.ValidationResult
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable

import java.text.ParseException
import java.text.SimpleDateFormat

@groovy.util.logging.Log4j
class CTime extends CPrimitive {

   // TODO: range constraint
   // http://www.openehr.org/releases/1.0.2/architecture/am/aom.pdf

   // OPT time Pattern
   String pattern

   // OPT DateTime Pattern => Java SimpleFormat Patterns
   static Map validators = [
      'HH:MM:SS':  ["HH:mm:ss", "HHmmss"],
      'HH:MM:??':  ["HH:mm:ss", "HHmmss", "HH:mm", "HH:mm"],
      'HH:??:??':  ["HH:mm:ss", "HHmmss", "HH:mm", "HH:mm", "HH"],
      'any_allowed': ["HH:mm:ss", "HHmmss", "HH:mm", "HH:mm", "HH"]
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
