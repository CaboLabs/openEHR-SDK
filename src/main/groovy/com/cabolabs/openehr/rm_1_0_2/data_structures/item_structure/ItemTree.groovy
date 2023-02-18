package com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Item

class ItemTree extends ItemStructure {

   List<Item> items

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = ((parent.path != '/') ? '/' : '') + parentAttribute.replaceAll(/\[\d+\]/, '')
      this.dataPath = ((parent.dataPath != '/') ? '/' : '') + parentAttribute
      this.parent = parent

      this.items.eachWithIndex{ item, i ->
         item.fillPathable(this, "items[$i]")
      }
   }
}