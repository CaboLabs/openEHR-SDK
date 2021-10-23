package com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Element

class ItemSingle extends ItemStructure {
   
   Element item

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = ((parent.path != '/') ? '/' : '') + parentAttribute.replaceAll(/\[\d+\]/, '')
      this.dataPath = ((parent.dataPath != '/') ? '/' : '') + parentAttribute
      this.parent = parent

      this.item.fillPathable(this, 'item')
   }
}
