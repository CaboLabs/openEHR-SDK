package com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation

import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText
import com.cabolabs.openehr.rm_1_0_2.data_types.basic.DataValue

class Element extends Item {

   DvCodedText null_flavour
   DataValue value

   Boolean is_null()
   {
      this.value == null
   }
}