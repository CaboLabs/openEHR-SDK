package com.cabolabs.openehr.rm_1_0_2.data_types.quantity

import com.cabolabs.openehr.rm_1_0_2.data_types.basic.DataValue
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText

class DvOrdinal extends DvOrdered {

   Integer value
   DvCodedText symbol

   int compareTo(Object o)
   {
      if (!(o instanceof DvOrdinal)) throw new Exception("Can't compare class "+ o.getClass() + " to DvOrdinal")

      // TODO: check the terminology_id is the same
      
      return this.value <=> o.value
   }
}
