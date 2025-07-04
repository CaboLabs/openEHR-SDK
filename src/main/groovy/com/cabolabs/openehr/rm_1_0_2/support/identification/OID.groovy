package com.cabolabs.openehr.rm_1_0_2.support.identification

import org.ietf.jgss.Oid // avoid name collision

class OID extends UID {

   Oid value

   OID()
   {
      this.value = new Oid() // NOTE: empty OID is not valid
   }

   OID(String uid)
   {
      this.value = new Oid(uid)
   }
}
