package com.cabolabs.openehr.rm_1_0_2.support.identification

import com.cabolabs.openehr.rm_1_0_2.support.identification.HierObjectId

class PartyRef extends ObjectRef {

   PartyRef()
   {
      super()
   }

   PartyRef(HierObjectId id, String type, String namespace)
   {
      super(id, type, namespace)
   }
}
