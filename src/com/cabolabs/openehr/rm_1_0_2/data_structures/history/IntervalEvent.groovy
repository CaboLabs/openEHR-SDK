package com.cabolabs.openehr.rm_1_0_2.data_structures.history

import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDuration

class IntervalEvent extends Event {
   
   DvDuration width
   DvCodedText math_function
   Integer sample_count

   DvDateTime interval_start_time()
   {
      // TODO
   }
}