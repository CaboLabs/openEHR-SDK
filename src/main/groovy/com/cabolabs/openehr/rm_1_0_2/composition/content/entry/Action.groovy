package com.cabolabs.openehr.rm_1_0_2.composition.content.entry

import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemStructure
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.date_time.DvDateTime

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class Action extends CareEntry {
   
   DvDateTime time
   ItemStructure description
   IsmTransition ism_transition
   InstructionDetails instruction_details
}
