package com.cabolabs.openehr.rm_1_0_2.ehr

import com.cabolabs.openehr.rm_1_0_2.common.generic.PartySelf
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemStructure
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class EhrStatus extends Locatable {
   
   PartySelf subject
   Boolean is_modifiable
   Boolean is_queryable
   ItemStructure other_details

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = '/'
      this.dataPath = '/'
      this.parent = null

      this.other_details.fillPathable(this, "other_details")
   }
}
