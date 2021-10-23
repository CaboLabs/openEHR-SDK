package com.cabolabs.openehr.rm_1_0_2.data_structures.history

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable

class PointEvent extends Event {

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
   }
}