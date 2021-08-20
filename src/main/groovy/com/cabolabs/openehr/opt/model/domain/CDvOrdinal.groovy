package com.cabolabs.openehr.opt.model.domain

import com.cabolabs.openehr.opt.model.ObjectNode
import com.cabolabs.openehr.opt.model.validation.ValidationResult

@groovy.util.logging.Log4j
class CDvOrdinal extends ObjectNode {

   // List<CDvOrdinalItem>
   List list = []


   /**
    * @param terminologyId format 'name[(version)]'
    */
   ValidationResult isValid(int value, String codeString, String terminologyId)
   {
      CDvOrdinalItem item = list.find{ it.value == value }

      if (!item) return new ValidationResult(isValid: false, message: "value ${value} is not valid")

      if (item.symbol.codeString != codeString) return new ValidationResult(isValid: false, message: "code_string ${codeString} doesn't match")

      if (item.symbol.terminologyId != terminologyId) return new ValidationResult(isValid: false, message: "terminology_id ${terminologyId} doesn't match")

      return new ValidationResult(isValid: true)
   }
}
