package com.cabolabs.openehr.rm_1_0_2.composition.content.entry

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemStructure
import com.cabolabs.openehr.rm_1_0_2.data_types.encapsulated.DvParsable

class Activity extends Locatable {
   
   ItemStructure description
   DvParsable timing
   String action_archetype_id
}
