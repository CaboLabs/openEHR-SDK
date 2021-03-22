package com.cabolabs.openehr.rm_1_0_2.ehr

import com.cabolabs.openehr.rm_1_0_2.common.generic.PartySelf
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemStructure

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class EhrStatus {
   
   PartySelf subject
   Boolean is_modifiable
   Boolean is_queryable
   ItemStructure other_details
}
