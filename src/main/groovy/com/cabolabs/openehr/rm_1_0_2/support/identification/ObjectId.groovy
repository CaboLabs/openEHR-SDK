package com.cabolabs.openehr.rm_1_0_2.support.identification

abstract class ObjectId {

   String value

   ObjectId()
   {

   }

   // NOTE: in superclasses we check the uid format so it validates with the right OBJECT_ID type
   ObjectId(String uid)
   {
      this.value = uid
   }

   abstract ObjectId clone()
}
