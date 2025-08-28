package com.cabolabs.openehr.rm_1_0_2.support.identification

import java.util.UUID as Uuid // avoid name collision

class UUID extends UID {

   Uuid value

   UUID()
   {
      this.value = Uuid.randomUUID()
   }

   UUID(String uid)
   {
      this.value = Uuid.fromString(uid)
   }

   String toString()
   {
      return this.value.toString()
   }
}
