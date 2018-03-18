package com.cabolabs.openehr.opt.model.domain

import com.cabolabs.openehr.opt.model.ObjectNode
import com.cabolabs.openehr.opt.model.validation.ValidationResult

@groovy.util.logging.Log4j
class CCodePhrase extends ObjectNode {

   // CODE LIST CONSTRAINT

   // List<String>
   List codeList = []
   String terminologyIdName
   String terminologyIdVersion // optional


   // REFERENCE SET URI CONSTRAINT

   // TODO: this can be a list on the OPT but since
   // the Template Designer doesnt allow more than one,
   // we support just one value.
   String terminologyRef

   ValidationResult isValid(String code, String terminologyId)
   {
      // TODO: search for the terminologyId in the ref
      if (terminologyRef) return new ValidationResult(isValid: true)


      def item = codeList.find { it == code }
      if (!item) return new ValidationResult(isValid: false, message:'CCodePhrase.validation.error.noMatchingCode')


      def tidPattern = ~/(\w+)\s*(?:\(?(\w*)\)?.*)?/
      def result = tidPattern.matcher(terminologyId)
      if (terminologyIdName != result[0][1] || terminologyIdVersion != result[0][2])
      {
         return new ValidationResult(isValid: false, message:'CCodePhrase.validation.error.noMatchingTerminology')
      }


      return new ValidationResult(isValid: true)
   }
}
