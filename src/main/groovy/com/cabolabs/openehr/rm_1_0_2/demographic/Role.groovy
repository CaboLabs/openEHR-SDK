package com.cabolabs.openehr.rm_1_0_2.demographic

import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemStructure
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.DvInterval
import com.cabolabs.openehr.rm_1_0_2.support.identification.PartyRef

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class Role extends Party {

   DvInterval time_validity // DvDate
   PartyRef performer
   List<Capability> capabilities // Capability

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = ((parent.path != '/') ? '/' : '') + parentAttribute.replaceAll(/\[\d+\]/, '')
      this.dataPath = ((parent.dataPath != '/') ? '/' : '') + parentAttribute
      this.parent = parent

      this.capabilities.fillPathable(this, "capabilities")
   }

   // getter with initializer
   List<Capability> getCapabilities()
   {
      if (capabilities == null) capabilities = []
      capabilities
   }
}
