package com.cabolabs.openehr.opt.model.validation

class ValidationResult {

   boolean isValid
   String message
   
   // TODO: ad list of params to be passed to the message evaluation, like the offending value(s)

   // Allows to check an instance as it is a boolean value itself
   boolean asBoolean()
   {
      return isValid
   }
}
