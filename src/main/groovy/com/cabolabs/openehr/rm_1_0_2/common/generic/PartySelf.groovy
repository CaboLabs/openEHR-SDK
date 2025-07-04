package com.cabolabs.openehr.rm_1_0_2.common.generic

import com.cabolabs.openehr.rm_1_0_2.support.identification.PartyRef
import com.cabolabs.openehr.rm_1_0_2.support.identification.HierObjectId

class PartySelf extends PartyProxy {

   PartySelf()
   {
      super()
   }

   PartySelf(HierObjectId id)
   {
      super()
      this.external_ref = new PartyRef(id: id, type: "PERSON", namespace: "demographic")
   }

}