package com.cabolabs.openehr.rm_1_0_2.data_structures.history

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemStructure
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDuration

abstract class Event extends Locatable {

   DvDateTime time
   ItemStructure data
   ItemStructure state

   DvDuration offset()
   {
      // TODO
   }
}