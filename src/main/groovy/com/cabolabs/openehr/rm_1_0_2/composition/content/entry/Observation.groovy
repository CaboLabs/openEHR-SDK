package com.cabolabs.openehr.rm_1_0_2.composition.content.entry

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.data_structures.history.History

class Observation extends CareEntry {
   
   History data
   History state

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = ((parent.path != '/') ? '/' : '') + parentAttribute.replaceAll(/\[\d+\]/, '')
      this.dataPath = ((parent.dataPath != '/') ? '/' : '') + parentAttribute
      this.parent = parent

      this.data.fillPathable(this, 'data')
      if (this.state)
      {
         this.state.fillPathable(this, 'state')
      }
      if (this.protocol)
      {
         this.protocol.fillPathable(this, 'protocol')
      }
   }
}
