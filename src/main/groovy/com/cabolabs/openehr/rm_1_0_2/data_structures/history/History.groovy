package com.cabolabs.openehr.rm_1_0_2.data_structures.history

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.data_structures.DataStructure
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDuration
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.*

class History extends DataStructure {
   
   DvDateTime origin
   DvDuration period
   DvDuration duration
   List<Event> events = []
   ItemStructure summary

   Boolean is_periodic()
   {
      // TODO
   }

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = ((parent.path != '/') ? '/' : '') + parentAttribute.replaceAll(/\[\d+\]/, '')
      this.dataPath = ((parent.dataPath != '/') ? '/' : '') + parentAttribute
      this.parent = parent

      this.events.eachWithIndex{ event, i ->
         event.fillPathable(this, "events[$i]")
      }

      this.summary?.fillPathable(this, 'summary')
   }
}