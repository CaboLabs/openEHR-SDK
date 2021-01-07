package com.cabolabs.openehr.rm_1_0_2.composition

import com.cabolabs.openehr.rm_1_0_2.common.generic.Participation
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemStructure
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime
import com.cabolabs.openehr.rm_1_0_2.data_types.text.DvCodedText

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class EventContext {
   
   DvDateTime start_time
   DvDateTime end_time
   String location
   DvCodedText setting
   ItemStructure other_details
   PartyIdentified health_care_facility
   List<Participation> participations
}
