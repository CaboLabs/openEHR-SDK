package com.cabolabs.openehr.rm_1_0_2.composition.content.entry

import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemStructure
import com.cabolabs.openehr.rm_1_0_2.support.identification.ObjectRef

abstract class CareEntry extends Entry {
   
   ItemStructure protocol
   ObjectRef guideline_id
}
