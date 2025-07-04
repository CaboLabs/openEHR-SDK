package com.cabolabs.openehr.rm_1_0_2.support.identification

class ObjectRef {

   String namespace
   String type
   ObjectId id

   ObjectRef()
   {
      // default constructor
   }

   ObjectRef(ObjectId id, String type, String namespace)
   {
      this.id = id
      this.type = type
      this.namespace = namespace
   }
}
