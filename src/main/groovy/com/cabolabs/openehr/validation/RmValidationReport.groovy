package com.cabolabs.openehr.validation

class RmValidationReport {

   def errors = []

   // this if for general error reporting
   def addError(String error)
   {
      errors << [error: error]
   }
   
   // this is for errors detected at speific parts in the RM structure
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