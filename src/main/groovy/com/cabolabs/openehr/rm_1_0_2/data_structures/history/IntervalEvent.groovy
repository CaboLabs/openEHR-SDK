package com.cabolabs.openehr.rm_1_0_2.data_structures.history

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDuration
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText

class IntervalEvent extends Event {
   
   DvDuration width
   DvCodedText math_function
   Integer sample_count

   DvDateTime interval_start_time()
   {
      // TODO
   }

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = ((parent.path != '/') ? '/' : '') + parentAttribute.replaceAll(/\[\d+\]/, '')
      this.dataPath = ((parent.dataPath != '/') ? '/' : '') + parentAttribute
      this.parent = parent

      this.data.fillPathable(this, 'data')
      if (this.state)
      {
         this.state.fillPathable(this, 'state')
      }
   }
}