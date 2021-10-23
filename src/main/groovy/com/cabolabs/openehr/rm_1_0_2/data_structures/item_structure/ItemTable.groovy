package com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.representation.Cluster

class ItemTable extends ItemStructure {
   
   List<Cluster> rows = []

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = ((parent.path != '/') ? '/' : '') + parentAttribute.replaceAll(/\[\d+\]/, '')
      this.dataPath = ((parent.dataPath != '/') ? '/' : '') + parentAttribute
      this.parent = parent

      this.rows.eachWithIndex{ item, i ->
         item.fillPathable(this, "rows[$i]")
      }
   }
}
