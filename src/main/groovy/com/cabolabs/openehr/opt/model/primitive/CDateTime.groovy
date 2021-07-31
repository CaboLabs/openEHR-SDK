package com.cabolabs.openehr.opt.model.primitive

import com.cabolabs.openehr.opt.model.IntervalInt
import com.cabolabs.openehr.opt.model.validation.ValidationResult

import java.text.SimpleDateFormat
import java.text.ParseException

@groovy.util.logging.Log4j
class CDateTime extends CPrimitive {

   // TODO: list constraint (this is not commonly used)
   // http://www.openehr.org/releases/1.0.2/architecture/am/aom.pdf page 38

   // OPT DateTime Pattern
   String pattern

   // TODO: accept seconds fraction with comma
   
   // OPT DateTime Pattern => Java SimpleFormat Patterns
   static Map validators = [
      'yyyy-mm-ddTHH:MM:SS': ["yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                              "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                              "yyyy-MM-dd'T'HH:mm:ss.SSS",
                              "yyyy-MM-dd'T'HH:mm:ss'Z'",
                              "yyyy-MM-dd'T'HH:mm:ssX",
                              "yyyy-MM-dd'T'HH:mm:ss"
                             ],
      'yyyy-mm-ddTHH:??:??': ["yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", // complete, with seconds and fractions
                              "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                              "yyyy-MM-dd'T'HH:mm:ss.SSS",
                              "yyyy-MM-dd'T'HH:mm:ss'Z'",
                              "yyyy-MM-dd'T'HH:mm:ssX",
                              "yyyy-MM-dd'T'HH:mm:ss",

                              "yyyy-MM-dd'T'HH:mm'Z'",          // without seconds and fractions
                              "yyyy-MM-dd'T'HH:mmX",
                              "yyyy-MM-dd'T'HH:mm",

                              "yyyy-MM-dd'T'HH'Z'",             // without minutes
                              "yyyy-MM-dd'T'HHX",
                              "yyyy-MM-dd'T'HH"
                             ],
      // This is applied when the complex object of the DV_DATE_TIME has no constraints
      'any_allowed':         ["yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",         // same as previous case, since it includes all the possible valid datetime formats
                              "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                              "yyyy-MM-dd'T'HH:mm:ss.SSS",
                              "yyyy-MM-dd'T'HH:mm:ss'Z'",
                              "yyyy-MM-dd'T'HH:mm:ssX",
                              "yyyy-MM-dd'T'HH:mm:ss",

                              "yyyy-MM-dd'T'HH:mm'Z'",
                              "yyyy-MM-dd'T'HH:mmX",
                              "yyyy-MM-dd'T'HH:mm",

                              "yyyy-MM-dd'T'HH'Z'",
                              "yyyy-MM-dd'T'HHX",
                              "yyyy-MM-dd'T'HH",

                              "yyyyMMdd'T'HHmmss.SSS'Z'",         // basic formats ISO-8601
                              "yyyyMMdd'T'HHmmss.SSSX",
                              "yyyyMM-dd'T'HHmmss.SSS",
                              "yyyyMM-dd'T'HHmmss'Z'",
                              "yyyyMM-dd'T'HHmmssX",
                              "yyyyMM-dd'T'HHmmss",

                              "yyyyMMdd'T'HHmm'Z'",
                              "yyyyMMdd'T'HHmmX",
                              "yyyyMMdd'T'HHmm",

                              "yyyyMMdd'T'HH'Z'",
                              "yyyyMMdd'T'HHX",
                              "yyyyMMdd'T'HH"
                             ]
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

      return new ValidationResult(isValid: false, message: "value '${formattedDate}' is not a valid datetime format")
   }
}
