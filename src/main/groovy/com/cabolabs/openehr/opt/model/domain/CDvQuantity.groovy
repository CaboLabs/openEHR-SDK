package com.cabolabs.openehr.opt.model.domain

import com.cabolabs.openehr.opt.model.ObjectNode
import com.cabolabs.openehr.opt.model.datatypes.CodePhrase
import com.cabolabs.openehr.opt.model.validation.ValidationResult
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.DvQuantity

class CDvQuantity extends ObjectNode {

   // property: CODE_PHRASE
   CodePhrase property

   // List<CQuantityItem>
   List<CQuantityItem> list = []

   ValidationResult isValid(DvQuantity dvq)
   {
      // if there are no constraints, the validation is always true
      if (list.size() > 0)
      {
         String units = dvq.units
         BigDecimal magnitude = dvq.magnitude

         def item = list.find { it.units == units }

         if (!item)
         {
            return new ValidationResult(isValid: false, message: "units '${units}' don't match "+ list.units)
         }

         if (item.magnitude && !item.magnitude.has(magnitude))
         {
            return new ValidationResult(isValid: false, message: "magnitude ${magnitude} is not in the interval "+ item.magnitude)
         }
      }

      // FIXME: validate the property

      return new ValidationResult(isValid: true)
   }

   String toString()
   {
      return "CDvQuantity: "+ property.codeString +" "+ list.toString()
   }
}
