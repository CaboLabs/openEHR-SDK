package com.cabolabs.openehr.rm_1_0_2.ehr

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.security.AccessControlSettings

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class EhrAccess extends Locatable {
   
   AccessControlSettings settings

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = ((parent.path != '/') ? '/' : '') + parentAttribute.replaceAll(/\[\d+\]/, '')
      this.dataPath = ((parent.dataPath != '/') ? '/' : '') + parentAttribute
      this.parent = parent

      // From the RM 1.0.2 spec we don't know if AccessControlSetting is Pathable
      //this.settings.fillPathable(this, "settings")
   }
}
