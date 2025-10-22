package com.cabolabs.openehr.dto_1_0_2.demographic

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.DvInterval
import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemStructure

class PartyRelationshipDto extends Locatable {

   ItemStructure details
   DvInterval time_validity // DvDate
   PartyDto source
   PartyDto target

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = ((parent.path != '/') ? '/' : '') + parentAttribute.replaceAll(/\[\d+\]/, '')
      this.dataPath = ((parent.dataPath != '/') ? '/' : '') + parentAttribute
      this.parent = parent

      this.details.fillPathable(this, "details")
   }
}