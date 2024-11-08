package com.cabolabs.openehr.rm_1_0_2.composition.content.entry

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.data_types.encapsulated.DvParsable
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvText

class Instruction extends CareEntry {

   DvText narrative
   DvDateTime expiry_time
   DvParsable wf_definition
   List<Activity> activities

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = ((parent.path != '/') ? '/' : '') + parentAttribute.replaceAll(/\[\d+\]/, '')
      this.dataPath = ((parent.dataPath != '/') ? '/' : '') + parentAttribute
      this.parent = parent

      if (this.protocol)
      {
         this.protocol.fillPathable(this, 'protocol')
      }

      this.activities.eachWithIndex{ activity, i ->
         activity.fillPathable(this, "activities[$i]")
      }
   }

   // getter with initializer
   List<Activity> getActivities()
   {
      if (activities == null) activities = []
      activities
   }
}
