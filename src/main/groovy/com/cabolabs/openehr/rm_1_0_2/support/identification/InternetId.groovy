package com.cabolabs.openehr.rm_1_0_2.support.identification

import java.util.UUID as Uuid // avoid name collision

class InternetId extends UID {

   URI value // java URI class is the closest to an Internet ID

   InternetId()
   {
      //this.value = new URI() // empty URI is not valid
   }

   InternetId(String uid)
   {
      this.value = new URI(uid)
   }
}
