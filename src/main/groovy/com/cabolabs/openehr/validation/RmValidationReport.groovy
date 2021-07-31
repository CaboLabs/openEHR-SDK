package com.cabolabs.openehr.validation

class RmValidationReport {

   def errors = []
   
   def addError(String path, String error)
   {
      errors << [path: path, error: error]
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