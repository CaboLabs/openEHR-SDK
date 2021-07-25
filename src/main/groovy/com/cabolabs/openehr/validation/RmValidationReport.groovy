package com.cabolabs.openehr.validation

class RmValidationReport {

   def errors = []
   
   def addError(String error)
   {
      errors << error
   }

   boolean hasErrors()
   {
      return errors.size() > 0
   }

   def append(RmValidationReport other)
   {
      this.errors.addAll(other.errors)
   }
}