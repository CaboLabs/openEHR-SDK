package com.cabolabs.openehr.rm_1_0_2.support.identification

abstract class ObjectId {

   String value

   ObjectId()
   {

   }

   ObjectId(String uid)
   {
      this.value = uid
   }
}
