package com.cabolabs.openehr.rm_1_0_2.demographic

import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Locatable
import com.cabolabs.openehr.rm_1_0_2.common.archetyped.Pathable
import com.cabolabs.openehr.rm_1_0_2.data_types.quantity.DvInterval

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class Contact extends Locatable {

   DvInterval time_validity // DvDate
   List<Address> addresses // Address

   @Override
   void fillPathable(Pathable parent, String parentAttribute)
   {
      this.path = ((parent.path != '/') ? '/' : '') + parentAttribute.replaceAll(/\[\d+\]/, '')
      this.dataPath = ((parent.dataPath != '/') ? '/' : '') + parentAttribute
      this.parent = parent

      this.addresses.fillPathable(this, "addresses")
   }

   // getter with initializer
   List<Address> getAddresses()
   {
      if (addresses == null) addresses = []
      addresses
   }
}
