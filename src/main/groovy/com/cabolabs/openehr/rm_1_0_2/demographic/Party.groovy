package com.cabolabs.openehr.rm_1_0_2.demographic

import com.cabolabs.openehr.rm_1_0_2.data_structures.item_structure.ItemStructure
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.support.identification.LocatableRef
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
abstract class Party extends Locatable {
   
   Set reverse_relationhips // LocatableRef
   ItemStructure details
   List contacts // Contact
   List identities // PartyIdentity

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = ((parent.path != '/') ? '/' : '') + parentAttribute.replaceAll(/\[\d+\]/, '')
      this.dataPath = ((parent.dataPath != '/') ? '/' : '') + parentAttribute
      this.parent = parent

      this.details.fillPathable(this, "details")
   }
}
