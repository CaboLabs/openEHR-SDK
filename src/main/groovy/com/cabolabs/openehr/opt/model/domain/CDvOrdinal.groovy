package com.cabolabs.openehr.opt.model.domain

import com.cabolabs.openehr.opt.model.ObjectNode
import com.cabolabs.openehr.opt.model.validation.ValidationResult
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.DvOrdinal

class CDvOrdinal extends ObjectNode {

   // List<CDvOrdinalItem>
   List list = []


   /**
    * @param terminologyId format 'name[(version)]'
    */
   ValidationResult isValid(DvOrdinal d)
   {
      int value            = d.value
      String codeString    = d.symbol.defining_code.codeString
      String terminologyId = d.symbol.defining_code.terminologyId.value

      CDvOrdinalItem item = list.find{ it.value == value }

      if (!item) return new ValidationResult(isValid: false, message: "value ${value} is not valid")

      if (item.symbol.codeString != codeString) return new ValidationResult(isValid: false, message: "code_string ${codeString} doesn't match")

      if (item.symbol.terminologyId != terminologyId) return new ValidationResult(isValid: false, message: "terminology_id '${terminologyId}' doesn't match '${item.symbol.terminologyId}'")

      return new ValidationResult(isValid: true)
   }
}
