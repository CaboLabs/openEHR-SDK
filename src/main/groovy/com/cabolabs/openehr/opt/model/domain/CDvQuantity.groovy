package com.cabolabs.openehr.opt.model.domain

import com.cabolabs.openehr.opt.model.ObjectNode
import com.cabolabs.openehr.opt.model.datatypes.CodePhrase
import com.cabolabs.openehr.opt.model.validation.ValidationResult
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable

@groovy.util.logging.Log4j
class CDvQuantity extends ObjectNode {

   // property: CODE_PHRASE
   CodePhrase property
   /*
   String propertyCode
   String propertyTerminologyIdName
   String propertyTerminologyIdVersion
   */

   // List<CQuantityItem>
   List list = []

   ValidationResult isValid(Pathable parent, String units, Double magnitude)
   {
      // if there are no constraints, the validation is always true
      if (list.size() > 0)
      {
         def item = list.find { it.units == units }

         if (!item) return new ValidationResult(isValid: false, message:'CDvQuantity.validation.error.noMatchingUnits')

         if (!item.magnitude.has(magnitude)) return new ValidationResult(isValid: false, message:'CDvQuantity.validation.error.magnitudeOutOfRange')
      }

      return new ValidationResult(isValid: true)
   }
}
