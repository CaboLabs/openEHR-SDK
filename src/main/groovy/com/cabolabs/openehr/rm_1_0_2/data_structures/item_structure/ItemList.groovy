package com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Element

class ItemList extends ItemStructure {

   List<Element> items

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

   // getter with initializer
   List<Element> getItems()
   {
      if (items == null) items = []
      items
   }
}
