package com.cabolabs.openehr.rm_1_0_2.composition.content.entry

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemStructure
import com.cabolabs.openehr.rm_1_0_2.support.identification.LocatableRef

class InstructionDetails extends Pathable {
   
   LocatableRef instruction_id
   String activity_id
   ItemStructure wf_details
}
