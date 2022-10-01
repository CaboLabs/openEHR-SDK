package com.cabolabs.openehr.rm_1_0_2.demographic

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
 class Group extends Actor {
   
   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = ((parent.path != '/') ? '/' : '') + parentAttribute.replaceAll(/\[\d+\]/, '')
      this.dataPath = ((parent.dataPath != '/') ? '/' : '') + parentAttribute
      this.parent = parent

      // TODO: parent fillPathable
   }
}
