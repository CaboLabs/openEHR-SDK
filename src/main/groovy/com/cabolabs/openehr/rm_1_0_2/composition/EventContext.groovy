package com.cabolabs.openehr.rm_1_0_2.composition

import com.cabolabs.openehr.rm_1_0_2.common.generic.Participation
import com.cabolabs.openehr.rm_1_0_2.common.generic.PartyIdentified
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemStructure
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class EventContext extends Pathable {

   DvDateTime start_time
   DvDateTime end_time
   String location
   DvCodedText setting
   ItemStructure other_context
   PartyIdentified health_care_facility
   List<Participation> participations

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = ((parent.path != '/') ? '/' : '') + parentAttribute
      this.dataPath = ((parent.dataPath != '/') ? '/' : '') + parentAttribute
      this.parent = parent

      if (this.other_context)
      {
         this.other_context.fillPathable(this, 'other_context')
      }
   }

   // getter with initializer
   List<Participation> getParticipations()
   {
      if (participations == null) participations = []
      participations
   }
}
