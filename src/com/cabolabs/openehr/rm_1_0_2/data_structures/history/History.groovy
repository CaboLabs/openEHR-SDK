package com.cabolabs.openehr.rm_1_0_2.data_structures.history

import com.cabolabs.openehr.rm_1_0_2.data.structures.DataStructure
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDuration

class History extends DataStructure {
   
   DvDateTime origin
   DvDuration period
   DvDuration duration
   List<Event> events

   Boolean is_periodic()
   {
      // TODO
   }
}